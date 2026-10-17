package com.beanote.flavor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.beanote.flavor.domain.MappingResult;
import com.beanote.flavor.matching.FlavorMatch;
import com.beanote.flavor.matching.FlavorMatcher;
import com.beanote.flavor.service.FlavorNormalizationService;
import com.beanote.support.IntegrationTest;

/**
 * 향미 표준화 매핑률 리포트 (Phase 3 완료 조건, F-5 의 첫 버전).
 * 실제 동의어 사전으로 ① 시드 원두 공식 설명 ② 평가용 말뭉치를 판정하고 reports/flavor-mapping.md 에 기록한다.
 */
@IntegrationTest
class FlavorMappingReportTest {

	private static final Path REPORT = Path.of("..", "reports", "flavor-mapping.md");

	@Autowired
	FlavorNormalizationService normalizationService;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void seedDescriptionsAndCorpusMeetMappingTargets() throws IOException {
		FlavorMatcher matcher = normalizationService.matcher();
		Map<Long, String> tagCodes = jdbc.query("SELECT id, code FROM flavor_tags",
				rs -> {
					Map<Long, String> map = new LinkedHashMap<>();
					while (rs.next()) {
						map.put(rs.getLong("id"), rs.getString("code"));
					}
					return map;
				});
		Map<Long, String> categoryCodes = jdbc.query("SELECT id, code FROM flavor_categories",
				rs -> {
					Map<Long, String> map = new LinkedHashMap<>();
					while (rs.next()) {
						map.put(rs.getLong("id"), rs.getString("code"));
					}
					return map;
				});

		SeedResult seed = evaluateSeed(matcher, tagCodes);
		List<CorpusRow> corpus = evaluateCorpus(matcher, tagCodes, categoryCodes);

		writeReport(seed, corpus);

		assertThat(seed.mappingRate()).as("시드 공식 설명 자동 매핑률").isGreaterThanOrEqualTo(0.95);
		assertThat(seed.agreementRate()).as("자동 매핑 태그와 시드 태그 일치율").isGreaterThanOrEqualTo(0.95);
		double accuracy = corpus.stream().filter(CorpusRow::correct).count() / (double) corpus.size();
		assertThat(accuracy).as("말뭉치 정확도").isGreaterThanOrEqualTo(0.85);
	}

	private SeedResult evaluateSeed(FlavorMatcher matcher, Map<Long, String> tagCodes) {
		List<Map<String, Object>> beans = jdbc.queryForList(
				"SELECT id, official_flavor_text FROM beans WHERE is_seed = TRUE AND official_flavor_text IS NOT NULL");
		Map<Long, Set<String>> seededTags = jdbc.query("""
				SELECT f.bean_id, t.code FROM bean_official_flavors f JOIN flavor_tags t ON t.id = f.flavor_tag_id
				""", rs -> {
			Map<Long, Set<String>> map = new LinkedHashMap<>();
			while (rs.next()) {
				map.computeIfAbsent(rs.getLong("bean_id"), k -> new java.util.HashSet<>()).add(rs.getString("code"));
			}
			return map;
		});

		Map<MappingResult, Integer> counts = new EnumMap<>(MappingResult.class);
		int segments = 0;
		int agreeing = 0;
		int confident = 0;
		List<String> misses = new ArrayList<>();
		for (Map<String, Object> bean : beans) {
			long beanId = ((Number) bean.get("id")).longValue();
			for (FlavorMatch match : matcher.matchAll((String) bean.get("official_flavor_text"))) {
				segments++;
				counts.merge(match.result(), 1, Integer::sum);
				if (match.result().isConfident() && match.entry().tagId() != null) {
					confident++;
					if (seededTags.getOrDefault(beanId, Set.of()).contains(tagCodes.get(match.entry().tagId()))) {
						agreeing++;
					}
				}
				else {
					misses.add(match.raw() + " (" + match.result() + ")");
				}
			}
		}
		return new SeedResult(beans.size(), segments, counts, confident, agreeing, misses);
	}

