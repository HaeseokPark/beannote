package com.beanote.recommendation.domain;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "recommendation_feedback")
public class RecommendationFeedback {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "bean_id", nullable = false)
	private Long beanId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FeedbackType type;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected RecommendationFeedback() {
	}

	public RecommendationFeedback(Long userId, Long beanId, FeedbackType type) {
		this.userId = userId;
		this.beanId = beanId;
		this.type = type;
	}

}
