package com.beanote.recommendation.model;

import java.util.Comparator;
import java.util.List;

import com.beanote.bean.domain.ProcessMethod;
import com.beanote.taste.model.SectionWeights;
import com.beanote.taste.model.Similarity;
import com.beanote.taste.model.VectorLayout;

/**
 * 원두 추천 점수 (5-3). JPA 에 의존하지 않는 순수 클래스.
 *
 * <pre>
 * score = α × contentSim + β × collabScore + γ × explore − 싫어하는 향 비율 × penalty
 *
 * contentSim  = (가중 코사인 + 1) / 2          → 0 ~ 1
 * collabScore = 이웃이 준 보정 평점을 0~1 로 옮긴 값 (Phase 7, 그 전에는 0)
 * explore     = 0.5 × (새 원산지) + 0.5 × (새 가공 방식)  → 0 ~ 1. 노트가 없으면 0
 * 싫어하는 향 비율 = 원두 향미 구간에서 싫어하는 대분류가 차지하는 비율 (0 ~ 1)
 * </pre>
 *
 * 싫어하는 향은 감점만 하고 후보에서 빼지는 않는다.
 */
public class RecommendationScorer {

	private final ScoreWeights weights;
	private final SectionWeights sectionWeights;

	public RecommendationScorer(ScoreWeights weights, SectionWeights sectionWeights) {
		this.weights = weights;
		this.sectionWeights = sectionWeights;
	}

	public List<ScoredCandidate> rank(List<Candidate> candidates, UserContext user, int limit) {
		return candidates.stream()
				.map(candidate -> score(candidate, user))
				.sorted(Comparator.comparingDouble(ScoredCandidate::score).reversed()
						.thenComparing(s -> -s.candidate().beanId()))
				.limit(limit)
				.toList();
	}

	public ScoredCandidate score(Candidate candidate, UserContext user) {
		double cosine = Similarity.cosine(user.taste(), candidate.vector(), sectionWeights);
		double contentSim = Similarity.toUnit(cosine);

		double[] contributions = Similarity.contributions(user.taste(), candidate.vector(), sectionWeights);
		for (int i = 0; i < contributions.length; i++) {
			contributions[i] *= weights.content() / 2;
		}

		boolean hasHistory = !user.triedOrigins().isEmpty() || !user.triedProcesses().isEmpty();
		boolean newOrigin = hasHistory && candidate.singleOrigin() != null
				&& !user.triedOrigins().contains(candidate.singleOrigin());
		boolean newProcess = hasHistory && candidate.process() != ProcessMethod.OTHER
				&& !user.triedProcesses().contains(candidate.process());
		double explore = (newOrigin ? 0.5 : 0) + (newProcess ? 0.5 : 0);

		double collab = user.collabScores().getOrDefault(candidate.beanId(), 0.0);
		double penalty = dislikedRatio(candidate.vector(), user) * weights.dislikePenalty();

		double score = weights.content() * contentSim + weights.collab() * collab + weights.explore() * explore
				- penalty;
		return new ScoredCandidate(candidate, score, contentSim, collab, explore, penalty, contributions, newOrigin,
				newProcess);
	}

	private static double dislikedRatio(double[] vector, UserContext user) {
		double total = 0;
		for (int i = VectorLayout.FLAVOR_START; i < VectorLayout.PROCESS_START; i++) {
			total += vector[i];
		}
		if (total <= 0) {
			return 0;
		}
		double disliked = 0;
		for (String code : user.dislikedCategories()) {
			disliked += vector[VectorLayout.categoryIndex(code)];
		}
		return disliked / total;
	}

}
