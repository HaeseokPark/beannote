package com.beanote.taste.event;

import java.util.Set;

/**
 * 취향·추천 계산의 입력이 바뀌었다 (노트 작성·수정·삭제, 설문 저장, 추천 피드백, 원두 등록).
 * 지금은 같은 트랜잭션 안에서 동기로 처리하고, 비동기 처리(5-6 P1)로 옮길 때 이 이벤트를 그대로 쓴다.
 *
 * @param userId  취향이 바뀐 사용자 (원두만 바뀐 경우 null)
 * @param beanIds 벡터를 다시 계산할 원두
 */
public record TasteInputChangedEvent(Long userId, Set<Long> beanIds) {

	public static TasteInputChangedEvent ofUser(Long userId) {
		return new TasteInputChangedEvent(userId, Set.of());
	}

	public static TasteInputChangedEvent ofBean(Long beanId) {
		return new TasteInputChangedEvent(null, Set.of(beanId));
	}

}
