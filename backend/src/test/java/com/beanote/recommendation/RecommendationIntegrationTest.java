package com.beanote.recommendation;

import static com.beanote.support.TestApi.noteJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.beanote.recommendation.service.RecommendationCache;
import com.beanote.support.IntegrationTest;
import com.beanote.support.TestApi;
import com.jayway.jsonpath.JsonPath;

/**
 * 추천 (R-1 ~ R-4) 규칙과 캐시 무효화 (5-6).
 */
@IntegrationTest
class RecommendationIntegrationTest {

	static final String BRIGHT_SURVEY = """
			{"coffeeHabit":"POUR_OVER",
			 "preferences":{"acidity":5,"sweetness":3,"body":2,"bitterness":1,"aftertaste":3},
			 "likedCategories":["FLORAL","CITRUS"],"dislikedCategories":["ROASTED"]}""";

	@Autowired
	MockMvcTester mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	RecommendationCache cache;

	TestApi api;
	String user;

	@BeforeEach
	void setUp() {
		api = new TestApi(mvc);
		user = api.newUserToken();
	}

	@Test
	void withoutSurveyOrNotesRecommendsPopularBeans() {
		String json = TestApi.body(api.get("/api/v1/recommendations", user));
		assertThat(JsonPath.<Boolean>read(json, "$.data.popular")).isTrue();
		assertThat(JsonPath.<String>read(json, "$.data.confidence")).isEqualTo("NONE");
		assertThat(JsonPath.<List<Object>>read(json, "$.data.items")).hasSize(10);
		assertThat(JsonPath.<List<Object>>read(json, "$.data.items[*].score")).containsOnlyNulls();
		assertThat(JsonPath.<List<String>>read(json, "$.data.items[*].reasons[0].type")).containsOnly("POPULAR");
	}

	@Test
	void surveyBasedRecommendationsFollowSurveyAndExplainWhy() {
		api.put("/api/v1/survey", user, BRIGHT_SURVEY);

		String json = TestApi.body(api.get("/api/v1/recommendations", user));
		assertThat(JsonPath.<Boolean>read(json, "$.data.popular")).isFalse();
		assertThat(JsonPath.<String>read(json, "$.data.confidence")).isEqualTo("SURVEY");
		List<Map<String, Object>> items = JsonPath.read(json, "$.data.items");
		assertThat(items).hasSize(10);
		// 산미·꽃향을 좋아하고 쓴맛·로스팅 향을 싫어하는 설문 → 상위는 밝은 로스팅
		assertThat(JsonPath.<List<String>>read(json, "$.data.items[0:3].bean.roastLevel"))
				.allSatisfy(level -> assertThat(level).isIn("LIGHT", "MEDIUM_LIGHT"));
		assertThat(JsonPath.<List<Double>>read(json, "$.data.items[*].score"))
				.isSortedAccordingTo((a, b) -> Double.compare(b, a));
		for (int i = 0; i < items.size(); i++) {
			List<Map<String, String>> reasons = JsonPath.read(json, "$.data.items[" + i + "].reasons");
			assertThat(reasons).hasSizeBetween(1, 2);
			assertThat(reasons).allSatisfy(r -> assertThat(r.get("text")).isNotBlank());
		}
	}

	@Test
	void 이미_마신_원두와_관심_없음_원두와_판매_종료_원두는_추천되지_않는다() {
		api.put("/api/v1/survey", user, BRIGHT_SURVEY);
		List<Integer> first = recommendedIds();

		// 상위 2개는 마셔 보고, 3번째는 관심 없음
		api.createNote(user, noteJson(first.get(0), "4", 4, "PRIVATE", "[]"));
		api.createNote(user, noteJson(first.get(1), "2", 2, "PRIVATE", "[]"));
		assertThat(api.post("/api/v1/recommendations/feedback", user,
				"{\"beanId\":%d,\"type\":\"NOT_INTERESTED\"}".formatted(first.get(2)))).hasStatusOk();

		List<Integer> after = recommendedIds();
		assertThat(after).doesNotContain(first.get(0), first.get(1), first.get(2));

		Integer discontinued = jdbc.queryForObject("SELECT id FROM beans WHERE status = 'DISCONTINUED' LIMIT 1",
				Integer.class);
		assertThat(after).doesNotContain(discontinued);
	}

	@Test
	void cacheIsUsedAndInvalidatedByOwnChanges() {
		api.put("/api/v1/survey", user, BRIGHT_SURVEY);
		Long userId = userId();

		api.get("/api/v1/recommendations", user);
		assertThat(cache.contains(userId)).isTrue();

		api.createNote(user, noteJson(1, "4", 4, "PRIVATE", "[]"));
		assertThat(cache.contains(userId)).as("노트 작성 후 무효화").isFalse();

		api.get("/api/v1/recommendations", user);
		api.put("/api/v1/survey", user, BRIGHT_SURVEY);
		assertThat(cache.contains(userId)).as("설문 변경 후 무효화").isFalse();

		api.get("/api/v1/recommendations", user);
		api.post("/api/v1/recommendations/feedback", user, "{\"beanId\":2,\"type\":\"ALREADY_TRIED\"}");
		assertThat(cache.contains(userId)).as("피드백 후 무효화").isFalse();
	}

	@Test
	void feedbackValidation() {
		assertThat(api.post("/api/v1/recommendations/feedback", user, "{\"beanId\":999999,\"type\":\"NOT_INTERESTED\"}"))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson().extractingPath("$.error.code").isEqualTo("BEAN_NOT_FOUND");
		assertThat(api.post("/api/v1/recommendations/feedback", user, "{\"beanId\":1,\"type\":\"MAYBE\"}"))
				.hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(api.post("/api/v1/recommendations/feedback", null, "{\"beanId\":1,\"type\":\"NOT_INTERESTED\"}"))
				.hasStatus(HttpStatus.UNAUTHORIZED);
		// 같은 피드백 두 번은 한 번만 저장
		api.post("/api/v1/recommendations/feedback", user, "{\"beanId\":1,\"type\":\"NOT_INTERESTED\"}");
		assertThat(api.post("/api/v1/recommendations/feedback", user, "{\"beanId\":1,\"type\":\"NOT_INTERESTED\"}"))
				.hasStatusOk();
	}

	@Test
	void recommendationsRequireLogin() {
		assertThat(api.get("/api/v1/recommendations", null)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void similarBeansArePublicAndExcludeItself() {
		String json = TestApi.body(api.get("/api/v1/beans/1/similar", null));
		List<Integer> ids = JsonPath.read(json, "$.data[*].bean.id");
		assertThat(ids).hasSize(5).doesNotContain(1).doesNotHaveDuplicates();
		assertThat(JsonPath.<List<Double>>read(json, "$.data[*].similarity"))
				.isSortedAccordingTo((a, b) -> Double.compare(b, a));

		assertThat(api.get("/api/v1/beans/999999/similar", null))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson().extractingPath("$.error.code").isEqualTo("BEAN_NOT_FOUND");
	}

	@Test
	void newBeanGetsVectorAndCanBeRecommended() {
		long bean = api.createFreshBean(user);
		assertThat(jdbc.queryForObject("SELECT vector IS NOT NULL FROM bean_stats WHERE bean_id = ?",
				Boolean.class, bean)).isTrue();
	}

	private List<Integer> recommendedIds() {
		return JsonPath.read(TestApi.body(api.get("/api/v1/recommendations", user)), "$.data.items[*].bean.id");
	}

	private Long userId() {
		return Long.valueOf(TestApi.read(api.get("/api/v1/users/me", user), "$.data.id"));
	}

}
