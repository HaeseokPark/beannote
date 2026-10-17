package com.beanote.flavor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

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
 * POST /flavors/normalize 와, 노트 자유 입력·원두 공식 설명에 표준화가 적용되는지 (3-2).
 */
@IntegrationTest
class FlavorNormalizationIntegrationTest {

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
	void normalizeSplitsAndJudgesEachExpressionWithoutSaving() {
		int logsBefore = countLogs();

		String json = TestApi.body(api.post("/api/v1/flavors/normalize", user, """
				{"text":"자스민향, 살짝 자두 같은 느낌 / 블루베라, 플로럴, 타이어"}"""));

		List<Map<String, Object>> items = JsonPath.read(json, "$.data");
		assertThat(items).extracting(item -> item.get("result"))
				.containsExactly("AUTO", "AUTO", "AUTO", "EXACT", "UNMAPPED");
		assertThat(JsonPath.<List<String>>read(json, "$.data[0:3].tag.code"))
				.containsExactly("JASMINE", "PLUM", "BLUEBERRY");
		assertThat(JsonPath.<String>read(json, "$.data[3].category.code")).isEqualTo("FLORAL");
		assertThat((Object) JsonPath.read(json, "$.data[4].tag")).isNull();

		assertThat(countLogs()).isEqualTo(logsBefore);
	}

	@Test
	void normalizeRequiresLoginAndText() {
		assertThat(api.post("/api/v1/flavors/normalize", null, "{\"text\":\"자스민\"}"))
				.hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(api.post("/api/v1/flavors/normalize", user, "{\"text\":\" \"}"))
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void noteFreeFlavorsAreSplitMappedAndLogged() {
		long noteId = api.createNote(user, TestApi.noteJson(1, "4", 3, "PRIVATE", "[]")
				.replace("[\"살짝 자두 같은 느낌\"]", "[\"자두, 꿀\", \"멜론\"]"));

		var body = org.assertj.core.api.Assertions.assertThat(api.get("/api/v1/notes/" + noteId, user)).bodyJson();
		body.extractingPath("$.data.freeFlavors[*].raw").asArray().containsExactlyInAnyOrder("자두", "꿀", "멜론");

		List<Map<String, Object>> logs = jdbc.queryForList("""
				SELECT l.raw_text, l.result, t.code
				FROM tasting_note_free_flavors f
				JOIN flavor_mapping_logs l ON l.id = f.mapping_log_id
				LEFT JOIN flavor_tags t ON t.id = l.flavor_tag_id
				WHERE f.note_id = ? AND l.source = 'NOTE' AND l.source_id = ?
				ORDER BY l.raw_text
				""", noteId, noteId);
		assertThat(logs).extracting(row -> row.get("raw_text") + ":" + row.get("result") + ":" + row.get("code"))
				.containsExactly("꿀:EXACT:HONEY", "멜론:UNMAPPED:null", "자두:EXACT:PLUM");
	}

	@Test
	void beanOfficialFlavorTextBecomesStandardTags() {
		String json = TestApi.body(api.post("/api/v1/beans", user, """
				{"roasteryId":3,"name":"표준화 테스트 원두 %d","blend":false,"originCountry":"KE",
				 "process":"WASHED","roastLevel":"LIGHT","force":true,
				 "officialFlavorText":"Blackcurrant, 자몽향, 쟈스민 그리고 타이어"}""".formatted(System.nanoTime())));

		assertThat(JsonPath.<List<String>>read(json, "$.data.flavors[*].code"))
				.containsExactlyInAnyOrder("BLACKCURRANT", "GRAPEFRUIT", "JASMINE");
		assertThat(JsonPath.<String>read(json, "$.data.officialFlavorText"))
				.isEqualTo("Blackcurrant, 자몽향, 쟈스민 그리고 타이어");

		long beanId = ((Number) JsonPath.read(json, "$.data.id")).longValue();
		assertThat(jdbc.queryForList(
				"SELECT result FROM flavor_mapping_logs WHERE source = 'BEAN' AND source_id = ? ORDER BY id",
				String.class, beanId))
				.containsExactly("EXACT", "AUTO", "EXACT", "UNMAPPED");
	}

	private int countLogs() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM flavor_mapping_logs", Integer.class);
	}

}
