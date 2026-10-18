package com.beanote.recommendation.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;
import com.beanote.recommendation.model.Candidate;
import com.beanote.recommendation.model.Neighbor;
import com.beanote.taste.model.VectorCodec;

/**
 * 추천 계산용 조회. 후보 전체를 한 번에 읽어야 해서 엔티티 대신 필요한 열만 SQL 로 읽는다.
 */
@Repository
public class RecommendationQueries {

	private final NamedParameterJdbcTemplate jdbc;

	public RecommendationQueries(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** 추천 가능한 원두 (판매 중, 병합되지 않음, 벡터 있음) */
	public List<Candidate> loadCandidates() {
		Map<Long, Set<String>> blendOrigins = new HashMap<>();
		jdbc.query("SELECT bean_id, country_code FROM bean_blend_origins", Map.of(),
				rs -> {
					blendOrigins.computeIfAbsent(rs.getLong("bean_id"), k -> new LinkedHashSet<>())
							.add(rs.getString("country_code"));
				});
		List<Candidate> result = new ArrayList<>();
		jdbc.query("""
				SELECT b.id, b.origin_country, b.process, b.roast_level, s.vector
				FROM beans b JOIN bean_stats s ON s.bean_id = b.id
				WHERE b.merged_into_id IS NULL AND b.status = 'ACTIVE' AND s.vector IS NOT NULL
				""", Map.of(), rs -> {
			long id = rs.getLong("id");
			String origin = rs.getString("origin_country");
			Set<String> origins = origin != null ? Set.of(origin) : blendOrigins.getOrDefault(id, Set.of());
			result.add(new Candidate(id, VectorCodec.fromJson(rs.getString("vector")), origins, origin,
					ProcessMethod.valueOf(rs.getString("process")), RoastLevel.valueOf(rs.getString("roast_level"))));
		});
		return result;
	}

	/** 노트를 쓴 원두와 피드백을 준 원두 (추천 제외 대상) */
	public Set<Long> excludedBeanIds(long userId) {
		return new HashSet<>(jdbc.queryForList("""
				SELECT bean_id FROM tasting_notes WHERE user_id = :userId
				UNION
				SELECT bean_id FROM recommendation_feedback WHERE user_id = :userId
				""", Map.of("userId", userId), Long.class));
	}

	/** 노트를 쓴 원두들의 원산지와 가공 방식 (탐험 가점 판단용) */
	public TriedAttributes triedAttributes(long userId) {
		Set<String> origins = new HashSet<>();
		Set<ProcessMethod> processes = new HashSet<>();
		MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
		jdbc.query("""
				SELECT DISTINCT b.origin_country, b.process
				FROM tasting_notes n JOIN beans b ON b.id = n.bean_id
				WHERE n.user_id = :userId
				""", params, rs -> {
			if (rs.getString("origin_country") != null) {
				origins.add(rs.getString("origin_country"));
			}
			processes.add(ProcessMethod.valueOf(rs.getString("process")));
		});
		origins.addAll(jdbc.queryForList("""
				SELECT DISTINCT o.country_code
				FROM tasting_notes n JOIN bean_blend_origins o ON o.bean_id = n.bean_id
				WHERE n.user_id = :userId
				""", params, String.class));
		return new TriedAttributes(origins, processes);
	}

	public Set<String> dislikedCategories(long userId) {
		return new HashSet<>(jdbc.queryForList("""
				SELECT c.code FROM taste_survey_flavors f JOIN flavor_categories c ON c.id = f.category_id
				WHERE f.user_id = :userId AND f.preference = 'DISLIKE'
				""", Map.of("userId", userId), String.class));
	}

	/** 인기순 (노트 수 → 평균 별점 → 최신). 취향 정보가 없는 사용자용 (O-4) */
	public List<Long> popularBeanIds(Collection<Long> excluded, int limit) {
		MapSqlParameterSource params = new MapSqlParameterSource("limit", limit)
				.addValue("excluded", excluded.isEmpty() ? List.of(-1L) : excluded);
		return jdbc.queryForList("""
				SELECT b.id FROM beans b LEFT JOIN bean_stats s ON s.bean_id = b.id
				WHERE b.merged_into_id IS NULL AND b.status = 'ACTIVE' AND b.id NOT IN (:excluded)
				ORDER BY COALESCE(s.note_count, 0) DESC, COALESCE(s.avg_rating, 0) DESC, b.id DESC
				LIMIT :limit
				""", params, Long.class);
	}

	public int activeUserCount() {
		Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE deleted_at IS NULL", Map.of(),
				Integer.class);
		return count == null ? 0 : count;
	}

	/** 배치가 계산해 둔 이웃 */
	public List<Neighbor> neighbors(long userId) {
		return jdbc.query("""
				SELECT neighbor_id, similarity FROM user_neighbors WHERE user_id = :userId ORDER BY similarity DESC
				""", Map.of("userId", userId),
				(rs, i) -> new Neighbor(rs.getLong("neighbor_id"), rs.getDouble("similarity")));
	}

	/** 사용자별 원두 → 별점 (협업 점수 계산용, 사용자 평균을 내려고 노트 전체를 읽는다) */
	public Map<Long, Map<Long, Double>> ratingsByUser(Collection<Long> userIds) {
		Map<Long, Map<Long, Double>> result = new HashMap<>();
		if (userIds.isEmpty()) {
			return result;
		}
		jdbc.query("SELECT user_id, bean_id, rating FROM tasting_notes WHERE user_id IN (:ids)",
				Map.of("ids", userIds), rs -> {
					// 같은 원두를 여러 번 마셨으면 마지막 평점
					result.computeIfAbsent(rs.getLong("user_id"), k -> new HashMap<>())
							.put(rs.getLong("bean_id"), rs.getDouble("rating"));
				});
		return result;
	}

	public record TriedAttributes(Set<String> origins, Set<ProcessMethod> processes) {
	}

}
