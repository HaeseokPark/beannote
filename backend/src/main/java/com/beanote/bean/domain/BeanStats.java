package com.beanote.bean.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 원두별 노트 통계. 쓰기는 BeanStatsRepository.recalculate (집계 SQL 한 번) 로만 한다.
 */
@Entity
@Immutable
@Table(name = "bean_stats")
public class BeanStats {

	@Id
	@Column(name = "bean_id")
	private Long beanId;

	@Column(name = "note_count", nullable = false)
	private int noteCount;

	@Column(name = "avg_rating")
	private BigDecimal avgRating;

	@Column(name = "avg_acidity")
	private BigDecimal avgAcidity;

	@Column(name = "avg_sweetness")
	private BigDecimal avgSweetness;

	@Column(name = "avg_body")
	private BigDecimal avgBody;

	@Column(name = "avg_bitterness")
	private BigDecimal avgBitterness;

	@Column(name = "avg_aftertaste")
	private BigDecimal avgAftertaste;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected BeanStats() {
	}

	public Long getBeanId() {
		return beanId;
	}

	public int getNoteCount() {
		return noteCount;
	}

	public BigDecimal getAvgRating() {
		return avgRating;
	}

	public BigDecimal getAvgAcidity() {
		return avgAcidity;
	}

	public BigDecimal getAvgSweetness() {
		return avgSweetness;
	}

	public BigDecimal getAvgBody() {
		return avgBody;
	}

	public BigDecimal getAvgBitterness() {
		return avgBitterness;
	}

	public BigDecimal getAvgAftertaste() {
		return avgAftertaste;
	}

}
