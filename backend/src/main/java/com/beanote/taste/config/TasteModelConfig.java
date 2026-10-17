package com.beanote.taste.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.beanote.taste.model.BeanVectorCalculator;
import com.beanote.taste.model.TasteEstimateTable;
import com.beanote.taste.model.TasteVectorCalculator;

/**
 * 설정값으로 순수 계산 클래스들을 만든다.
 */
@Configuration
@EnableConfigurationProperties(RecommendationProperties.class)
public class TasteModelConfig {

	@Bean
	public BeanVectorCalculator beanVectorCalculator(RecommendationProperties properties) {
		return new BeanVectorCalculator(
				new TasteEstimateTable(properties.roastTaste(), properties.processTasteAdjust()),
				properties.priorK());
	}

	@Bean
	public TasteVectorCalculator tasteVectorCalculator(RecommendationProperties properties) {
		return new TasteVectorCalculator(properties.decayPerWeek(), properties.surveyMinNotes(),
				properties.stableNotes(), properties.sectionWeights());
	}

}
