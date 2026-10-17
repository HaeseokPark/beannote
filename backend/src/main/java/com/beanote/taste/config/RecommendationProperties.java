package com.beanote.taste.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;
import com.beanote.recommendation.model.ScoreWeights;
import com.beanote.survey.domain.CoffeeHabit;
import com.beanote.taste.model.SectionWeights;

/**
 * beanote.recommendation.* — 취향·추천 계산의 모든 숫자 (기획서 5장: 코드에 숫자를 직접 쓰지 않는다).
 *
 * @param priorK             원두 벡터에서 노트 평균을 믿기 시작하는 정도 K
 * @param decayPerWeek       노트 영향 주간 감쇠율
 * @param surveyMinNotes     이 수 미만이면 설문 벡터만 쓴다
 * @param stableNotes        이 수 이상이면 노트 벡터만 쓴다
 * @param collabMinUsers     사용자가 이 수 미만이면 협업 점수를 쓰지 않는다
 * @param roastTaste         로스팅별 기본 맛 축 추정 (산미, 단맛, 바디, 쓴맛, 여운)
 * @param processTasteAdjust 가공 방식별 맛 축 보정값
 * @param habitRoast         평소 마시는 커피 → 추정 선호 로스팅
 */
@ConfigurationProperties(prefix = "beanote.recommendation")
public record RecommendationProperties(
		double priorK,
		double decayPerWeek,
		int surveyMinNotes,
		int stableNotes,
		SectionWeights sectionWeights,
		ScoreWeights scoreWeights,
		int collabMinUsers,
		Duration cacheTtl,
		int size,
		int similarSize,
		Map<RoastLevel, List<Double>> roastTaste,
		Map<ProcessMethod, List<Double>> processTasteAdjust,
		Map<CoffeeHabit, RoastLevel> habitRoast) {
}
