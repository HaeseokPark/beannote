package com.beanote.bean;

import static com.beanote.support.TestApi.noteJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.beanote.support.IntegrationTest;
import com.beanote.support.TestApi;
import com.jayway.jsonpath.JsonPath;

/**
 * 노트 작성·수정·삭제 시 원두 통계 갱신 (2-3) 과 통계 기반 정렬 (B-1).
 */
@IntegrationTest
class BeanStatsIntegrationTest {

	@Autowired
	MockMvcTester mvc;

	TestApi api;
	String user;

	@BeforeEach
	void setUp() {
		api = new TestApi(mvc);
		user = api.newUserToken();
	}

	@Test
	void statsFollowNoteCreateUpdateDelete() {
		long bean = api.createFreshBean(user);
		assertStats(bean, 0, null, null);

		long first = api.createNote(user, noteJson(bean, "4.0", 4, "PUBLIC", "[]"));
		// 비공개 노트도 집계에 포함한다 (개별 노트는 노출하지 않음)
		long second = api.createNote(api.newUserToken(), noteJson(bean, "3.0", 2, "PRIVATE", "[]"));
		assertStats(bean, 2, 3.5, 3.0);

		assertThat(api.put("/api/v1/notes/" + first, user, noteJson(bean, "5.0", 5, "PUBLIC", "[]"))).hasStatusOk();
		assertStats(bean, 2, 4.0, 3.5);

		assertThat(api.delete("/api/v1/notes/" + first, user)).hasStatusOk();
		assertStats(bean, 1, 3.0, 2.0);

		assertThat(second).isPositive();
	}

	@Test
	void movingNoteToAnotherBeanUpdatesBoth() {
		long from = api.createFreshBean(user);
		long to = api.createFreshBean(user);
		long note = api.createNote(user, noteJson(from, "4.5", 3, "PRIVATE", "[]"));
		assertStats(from, 1, 4.5, 3.0);

		assertThat(api.put("/api/v1/notes/" + note, user, noteJson(to, "4.5", 3, "PRIVATE", "[]"))).hasStatusOk();

		assertStats(from, 0, null, null);
		assertStats(to, 1, 4.5, 3.0);
	}

	@Test
	void listShowsNoteCountAndPopularSortPutsBeansWithNotesFirst() {
		long bean = api.createFreshBean(user);
		api.createNote(user, noteJson(bean, "4", 3, "PRIVATE", "[]"));
		api.createNote(user, noteJson(bean, "4", 3, "PRIVATE", "[]"));

		List<Map<String, Object>> items = collect("/api/v1/beans?sort=POPULAR&size=7");
		assertThat(items).extracting(item -> item.get("id")).doesNotHaveDuplicates();
		assertThat(items).isSortedAccordingTo((x, y) -> {
			int byCount = Integer.compare((Integer) y.get("noteCount"), (Integer) x.get("noteCount"));
			return byCount != 0 ? byCount : Integer.compare((Integer) y.get("id"), (Integer) x.get("id"));
		});
		assertThat(items.stream().filter(item -> ((Integer) item.get("id")).longValue() == bean).findFirst())
				.hasValueSatisfying(item -> assertThat(item.get("noteCount")).isEqualTo(2));
	}

	@Test
	void ratingSortOrdersByAverageRatingAcrossPages() {
		long top = api.createFreshBean(user);
		api.createNote(user, noteJson(top, "5.0", 3, "PRIVATE", "[]"));

		List<Map<String, Object>> items = collect("/api/v1/beans?sort=RATING&size=9");
		assertThat(items).extracting(item -> item.get("id")).doesNotHaveDuplicates();
		assertThat(items).extracting(item -> rating(item.get("avgRating"))).isSortedAccordingTo((x, y) -> Double.compare(y, x));
		assertThat(rating(items.getFirst().get("avgRating"))).isEqualTo(5.0);
	}

	private void assertStats(long beanId, int count, Double avgRating, Double avgAcidity) {
		var body = assertThat(api.get("/api/v1/beans/" + beanId, null)).hasStatusOk().bodyJson();
		body.extractingPath("$.data.stats.noteCount").isEqualTo(count);
		if (avgRating == null) {
			body.extractingPath("$.data.stats.avgRating").isNull();
			body.extractingPath("$.data.stats.axes").isNull();
		}
		else {
			body.extractingPath("$.data.stats.avgRating").asNumber().satisfies(
					n -> assertThat(n.doubleValue()).isEqualTo(avgRating));
			body.extractingPath("$.data.stats.axes.acidity").asNumber().satisfies(
					n -> assertThat(n.doubleValue()).isEqualTo(avgAcidity));
		}
	}

	private List<Map<String, Object>> collect(String uri) {
		List<Map<String, Object>> items = new ArrayList<>();
		String cursor = null;
		do {
			String json = TestApi.body(api.get(uri + (cursor == null ? "" : "&cursor=" + cursor), null));
			items.addAll(JsonPath.read(json, "$.data.items[*]"));
			cursor = JsonPath.read(json, "$.data.nextCursor");
		}
		while (cursor != null);
		return items;
	}

	private static double rating(Object value) {
		return value == null ? 0.0 : ((Number) value).doubleValue();
	}

}
