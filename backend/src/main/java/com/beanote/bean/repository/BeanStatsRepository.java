package com.beanote.bean.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beanote.bean.domain.BeanStats;

public interface BeanStatsRepository extends JpaRepository<BeanStats, Long> {

	/**
	 * 해당 원두의 노트를 다시 집계해 bean_stats 에 덮어쓴다(없으면 만든다).
	 * 매번 전체를 다시 세므로 동시에 여러 노트가 바뀌어도 마지막 계산이 실제 데이터와 일치한다.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(nativeQuery = true, value = """
			INSERT INTO bean_stats (bean_id, note_count, avg_rating, avg_acidity, avg_sweetness, avg_body,
			                        avg_bitterness, avg_aftertaste, updated_at)
			SELECT * FROM (
			    SELECT :beanId AS bean_id, COUNT(*) AS note_count, AVG(rating) AS avg_rating,
			           AVG(acidity) AS avg_acidity, AVG(sweetness) AS avg_sweetness, AVG(body) AS avg_body,
			           AVG(bitterness) AS avg_bitterness, AVG(aftertaste) AS avg_aftertaste,
			           UTC_TIMESTAMP(6) AS updated_at
			    FROM tasting_notes WHERE bean_id = :beanId
			) AS s
			ON DUPLICATE KEY UPDATE
			    note_count = s.note_count, avg_rating = s.avg_rating, avg_acidity = s.avg_acidity,
			    avg_sweetness = s.avg_sweetness, avg_body = s.avg_body, avg_bitterness = s.avg_bitterness,
			    avg_aftertaste = s.avg_aftertaste, updated_at = s.updated_at
			""")
	void recalculate(@Param("beanId") Long beanId);

}
