package com.beanote.recommendation.domain;

/**
 * 추천 피드백 (R-3). 두 경우 모두 이후 추천에서 뺀다.
 * ALREADY_TRIED 는 화면에서 노트 작성으로 이어진다.
 */
public enum FeedbackType {
	NOT_INTERESTED, ALREADY_TRIED
}
