package com.beanote.recommendation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanote.recommendation.model.Neighbor;
import com.beanote.recommendation.model.NeighborFinder;
import com.beanote.taste.config.RecommendationProperties;
import com.beanote.taste.model.VectorCodec;

/**
 * 협업 이웃 배치 (5-6): 매일 새벽 모든 사용자의 이웃을 다시 계산해 user_neighbors 를 통째로 바꾼다.
 * 이웃은 하루 정도 늦게 반영돼도 추천 품질에 큰 차이가 없어서 요청 때마다 계산하지 않는다.
 */
@Service
public class NeighborBatchService {

	private static final Logger log = LoggerFactory.getLogger(NeighborBatchService.class);

	private final NamedParameterJdbcTemplate jdbc;
	private final NeighborFinder finder;

	public NeighborBatchService(NamedParameterJdbcTemplate jdbc, RecommendationProperties properties) {
		this.jdbc = jdbc;
		this.finder = new NeighborFinder(properties.neighborCount(), properties.sectionWeights());
	}

	@Scheduled(cron = "${beanote.recommendation.neighbor-cron}", zone = "Asia/Seoul")
	public void scheduled() {
		recalculateAll();
	}

	@Transactional
	public int recalculateAll() {
		long start = System.nanoTime();
		Map<Long, double[]> vectors = new HashMap<>();
		jdbc.query("SELECT user_id, vector FROM taste_profiles WHERE vector IS NOT NULL", Map.of(),
				rs -> {
					vectors.put(rs.getLong("user_id"), VectorCodec.fromJson(rs.getString("vector")));
				});
		Map<Long, List<Neighbor>> neighbors = finder.findAll(vectors);

		List<MapSqlParameterSource> rows = new ArrayList<>();
		neighbors.forEach((userId, list) -> list.forEach(n -> rows.add(new MapSqlParameterSource()
				.addValue("userId", userId)
				.addValue("neighborId", n.userId())
				.addValue("similarity", n.similarity()))));

		jdbc.update("DELETE FROM user_neighbors", Map.of());
		jdbc.batchUpdate("""
				INSERT INTO user_neighbors (user_id, neighbor_id, similarity, computed_at)
				VALUES (:userId, :neighborId, :similarity, UTC_TIMESTAMP(6))
				""", rows.toArray(MapSqlParameterSource[]::new));

		log.info("협업 이웃 계산: 사용자 {}명, 이웃 {}쌍 ({} ms)", vectors.size(), rows.size(),
				(System.nanoTime() - start) / 1_000_000);
		return rows.size();
	}

}
