package com.beanote.bean;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.beanote.support.IntegrationTest;
import com.beanote.support.TestApi;

/**
 * 사용자 원두 등록과 중복 후보 (B-3).
 */
@IntegrationTest
class BeanRegistrationIntegrationTest {

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
	void duplicatesFindSimilarNamesInSameRoastery() {
		assertThat(api.get("/api/v1/beans/duplicates?roasteryId=1&name=예가체프 코체레", user))
				.hasStatusOk()
				.bodyJson().extractingPath("$.data[*].id").asArray().contains(1);
		// 공백·오타 한 글자 차이도 후보
		assertThat(api.get("/api/v1/beans/duplicates?roasteryId=1&name=케냐니에리 AB", user))
				.hasStatusOk()
				.bodyJson().extractingPath("$.data[*].id").asArray().contains(5);
		assertThat(api.get("/api/v1/beans/duplicates?roasteryId=1&name=완전히 다른 원두", user))
				.hasStatusOk()
				.bodyJson().extractingPath("$.data").asArray().isEmpty();
		assertThat(api.get("/api/v1/beans/duplicates?roasteryId=1&name=x", null)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void similarBeanNeedsForceToRegister() {
		String json = """
				{"roasteryId":1,"name":"에티오피아 예가체프 코체레","blend":false,"originCountry":"ET",
				 "process":"WASHED","roastLevel":"LIGHT","force":%s}""";

		assertThat(api.post("/api/v1/beans", user, json.formatted("false")))
				.hasStatus(HttpStatus.CONFLICT)
				.bodyJson().extractingPath("$.error.code").isEqualTo("BEAN_DUPLICATE_SUSPECTED");

		assertThat(api.post("/api/v1/beans", user, json.formatted("true")))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.isLenientlyEqualTo("""
						{"data":{"name":"에티오피아 예가체프 코체레","roastery":{"id":1},"seed":false,"status":"ACTIVE",
						         "stats":{"noteCount":0}}}
						""");
	}

	@Test
	void cafeCoffeeCreatesRoasteryOnceAndAppearsInSearch() {
		String cafe = "골목 카페 " + UUID.randomUUID().toString().substring(0, 8);
		String json = """
				{"roasteryName":"%s","name":"%s","blend":true,"blendOrigins":["BR","CO"],
				 "process":"OTHER","roastLevel":"MEDIUM_DARK","varieties":["버번"],"flavorTagIds":[],"force":true}""";

		assertThat(api.post("/api/v1/beans", user, json.formatted(cafe, "시그니처 라떼"))).hasStatus(HttpStatus.CREATED)
				.bodyJson().extractingPath("$.data.origins[*].code").asArray().containsExactly("BR", "CO");
		assertThat(api.post("/api/v1/beans", user, json.formatted(cafe, "아메리카노"))).hasStatus(HttpStatus.CREATED);

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM roasteries WHERE name = ?", Integer.class, cafe))
				.isEqualTo(1);
		assertThat(api.get("/api/v1/beans?q=" + cafe, null)).hasStatusOk()
				.bodyJson().extractingPath("$.data.items").asArray().hasSize(2);
	}

	@Test
	void originRulesForSingleOriginAndBlend() {
		assertThat(api.post("/api/v1/beans", user, """
				{"roasteryId":2,"name":"원산지 없는 원두","blend":false,"process":"WASHED","roastLevel":"LIGHT"}"""))
				.hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(api.post("/api/v1/beans", user, """
				{"roasteryId":2,"name":"원산지 하나 블렌드","blend":true,"blendOrigins":["BR"],
				 "process":"OTHER","roastLevel":"MEDIUM"}"""))
				.hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(api.post("/api/v1/beans", user, """
				{"name":"로스터리 없음","blend":false,"originCountry":"BR","process":"WASHED","roastLevel":"LIGHT"}"""))
				.hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(api.post("/api/v1/beans", null, """
				{"roasteryId":2,"name":"x","blend":false,"originCountry":"BR","process":"WASHED","roastLevel":"LIGHT"}"""))
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}


}
