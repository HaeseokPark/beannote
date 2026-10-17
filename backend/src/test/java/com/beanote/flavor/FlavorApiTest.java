package com.beanote.flavor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.beanote.support.IntegrationTest;

@IntegrationTest
class FlavorApiTest {

	@Autowired
	MockMvcTester mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void returnsAllCategoriesWithTagsInOrderWithoutLogin() {
		var body = assertThat(mvc.get().uri("/api/v1/flavors")).hasStatusOk().bodyJson();

		body.extractingPath("$.data.length()").isEqualTo(10);
		body.extractingPath("$.data[0].code").isEqualTo("FLORAL");
		body.extractingPath("$.data[0].tags[0].code").isEqualTo("JASMINE");
		body.extractingPath("$.data[0].tags[0].name").isEqualTo("자스민");
		body.extractingPath("$.data[9].code").isEqualTo("GREEN");
		body.extractingPath("$.data[*].tags[*].code").asArray().hasSize(50);
	}

	@Test
	void seedHasCategoriesTagsAndSynonymsInPlannedRange() {
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flavor_categories", Integer.class)).isEqualTo(10);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flavor_tags", Integer.class)).isBetween(40, 50);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flavor_synonyms", Integer.class)).isBetween(150, 200);
	}

	@Test
	void everyTagHasItsKoreanNameAsSynonym() {
		List<String> missing = jdbc.queryForList("""
				SELECT t.code FROM flavor_tags t
				WHERE NOT EXISTS (
				    SELECT 1 FROM flavor_synonyms s
				    WHERE s.flavor_tag_id = t.id AND s.expression_normalized = LOWER(REPLACE(t.name_ko, ' ', '')))
				""", String.class);
		assertThat(missing).isEmpty();
	}

}
