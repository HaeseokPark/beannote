package com.beanote.taste;

import static com.beanote.support.TestApi.noteJson;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.beanote.support.IntegrationTest;
import com.beanote.support.TestApi;
import com.jayway.jsonpath.JsonPath;

/**
 * 온보딩 설문 (O-1 ~ O-5) 과 취향 프로필 (T-1 ~ T-4).
 */
@IntegrationTest
class SurveyAndTasteProfileIntegrationTest {

	static final String BRIGHT_SURVEY = """
			{"coffeeHabit":"POUR_OVER",
			 "preferences":{"acidity":5,"sweetness":3,"body":2,"bitterness":1,"aftertaste":3},
			 "likedCategories":["FLORAL","CITRUS"],"dislikedCategories":["ROASTED"]}""";

	@Autowired
	MockMvcTester mvc;

	@Autowired
	JdbcTemplate jdbc;

	TestApi api;
	String user;

	@BeforeEach
	void setUp() {
		api = new TestApi(mvc);
		user = api.newUserToken();
	}

	@Test
	void surveyIsEmptyUntilSavedAndCanBeRedone() {
		assertThat(api.get("/api/v1/survey", user)).hasStatusOk()
				.bodyJson().extractingPath("$.data").isNull();

		assertThat(api.put("/api/v1/survey", user, BRIGHT_SURVEY)).hasStatusOk()
				.bodyJson()
				.isLenientlyEqualTo("""
						{"data":{"coffeeHabit":"POUR_OVER","preferences":{"acidity":5,"bitterness":1},
						 "likedCategories":[{"code":"FLORAL","name":"꽃"},{"code":"CITRUS"}],
						 "dislikedCategories":[{"code":"ROASTED"}]}}
						""");

		// 다시 하기 (O-5)
		assertThat(api.put("/api/v1/survey", user, BRIGHT_SURVEY.replace("\"POUR_OVER\"", "\"LATTE\"")
				.replace("[\"FLORAL\",\"CITRUS\"]", "[\"NUTTY_COCOA\"]"))).hasStatusOk();
		assertThat(api.get("/api/v1/survey", user)).bodyJson()
				.isLenientlyEqualTo("{\"data\":{\"coffeeHabit\":\"LATTE\",\"likedCategories\":[{\"code\":\"NUTTY_COCOA\"}]}}");
	}

	@Test
	void surveyValidation() {
		assertThat(api.put("/api/v1/survey", user, BRIGHT_SURVEY.replace("\"ROASTED\"", "\"FLORAL\"")))
				.as("같은 향을 좋아하고 싫어함")
				.hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(api.put("/api/v1/survey", user, BRIGHT_SURVEY.replace("\"ROASTED\"", "\"BANANA\"")))
				.hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(api.put("/api/v1/survey", user,
				BRIGHT_SURVEY.replace("[\"FLORAL\",\"CITRUS\"]", "[\"FLORAL\",\"CITRUS\",\"BERRY\",\"SWEET\"]")))
				.as("좋아하는 향 최대 3개")
				.hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(api.put("/api/v1/survey", user, BRIGHT_SURVEY.replace("\"acidity\":5", "\"acidity\":6")))
				.hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(api.put("/api/v1/survey", null, BRIGHT_SURVEY)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void profileMovesFromNoneToSurveyToForming() {
		var none = assertThat(api.get("/api/v1/taste-profile/me", user)).hasStatusOk().bodyJson();
		none.extractingPath("$.data.confidence").isEqualTo("NONE");
		none.extractingPath("$.data.radar").isNull();

		api.put("/api/v1/survey", user, BRIGHT_SURVEY);
		var survey = assertThat(api.get("/api/v1/taste-profile/me", user)).bodyJson();
		survey.extractingPath("$.data.confidence").isEqualTo("SURVEY");
		survey.extractingPath("$.data.radarSource").isEqualTo("SURVEY");
		survey.extractingPath("$.data.radar.acidity").isEqualTo(5.0);
		survey.extractingPath("$.data.likedCategories[*].code").asArray().contains("FLORAL", "CITRUS");
		survey.extractingPath("$.data.dislikedCategories[0].code").isEqualTo("ROASTED");

		// 노트 3개 → 형성 중. 별점 높은 노트(산미 5, 블루베리)가 레이더·선호 향미를 이끈다
		long berry = tagId("BLUEBERRY");
		long smoky = tagId("SMOKY");
		api.createNote(user, noteJson(2, "5.0", 5, "PRIVATE", "[" + berry + "]"));   // 구지 내추럴 (NATURAL, ET)
		api.createNote(user, noteJson(13, "1.5", 1, "PRIVATE", "[" + smoky + "]"));  // 만델링 (OTHER, ID)
		api.createNote(user, noteJson(3, "3.5", 3, "PRIVATE", "[]"));                // 우일라 (WASHED, CO)

		String json = TestApi.body(api.get("/api/v1/taste-profile/me", user));
		assertThat(JsonPath.<String>read(json, "$.data.confidence")).isEqualTo("FORMING");
		assertThat(JsonPath.<Integer>read(json, "$.data.notesUntilStable")).isEqualTo(7);
		assertThat(JsonPath.<String>read(json, "$.data.radarSource")).isEqualTo("NOTES");
		// 평균(3.33)보다 높은 노트 두 개(5.0, 3.5)의 가중 평균 → 산미 5 쪽으로 크게 기운다
		assertThat(JsonPath.<Double>read(json, "$.data.radar.acidity")).isGreaterThan(4.5);
		assertThat(JsonPath.<String>read(json, "$.data.likedFlavors[0].code")).isEqualTo("BLUEBERRY");
		assertThat(JsonPath.<String>read(json, "$.data.dislikedFlavors[0].code")).isEqualTo("SMOKY");
		assertThat(JsonPath.<String>read(json, "$.data.processes[0].code")).isEqualTo("NATURAL");
		assertThat(JsonPath.<String>read(json, "$.data.origins[0].name")).isEqualTo("에티오피아");

		// 저장된 취향 벡터도 갱신됐다
		assertThat(jdbc.queryForObject("""
				SELECT confidence FROM taste_profiles p JOIN users u ON u.id = p.user_id
				WHERE p.note_count = 3 AND u.id = (SELECT user_id FROM tasting_notes ORDER BY id DESC LIMIT 1)
				""", String.class)).isEqualTo("FORMING");
	}

	private long tagId(String code) {
		return jdbc.queryForObject("SELECT id FROM flavor_tags WHERE code = ?", Long.class, code);
	}

}
