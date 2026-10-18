package com.beanote.recommendation;

import static com.beanote.support.TestApi.noteJson;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.beanote.recommendation.service.NeighborBatchService;
import com.beanote.support.IntegrationTest;
import com.beanote.support.TestApi;

/**
 * 협업 이웃 배치 (5-6): 취향 벡터가 있는 사용자들의 이웃을 user_neighbors 에 다시 쓴다.
 */
@IntegrationTest
class NeighborBatchIntegrationTest {

	static final String BRIGHT = """
			{"coffeeHabit":"POUR_OVER",
			 "preferences":{"acidity":5,"sweetness":3,"body":2,"bitterness":1,"aftertaste":3},
			 "likedCategories":["FLORAL"],"dislikedCategories":[]}""";
	static final String DARK = """
			{"coffeeHabit":"ESPRESSO",
			 "preferences":{"acidity":1,"sweetness":3,"body":5,"bitterness":5,"aftertaste":4},
			 "likedCategories":["ROASTED"],"dislikedCategories":[]}""";

	@Autowired
	MockMvcTester mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	NeighborBatchService batch;

	@Test
	void similarTastesBecomeNeighborsAndOppositesDoNot() {
		TestApi api = new TestApi(mvc);
		String a = api.newUserToken();
		String b = api.newUserToken();
		String c = api.newUserToken();
		api.put("/api/v1/survey", a, BRIGHT);
		api.put("/api/v1/survey", b, BRIGHT.replace("\"acidity\":5", "\"acidity\":4"));
		api.put("/api/v1/survey", c, DARK);
		api.createNote(a, noteJson(1, "4", 4, "PRIVATE", "[]"));

		batch.recalculateAll();

		long idA = userId(api, a);
		long idB = userId(api, b);
		long idC = userId(api, c);
		assertThat(jdbc.queryForList("SELECT neighbor_id FROM user_neighbors WHERE user_id = ?", Long.class, idA))
				.contains(idB)
				.doesNotContain(idC);
		// 배치는 매번 통째로 다시 쓴다
		int before = count();
		batch.recalculateAll();
		assertThat(count()).isEqualTo(before);
	}

	private int count() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM user_neighbors", Integer.class);
	}

	private static long userId(TestApi api, String token) {
		return Long.parseLong(TestApi.read(api.get("/api/v1/users/me", token), "$.data.id"));
	}

}
