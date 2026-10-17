package com.beanote.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.beanote.recommendation.service.RecommendationCache;
import com.beanote.support.IntegrationTest;
import com.beanote.support.TestApi;
import com.beanote.taste.service.BeanVectorService;

/**
 * 추천 API 응답 시간 측정 (Phase 5 완료 조건, 5-6 목표: 원두 1,000개 · 사용자 1,000명 기준 캐시 미스 p95 300ms, 히트 50ms).
 * 일반 테스트에서 빠지고 ./gradlew perfTest 로 따로 실행한다. 결과는 reports/recommendation-perf.md.
 */
@Tag("perf")
@IntegrationTest
class RecommendationPerformanceTest {

	private static final int TOTAL_BEANS = 1000;
	private static final int USERS = 1000;
	private static final int NOTES_PER_USER = 10;
	private static final int WARMUP = 30;
	private static final int RUNS = 200;
	private static final Path REPORT = Path.of("..", "reports", "recommendation-perf.md");

	private static final String[] ORIGINS = { "ET", "KE", "CO", "BR", "GT", "CR", "PA", "RW", "PE", "ID" };
	private static final String[] PROCESSES = { "WASHED", "NATURAL", "HONEY", "ANAEROBIC" };
	private static final String[] ROASTS = { "LIGHT", "MEDIUM_LIGHT", "MEDIUM", "MEDIUM_DARK", "DARK" };

	@Autowired
	MockMvcTester mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	BeanVectorService beanVectorService;

	@Autowired
	RecommendationCache cache;

	private final Random random = new Random(20261017);

	@Test
	void measureRecommendationLatency() throws IOException {
		long setupStart = System.nanoTime();
		insertBeans();
		insertUsersAndNotes();
		recalculateStats();
		long vectorStart = System.nanoTime();
		int vectors = beanVectorService.recalculateAll();
		long vectorMs = (System.nanoTime() - vectorStart) / 1_000_000;
		long setupMs = (System.nanoTime() - setupStart) / 1_000_000;

		TestApi api = new TestApi(mvc);
		String token = api.newUserToken();
		api.put("/api/v1/survey", token, """
				{"coffeeHabit":"POUR_OVER",
				 "preferences":{"acidity":5,"sweetness":3,"body":2,"bitterness":1,"aftertaste":3},
				 "likedCategories":["FLORAL","CITRUS"],"dislikedCategories":["ROASTED"]}""");
		for (int i = 0; i < NOTES_PER_USER; i++) {
			api.createNote(token, TestApi.noteJson(1 + random.nextInt(TOTAL_BEANS),
					String.valueOf(1 + random.nextInt(9) * 0.5), 1 + random.nextInt(5), "PRIVATE", "[]"));
		}
		Long userId = Long.valueOf(TestApi.read(api.get("/api/v1/users/me", token), "$.data.id"));

		for (int i = 0; i < WARMUP; i++) {
			cache.invalidateAfterCommit(userId);
			api.get("/api/v1/recommendations", token);
		}
		double[] miss = new double[RUNS];
		double[] hit = new double[RUNS];
		for (int i = 0; i < RUNS; i++) {
			cache.invalidateAfterCommit(userId);
			miss[i] = timeMs(() -> assertThat(api.get("/api/v1/recommendations", token)).hasStatusOk());
			hit[i] = timeMs(() -> assertThat(api.get("/api/v1/recommendations", token)).hasStatusOk());
		}

		int beans = jdbc.queryForObject("SELECT COUNT(*) FROM beans", Integer.class);
		int users = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
		int notes = jdbc.queryForObject("SELECT COUNT(*) FROM tasting_notes", Integer.class);
		writeReport(beans, users, notes, vectors, vectorMs, setupMs, miss, hit);

		assertThat(percentile(miss, 95)).as("캐시 미스 p95 (ms)").isLessThan(300);
		assertThat(percentile(hit, 95)).as("캐시 히트 p95 (ms)").isLessThan(50);
	}

	private void insertBeans() {
		int existing = jdbc.queryForObject("SELECT COUNT(*) FROM beans", Integer.class);
		List<Long> tagIds = jdbc.queryForList("SELECT id FROM flavor_tags", Long.class);
		List<Object[]> beans = new ArrayList<>();
		List<Object[]> flavors = new ArrayList<>();
		for (int id = existing + 1; id <= TOTAL_BEANS; id++) {
			String name = "성능 측정 원두 " + id;
			beans.add(new Object[] { id, 1 + random.nextInt(12), name, name.replace(" ", ""), pick(ORIGINS),
					pick(PROCESSES), pick(ROASTS) });
			for (Long tag : List.of(tagIds.get(random.nextInt(tagIds.size())), tagIds.get(random.nextInt(tagIds.size())),
					tagIds.get(random.nextInt(tagIds.size())))) {
				flavors.add(new Object[] { id, tag });
			}
		}
		jdbc.batchUpdate("""
				INSERT INTO beans (id, roastery_id, name, name_normalized, origin_country, process, roast_level,
				                   is_blend, status, is_seed, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, FALSE, 'ACTIVE', FALSE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
				""", beans);
		jdbc.batchUpdate("INSERT IGNORE INTO bean_official_flavors (bean_id, flavor_tag_id) VALUES (?, ?)", flavors);
	}