	private List<CorpusRow> evaluateCorpus(FlavorMatcher matcher, Map<Long, String> tagCodes,
			Map<Long, String> categoryCodes) throws IOException {
		List<CorpusRow> rows = new ArrayList<>();
		try (InputStream in = getClass().getResourceAsStream("/flavor/expression-corpus.tsv")) {
			for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] cols = line.split("\t");
				FlavorMatch match = matcher.match(cols[0]);
				String actual = match.entry() == null ? "NONE"
						: match.entry().tagId() != null ? "TAG:" + tagCodes.get(match.entry().tagId())
								: "CAT:" + categoryCodes.get(match.entry().categoryId());
				boolean correct = cols[1].equals("NONE")
						? !match.result().isConfident()
						: match.result().isConfident() && actual.equals(cols[1]);
				rows.add(new CorpusRow(cols[0], cols[1], cols[2], match.result(), match.score(), actual, correct));
			}
		}
		return rows;
	}

	private void writeReport(SeedResult seed, List<CorpusRow> corpus) throws IOException {
		StringBuilder md = new StringBuilder();
		md.append("# 향미 표현 표준화 매핑률 리포트\n\n")
				.append("> `FlavorMappingReportTest` 가 테스트 실행 때마다 다시 만든다. 판정 기준: AUTO ≥ 85점, REVIEW 70~85점, UNMAPPED < 70점.\n")
				.append("> 시드 원두와 말뭉치는 직접 만든 예시 데이터이며, 실제 로스터리 설명 전체를 대표하지 않는다.\n\n");

		md.append("## 1. 시드 원두 공식 설명\n\n")
				.append("시드 설명은 표준 태그 이름으로 작성했기 때문에 높은 매핑률이 당연하다. 사전·정규화가 깨지지 않았는지 확인하는 회귀 지표로 보고, ")
				.append("실제 판정 성능은 2장 말뭉치로 본다. 말뭉치도 판정기와 함께 만들었으므로 낙관적인 수치일 수 있다.\n\n")
				.append("| 항목 | 값 |\n|---|---|\n")
				.append("| 원두 수 | ").append(seed.beans()).append(" |\n")
				.append("| 표현 조각 수 | ").append(seed.segments()).append(" |\n");
		for (MappingResult result : MappingResult.values()) {
			md.append("| ").append(result).append(" | ").append(seed.counts().getOrDefault(result, 0)).append(" |\n");
		}
		md.append("| **자동 매핑률** (EXACT+AUTO) | **").append(percent(seed.mappingRate())).append("** |\n")
				.append("| 자동 매핑 태그가 시드 태그와 일치 | ").append(percent(seed.agreementRate())).append(" |\n\n");
		if (!seed.misses().isEmpty()) {
			md.append("매핑되지 않은 표현: ").append(String.join(", ", seed.misses())).append("\n\n");
		}

		md.append("## 2. 평가용 말뭉치 (`src/test/resources/flavor/expression-corpus.tsv`)\n\n")
				.append("| 유형 | 표현 수 | 정답 | 정확도 |\n|---|---|---|---|\n");
		Map<String, List<CorpusRow>> byKind = corpus.stream()
				.collect(Collectors.groupingBy(CorpusRow::kind, LinkedHashMap::new, Collectors.toList()));
		byKind.forEach((kind, rows) -> {
			long correct = rows.stream().filter(CorpusRow::correct).count();
			md.append("| ").append(kind).append(" | ").append(rows.size()).append(" | ").append(correct).append(" | ")
					.append(percent(correct / (double) rows.size())).append(" |\n");
		});
		long totalCorrect = corpus.stream().filter(CorpusRow::correct).count();
		md.append("| **전체** | ").append(corpus.size()).append(" | ").append(totalCorrect).append(" | **")
				.append(percent(totalCorrect / (double) corpus.size())).append("** |\n\n");

		md.append("### 표현별 결과\n\n| 표현 | 기대 | 판정 | 점수 | 결과 | 정답 |\n|---|---|---|---|---|---|\n");
		for (CorpusRow row : corpus) {
			md.append("| ").append(row.expression()).append(" | ").append(row.expected()).append(" | ")
					.append(row.result()).append(" | ").append(String.format("%.1f", row.score())).append(" | ")
					.append(row.actual()).append(" | ").append(row.correct() ? "O" : "**X**").append(" |\n");
		}
		md.append("\nNONE 표현은 표준 태그로 확정(EXACT·AUTO)하지 않으면 정답으로 본다. REVIEW 는 관리자 검토 큐(F-4)로 간다.\n");

		Files.createDirectories(REPORT.getParent());
		Files.writeString(REPORT, md.toString(), StandardCharsets.UTF_8);
	}

	private static String percent(double rate) {
		return String.format("%.1f%%", rate * 100);
	}

	private record SeedResult(int beans, int segments, Map<MappingResult, Integer> counts, int confident,
			int agreeing, List<String> misses) {

		double mappingRate() {
			return segments == 0 ? 0 : confident / (double) segments;
		}

		double agreementRate() {
			return confident == 0 ? 0 : agreeing / (double) confident;
		}

	}

	private record CorpusRow(String expression, String expected, String kind, MappingResult result, double score,
			String actual, boolean correct) {
	}

}
