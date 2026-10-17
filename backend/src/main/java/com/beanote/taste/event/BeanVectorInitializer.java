package com.beanote.taste.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.beanote.taste.service.BeanVectorService;

/**
 * 서버 시작 시 모든 원두 벡터를 다시 계산한다. 시드 원두에 벡터를 채우고, 추정표·K 같은 설정값이 바뀌어도 벡터가 맞도록 한다.
 * 원두 1,000개 기준 수백 ms 수준이라 시작 시간에 큰 영향이 없다.
 */
@Component
public class BeanVectorInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BeanVectorInitializer.class);

	private final BeanVectorService beanVectorService;

	public BeanVectorInitializer(BeanVectorService beanVectorService) {
		this.beanVectorService = beanVectorService;
	}

	@Override
	public void run(ApplicationArguments args) {
		long start = System.nanoTime();
		int count = beanVectorService.recalculateAll();
		log.info("원두 벡터 {}개 계산 ({} ms)", count, (System.nanoTime() - start) / 1_000_000);
	}

}