	private void insertUsersAndNotes() {
		List<Object[]> users = new ArrayList<>();
		for (int i = 0; i < USERS; i++) {
			users.add(new Object[] { "perf-" + i + "@beanote.test", "사용자" + i });
		}
		jdbc.batchUpdate("""
				INSERT INTO users (email, password_hash, nickname, role, provider, created_at, updated_at)
				VALUES (?, NULL, ?, 'USER', 'LOCAL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
				""", users);
		List<Long> userIds = jdbc.queryForList("SELECT id FROM users WHERE email LIKE 'perf-%'", Long.class);

		List<Object[]> notes = new ArrayList<>();
		LocalDate today = LocalDate.now();
		for (Long userId : userIds) {
			for (int n = 0; n < NOTES_PER_USER; n++) {
				notes.add(new Object[] { userId, 1 + random.nextInt(TOTAL_BEANS),
						Date.valueOf(today.minusDays(random.nextInt(180))), 1 + random.nextInt(5),
						1 + random.nextInt(5), 1 + random.nextInt(5), 1 + random.nextInt(5), 1 + random.nextInt(5),
						(1 + random.nextInt(10)) * 0.5 });
			}
		}
		jdbc.batchUpdate("""
				INSERT INTO tasting_notes (user_id, bean_id, tasted_on, brew_method, acidity, sweetness, body,
				                           bitterness, aftertaste, rating, visibility, created_at, updated_at)
				VALUES (?, ?, ?, 'POUR_OVER', ?, ?, ?, ?, ?, ?, 'PRIVATE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
				""", notes);
	}

	private void recalculateStats() {
		jdbc.update("""
				INSERT INTO bean_stats (bean_id, note_count, avg_rating, avg_acidity, avg_sweetness, avg_body,
				                        avg_bitterness, avg_aftertaste, updated_at)
				SELECT * FROM (
				    SELECT bean_id, COUNT(*) AS c, AVG(rating) AS r, AVG(acidity) AS a, AVG(sweetness) AS s,
				           AVG(body) AS b, AVG(bitterness) AS bi, AVG(aftertaste) AS af, UTC_TIMESTAMP(6) AS u
				    FROM tasting_notes GROUP BY bean_id
				) AS x
				ON DUPLICATE KEY UPDATE note_count = x.c, avg_rating = x.r, avg_acidity = x.a,
				    avg_sweetness = x.s, avg_body = x.b, avg_bitterness = x.bi, avg_aftertaste = x.af
				""");
	}

	private void writeReport(int beans, int users, int notes, int vectors, long vectorMs, long setupMs,
			double[] miss, double[] hit) throws IOException {
		String md = """
				# 추천 API 응답 시간 측정

				> `./gradlew perfTest` (`RecommendationPerformanceTest`) 가 다시 만든다. 기획서 5-6 목표: 캐시 미스 p95 300ms, 캐시 히트 p95 50ms 이하.
				> 측정은 MockMvc 로 컨트롤러·보안 필터·직렬화까지 포함한 서버 내부 처리 시간이며 네트워크 왕복은 빠져 있다.
				> 데이터는 고정 시드 난수로 만든 가상 데이터다.

				## 환경

				| 항목 | 값 |
				|---|---|
				| 측정일 | %s |
				| CPU 코어 | %d |
				| Java | %s |
				| DB | 로컬 MySQL %s |

				## 데이터

				| 항목 | 값 |
				|---|---|
				| 원두 | %,d |
				| 사용자 | %,d |
				| 노트 | %,d |
				| 원두 벡터 전체 재계산 | %,d개 / %,d ms |
				| 데이터 준비 전체 | %,d ms |

				## 결과 (GET /api/v1/recommendations, 반복 %d회, 예열 %d회)

				| 구분 | p50 | p95 | p99 | 최대 | 목표 (p95) |
				|---|---|---|---|---|---|
				| 캐시 미스 | %.1f ms | **%.1f ms** | %.1f ms | %.1f ms | 300 ms |
				| 캐시 히트 | %.1f ms | **%.1f ms** | %.1f ms | %.1f ms | 50 ms |

				캐시 미스 한 번에 하는 일: 저장된 취향 벡터 읽기 → 제외 대상·탐험용 이력 조회 → 후보 원두 벡터 전체(약 %,d개) 읽기 →
				가중 코사인 점수 계산·정렬 → 상위 10개 원두 요약 조회 → 추천 이유 생성.
				""".formatted(LocalDate.now(), Runtime.getRuntime().availableProcessors(),
				System.getProperty("java.version"), jdbc.queryForObject("SELECT VERSION()", String.class),
				beans, users, notes, vectors, vectorMs, setupMs, RUNS, WARMUP,
				percentile(miss, 50), percentile(miss, 95), percentile(miss, 99), max(miss),
				percentile(hit, 50), percentile(hit, 95), percentile(hit, 99), max(hit), beans);
		Files.createDirectories(REPORT.getParent());
		Files.writeString(REPORT, md, StandardCharsets.UTF_8);
	}

	private String pick(String[] values) {
		return values[random.nextInt(values.length)];
	}

	private static double timeMs(Runnable action) {
		long start = System.nanoTime();
		action.run();
		return (System.nanoTime() - start) / 1_000_000.0;
	}

	private static double percentile(double[] values, int p) {
		double[] sorted = values.clone();
		Arrays.sort(sorted);
		int index = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
		return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
	}

	private static double max(double[] values) {
		return Arrays.stream(values).max().orElse(0);
	}

}
