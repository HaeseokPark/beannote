package com.beanote.recommendation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanote.bean.service.BeanService;
import com.beanote.global.error.BusinessException;
import com.beanote.global.error.ErrorCode;
import com.beanote.recommendation.domain.FeedbackType;
import com.beanote.recommendation.domain.RecommendationFeedback;
import com.beanote.recommendation.repository.RecommendationFeedbackRepository;

/**
 * 추천 피드백 (R-3). 같은 피드백을 여러 번 보내도 한 번만 저장한다.
 */
@Service
@Transactional
public class FeedbackService {

	private final RecommendationFeedbackRepository feedbackRepository;
	private final BeanService beanService;
	private final RecommendationCache cache;

	public FeedbackService(RecommendationFeedbackRepository feedbackRepository, BeanService beanService,
			RecommendationCache cache) {
		this.feedbackRepository = feedbackRepository;
		this.beanService = beanService;
		this.cache = cache;
	}

	public void give(Long userId, Long beanId, FeedbackType type) {
		if (!beanService.isListed(beanId)) {
			throw new BusinessException(ErrorCode.BEAN_NOT_FOUND);
		}
		if (!feedbackRepository.existsByUserIdAndBeanIdAndType(userId, beanId, type)) {
			feedbackRepository.save(new RecommendationFeedback(userId, beanId, type));
		}
		cache.invalidateAfterCommit(userId);
	}

}
