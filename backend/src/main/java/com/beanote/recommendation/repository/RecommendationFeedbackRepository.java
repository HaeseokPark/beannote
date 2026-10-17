package com.beanote.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanote.recommendation.domain.FeedbackType;
import com.beanote.recommendation.domain.RecommendationFeedback;

public interface RecommendationFeedbackRepository extends JpaRepository<RecommendationFeedback, Long> {

	boolean existsByUserIdAndBeanIdAndType(Long userId, Long beanId, FeedbackType type);

}
