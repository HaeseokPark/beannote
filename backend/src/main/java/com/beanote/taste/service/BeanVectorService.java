package com.beanote.taste.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;
import com.beanote.taste.model.BeanFeatures;
import com.beanote.taste.model.BeanVectorCalculator;
import com.beanote.taste.model.VectorCodec;

/**
 * 원두 벡터 계산·저장 (5-1, 5-6). 벡터는 bean_stats.vector(JSON) 에 둔다.
 * 원두 하나당 몇 개의 집계 쿼리를 묶어서 처리하도록 SQL 로 직접 읽는다.
 */
@Service
public class BeanVectorService {

	private static final int CHUNK = 500;

	private final NamedParameterJdbcTemplate jdbc;
	private final BeanVectorCalculator calculator;

	public BeanVectorService(NamedParameterJdbcTemplate jdbc, BeanVectorCalculator calculator) {
		this.jdbc = jdbc;
		this.calculator = calculator;
	}

	/** 병합되지 않은 모든 원두의 벡터를 다시 계산한다. 서버 시작 시 실행 (설정값이 바뀌어도 벡터가 맞도록). */
	@Transactional
	public int recalculateAll() {
		List<Long> ids = jdbc.queryForList("SELECT id FROM beans WHERE merged_into_id IS NULL", Map.of(), Long.class);
		for (int i = 0; i < ids.size(); i += CHUNK) {
			recalculate(ids.subList(i, Math.min(ids.size(), i + CHUNK)));
		}
		return ids.size();
	}

	@Transactional
	public void recalculate(Collection<Long> beanIds) {
		if (beanIds.isEmpty()) {
			return;
		}
		MapSqlParameterSource ids = new MapSqlParameterSource("ids", beanIds);
		Map<Long, double[]> vectors = new HashMap<>();
		Map<Long, Map<String, Integer>> official = categoryCounts("""
				SELECT f.bean_id AS bean_id, c.code AS code, COUNT(*) AS cnt
				FROM bean_official_flavors f
				JOIN flavor_tags t ON t.id = f.flavor_tag_id
				JOIN flavor_categories c ON c.id = t.category_id
				WHERE f.bean_id IN (:ids)
				GROUP BY f.bean_id, c.code
				""", ids);
		Map<Long, Map<String, Integer>> fromNotes = categoryCounts("""
				SELECT n.bean_id AS bean_id, c.code AS code, COUNT(*) AS cnt
				FROM tasting_note_flavors nf
				JOIN tasting_notes n ON n.id = nf.note_id
				JOIN flavor_tags t ON t.id = nf.flavor_tag_id
				JOIN flavor_categories c ON c.id = t.category_id
				WHERE n.bean_id IN (:ids)
				GROUP BY n.bean_id, c.code
				""", ids);
		Map<Long, NoteStats> stats = new HashMap<>();
		jdbc.query("""
				SELECT bean_id, note_count, avg_acidity, avg_sweetness, avg_body, avg_bitterness, avg_aftertaste
				FROM bean_stats WHERE bean_id IN (:ids)
				""", ids, rs -> {
			int count = rs.getInt("note_count");
			double[] axes = count == 0 ? null : new double[] { rs.getDouble("avg_acidity"),
					rs.getDouble("avg_sweetness"), rs.getDouble("avg_body"), rs.getDouble("avg_bitterness"),
					rs.getDouble("avg_aftertaste") };
			stats.put(rs.getLong("bean_id"), new NoteStats(count, axes));
		});

		jdbc.query("SELECT id, process, roast_level FROM beans WHERE id IN (:ids)", ids, rs -> {
			long id = rs.getLong("id");
			NoteStats s = stats.getOrDefault(id, new NoteStats(0, null));
			BeanFeatures features = new BeanFeatures(ProcessMethod.valueOf(rs.getString("process")),
					RoastLevel.valueOf(rs.getString("roast_level")), official.getOrDefault(id, Map.of()), s.count(),
					s.axes(), fromNotes.getOrDefault(id, Map.of()));
			vectors.put(id, calculator.calculate(features));
		});

		List<MapSqlParameterSource> batch = new ArrayList<>();
		vectors.forEach((id, vector) -> batch.add(
				new MapSqlParameterSource("id", id).addValue("vector", VectorCodec.toJson(vector))));
		// 통계 행이 없는 원두(노트 0개)는 note_count 0 으로 만든다
		jdbc.batchUpdate("""
				INSERT INTO bean_stats (bean_id, note_count, updated_at, vector)
				VALUES (:id, 0, UTC_TIMESTAMP(6), CAST(:vector AS JSON))
				ON DUPLICATE KEY UPDATE vector = CAST(:vector AS JSON)
				""", batch.toArray(MapSqlParameterSource[]::new));
	}

	/** 저장된 원두 벡터. 벡터가 없는 원두는 먼저 계산한다. */
	@Transactional
	public Map<Long, double[]> loadVectors(Collection<Long> beanIds) {
		if (beanIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, double[]> result = readVectors(beanIds);
		List<Long> missing = beanIds.stream().filter(id -> !result.containsKey(id)).toList();
		if (!missing.isEmpty()) {
			recalculate(missing);
			result.putAll(readVectors(missing));
		}
		return result;
	}

	private Map<Long, double[]> readVectors(Collection<Long> beanIds) {
		Map<Long, double[]> result = new HashMap<>();
		jdbc.query("SELECT bean_id, vector FROM bean_stats WHERE bean_id IN (:ids) AND vector IS NOT NULL",
				new MapSqlParameterSource("ids", beanIds),
				rs -> {
					result.put(rs.getLong("bean_id"), VectorCodec.fromJson(rs.getString("vector")));
				});
		return result;
	}

	private Map<Long, Map<String, Integer>> categoryCounts(String sql, MapSqlParameterSource params) {
		Map<Long, Map<String, Integer>> result = new HashMap<>();
		jdbc.query(sql, params, rs -> {
			result.computeIfAbsent(rs.getLong("bean_id"), k -> new HashMap<>())
					.put(rs.getString("code"), rs.getInt("cnt"));
		});
		return result;
	}

	private record NoteStats(int count, double[] axes) {
	}

}
