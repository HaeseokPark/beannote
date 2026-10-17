package com.beanote.bean;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.beanote.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManagerFactory;

/**
 * 원두 목록·검색·상세 (B-1, B-2). 기대값은 SQL 로 직접 센 값과 비교한다.
 */
@IntegrationTest
class BeanSearchIntegrationTest {

	@Autowired
	MockMvcTester mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	EntityManagerFactory entityManagerFactory;

	@Test
	void listIsPublicAndPagesThroughAllBeansWithoutDuplicates() {
		List<Integer> ids = collectIds("/api/v1/beans?size=7");

		int listed = count("SELECT COUNT(*) FROM beans WHERE merged_into_id IS NULL AND status <> 'PENDING'");
		assertThat(ids).hasSize(listed).doesNotHaveDuplicates();
		// 기본 정렬은 최신순(id 내림차순)
		assertThat(ids).isSortedAccordingTo((a, b) -> Integer.compare(b, a));
	}

	@Test
	void nameSortOrdersByNameAcrossPages() {
		List<String> names = new ArrayList<>();
		String cursor = null;
		do {
			String json = body(mvc.get().uri("/api/v1/beans?sort=NAME&size=9"
					+ (cursor == null ? "" : "&cursor=" + cursor)).exchange());
			names.addAll(JsonPath.read(json, "$.data.items[*].name"));
			cursor = JsonPath.read(json, "$.data.nextCursor");
		}
		while (cursor != null);

		List<String> expected = jdbc.queryForList(
				"SELECT name FROM beans WHERE merged_into_id IS NULL AND status <> 'PENDING' ORDER BY name, id",
				String.class);
		assertThat(names).containsExactlyElementsOf(expected);
	}

	@Test
	void filtersByOriginIncludingBlendComponents() {
		int expected = count("""
				SELECT COUNT(*) FROM beans b WHERE b.origin_country = 'ET'
				   OR EXISTS (SELECT 1 FROM bean_blend_origins o WHERE o.bean_id = b.id AND o.country_code = 'ET')
				""");
		assertThat(collectIds("/api/v1/beans?origin=ET&size=50")).hasSize(expected).hasSizeGreaterThan(8);
	}

	@Test
	void filtersByProcessRoastLevelAndFlavorCategoryTogether() {
		int expected = count("""
				SELECT COUNT(*) FROM beans b
				WHERE b.process = 'NATURAL' AND b.roast_level = 'LIGHT'
				  AND EXISTS (SELECT 1 FROM bean_official_flavors f
				              JOIN flavor_tags t ON t.id = f.flavor_tag_id
				              JOIN flavor_categories c ON c.id = t.category_id
				              WHERE f.bean_id = b.id AND c.code = 'BERRY')
				""");
		List<Integer> ids = collectIds("/api/v1/beans?process=NATURAL&roastLevel=LIGHT&flavorCategory=BERRY");
		assertThat(ids).hasSize(expected).isNotEmpty();
	}

	@Test
	void keywordMatchesBeanNameIgnoringSpacesRoasteryAndCountryName() {
		assertThat(collectIds("/api/v1/beans?q=예가체프코체레")).containsExactly(1);
		assertThat(collectIds("/api/v1/beans?q=모래시계"))
				.hasSize(count("SELECT COUNT(*) FROM beans WHERE roastery_id = 1"));
		// "케냐" 는 국가 이름 → 케냐 싱글오리진 + 케냐가 섞인 블렌드
		assertThat(collectIds("/api/v1/beans?q=케냐&size=50")).hasSize(count("""
				SELECT COUNT(*) FROM beans b WHERE b.origin_country = 'KE'
				   OR EXISTS (SELECT 1 FROM bean_blend_origins o WHERE o.bean_id = b.id AND o.country_code = 'KE')
				"""));
		// LIKE 특수문자는 글자 그대로 검색한다
		assertThat(collectIds("/api/v1/beans?q=%25")).isEmpty();
	}

	@Test
	void invalidFilterOrCursorReturnsInvalidInput() {
		assertThat(mvc.get().uri("/api/v1/beans?process=BOILED"))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		assertThat(mvc.get().uri("/api/v1/beans?cursor=@@@"))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
	}

	@Test
	void detailShowsAttributesAndOfficialFlavors() {
		var body = assertThat(mvc.get().uri("/api/v1/beans/1")).hasStatusOk().bodyJson();
		body.extractingPath("$.data.name").isEqualTo("에티오피아 예가체프 코체레 G1");
		body.extractingPath("$.data.roastery.name").isEqualTo("모래시계 커피 로스터스");
		body.extractingPath("$.data.origins[0].code").isEqualTo("ET");
		body.extractingPath("$.data.origins[0].name").isEqualTo("에티오피아");
		body.extractingPath("$.data.process").isEqualTo("WASHED");
		body.extractingPath("$.data.flavors[*].code").asArray().containsExactly("JASMINE", "BLACK_TEA", "LEMON");
		body.extractingPath("$.data.seed").isEqualTo(true);
	}

	@Test
	void blendDetailListsAllOrigins() {
		assertThat(mvc.get().uri("/api/v1/beans/14")).hasStatusOk()
				.bodyJson().extractingPath("$.data.origins[*].code").asArray().containsExactly("BR", "CO", "GT");
	}

	@Test
	void missingBeanReturnsBeanNotFound() {
		assertThat(mvc.get().uri("/api/v1/beans/999999"))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson().extractingPath("$.error.code").isEqualTo("BEAN_NOT_FOUND");
	}

	@Test
	void listQueryCountDoesNotGrowWithPageSize() {
		// 두 페이지 모두 싱글오리진·블렌드가 섞여 있어야 같은 종류의 배치 조회가 일어난다
		long small = statementsFor("/api/v1/beans?size=10");
		long large = statementsFor("/api/v1/beans?size=50");
		assertThat(large).isEqualTo(small);
	}

	@Test
	void roasteriesArePublic() {
		assertThat(mvc.get().uri("/api/v1/roasteries")).hasStatusOk()
				.bodyJson().extractingPath("$.data.length()").isEqualTo(count("SELECT COUNT(*) FROM roasteries"));
		assertThat(mvc.get().uri("/api/v1/roasteries?q=파도")).hasStatusOk()
				.bodyJson().extractingPath("$.data[0].name").isEqualTo("파도소리 커피");
	}

	private long statementsFor(String uri) {
		Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		stats.clear();
		assertThat(mvc.get().uri(uri)).hasStatusOk();
		return stats.getPrepareStatementCount();
	}

	private List<Integer> collectIds(String uri) {
		List<Integer> ids = new ArrayList<>();
		String cursor = null;
		do {
			String json = body(mvc.get().uri(uri + (cursor == null ? "" : "&cursor=" + cursor)).exchange());
			ids.addAll(JsonPath.read(json, "$.data.items[*].id"));
			cursor = JsonPath.read(json, "$.data.nextCursor");
		}
		while (cursor != null);
		return ids;
	}

	private int count(String sql) {
		return jdbc.queryForObject(sql, Integer.class);
	}

	private static String body(MvcTestResult result) {
		assertThat(result).hasStatusOk();
		try {
			return result.getResponse().getContentAsString();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}
