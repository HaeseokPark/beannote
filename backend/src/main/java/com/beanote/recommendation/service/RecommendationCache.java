package com.beanote.recommendation.service;

import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.beanote.recommendation.dto.RecommendationResponse;
import com.beanote.taste.config.RecommendationProperties;
import com.beanote.taste.event.TasteInputChangedEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 사용자별 추천 결과 캐시 (5-6). 30분 유지, 본인 노트·설문·피드백이 바뀌면 커밋 뒤에 무효화한다.
 * 커밋 전에 지우면 그 사이 요청이 바뀌기 전 데이터로 다시 캐시를 채울 수 있어서 커밋 뒤에 지운다.
 * 서버 1대 기준 로컬 메모리(Caffeine)이고, 서버를 늘리면 Redis 로 옮긴다 (P2).
 */
@Component
public class RecommendationCache {

	private final Cache<Long, RecommendationResponse> cache;

	public RecommendationCache(RecommendationProperties properties) {
		this.cache = Caffeine.newBuilder()
				.expireAfterWrite(properties.cacheTtl())
				.maximumSize(100_000)
				.build();
	}

	public RecommendationResponse get(Long userId, Function<Long, RecommendationResponse> loader) {
		return cache.get(userId, loader);
	}

	public boolean contains(Long userId) {
		return cache.getIfPresent(userId) != null;
	}

	@TransactionalEventListener
	public void on(TasteInputChangedEvent event) {
		if (event.userId() != null) {
			cache.invalidate(event.userId());
		}
	}

	/** 진행 중인 트랜잭션이 커밋된 뒤 무효화한다. 트랜잭션 밖이면 바로 지운다. */
	public void invalidateAfterCommit(Long userId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			cache.invalidate(userId);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				cache.invalidate(userId);
			}
		});
	}

}
