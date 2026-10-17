package com.beanote.flavor.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 향미 표현 하나를 표준화한 결과. REVIEW 의 경우 tag/category 는 확정이 아닌 "제안"이다.
 */
@Entity
@Table(name = "flavor_mapping_logs")
public class FlavorMappingLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "raw_text", nullable = false, length = 100)
	private String rawText;

	@Column(nullable = false, length = 100)
	private String normalized;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MappingResult result;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal score;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "flavor_tag_id")
	private FlavorTag flavorTag;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private FlavorCategory category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MappingSource source;

	@Column(name = "source_id")
	private Long sourceId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected FlavorMappingLog() {
	}

	public FlavorMappingLog(String rawText, String normalized, MappingResult result, BigDecimal score,
			FlavorTag flavorTag, FlavorCategory category, MappingSource source) {
		this.rawText = truncate(rawText);
		this.normalized = truncate(normalized);
		this.result = result;
		this.score = score;
		this.flavorTag = flavorTag;
		this.category = category;
		this.source = source;
	}

	public void attachTo(Long sourceId) {
		this.sourceId = sourceId;
	}

	private static String truncate(String text) {
		return text.length() <= 100 ? text : text.substring(0, 100);
	}

	public Long getId() {
		return id;
	}

	public String getRawText() {
		return rawText;
	}

	public String getNormalized() {
		return normalized;
	}

	public MappingResult getResult() {
		return result;
	}

	public BigDecimal getScore() {
		return score;
	}

	public FlavorTag getFlavorTag() {
		return flavorTag;
	}

	public FlavorCategory getCategory() {
		return category;
	}

	public MappingSource getSource() {
		return source;
	}

	public Long getSourceId() {
		return sourceId;
	}

}
