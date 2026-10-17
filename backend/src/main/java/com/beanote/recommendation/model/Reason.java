package com.beanote.recommendation.model;

/**
 * 추천 이유 한 줄 (R-2).
 */
public record Reason(Type type, String text) {

	public enum Type {
		FLAVOR, TASTE_AXIS, ROAST, PROCESS, EXPLORE, COLLAB, POPULAR, PROFILE
	}

}
