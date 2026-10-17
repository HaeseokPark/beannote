package com.beanote.taste.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.beanote.taste.service.BeanVectorService;
import com.beanote.taste.service.TasteProfileService;

import jakarta.persistence.EntityManager;

/**
 * 노트·설문·원두가 바뀌면 같은 트랜잭션 안에서 원두 벡터와 취향 벡터를 다시 계산한다 (5-6, P0 동기 처리).
 * 원두 벡터를 먼저 고친 뒤 그 벡터로 취향을 계산한다.
 */
@Component
public class TasteEventHandler {

	private final BeanVectorService beanVectorService;
	private final TasteProfileService tasteProfileService;
	private final EntityManager entityManager;

	public TasteEventHandler(BeanVectorService beanVectorService, TasteProfileService tasteProfileService,
			EntityManager entityManager) {
		this.beanVectorService = beanVectorService;
		this.tasteProfileService = tasteProfileService;
		this.entityManager = entityManager;
	}

	@EventListener
	public void on(TasteInputChangedEvent event) {
		// JPA 로 바꾼 내용(노트 향미 연결 등)을 SQL 집계가 볼 수 있게 먼저 DB 에 반영한다
		entityManager.flush();
		if (!event.beanIds().isEmpty()) {
			beanVectorService.recalculate(event.beanIds());
		}
		if (event.userId() != null) {
			tasteProfileService.refresh(event.userId());
		}
	}

}
