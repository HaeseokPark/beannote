package com.beanote.taste.domain;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.beanote.taste.model.TasteVectorCalculator.Confidence;
import com.beanote.taste.model.VectorCodec;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 사용자 취향 벡터 저장본. 협업 필터링 이웃 계산(Phase 7)과 추천 계산에서 읽는다.
 */
@Entity
@Table(name = "taste_profiles")
public class TasteProfile {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "vector")
	private String vector;

	@Column(name = "note_count", nullable = false)
	private int noteCount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Confidence confidence;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected TasteProfile() {
	}

	public TasteProfile(Long userId) {
		this.userId = userId;
	}

	public void update(double[] vector, int noteCount, Confidence confidence) {
		this.vector = vector == null ? null : VectorCodec.toJson(vector);
		this.noteCount = noteCount;
		this.confidence = confidence;
	}

	public double[] vector() {
		return VectorCodec.fromJson(vector);
	}

	public Long getUserId() {
		return userId;
	}

	public int getNoteCount() {
		return noteCount;
	}

	public Confidence getConfidence() {
		return confidence;
	}

}
