package com.beanote.recommendation.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.beanote.bean.domain.OriginCountry;
import com.beanote.bean.domain.ProcessMethod;
import com.beanote.global.util.Josa;
import com.beanote.taste.model.VectorLayout;

/**
 * 추천 이유 생성 (5-4). LLM 을 쓰지 않고 템플릿으로 만들어 재현성과 테스트 용이성을 지킨다.
 *
 * <ol>
 * <li>contentSim 을 차원별 기여도로 분해한다 (Similarity.contributions).</li>
 * <li>탐험·협업 점수도 같은 단위(최종 점수에 더해진 양)로 후보에 넣는다.</li>
 * <li>기여도 상위 2개를 서로 다른 유형으로 고르고 템플릿 문장으로 바꾼다.</li>
 * </ol>
 */
public class ReasonGenerator {

	private static final int MAX_REASONS = 2;

	/** 맛 축별 문구: [높을 때, 낮을 때] */
	private static final Map<String, String[]> AXIS_PHRASES = Map.of(
			"ACIDITY", new String[] { "밝은 산미", "부드러운 산미" },
			"SWEETNESS", new String[] { "뚜렷한 단맛", "담백한 맛" },
			"BODY", new String[] { "묵직한 바디감", "가벼운 바디감" },
			"BITTERNESS", new String[] { "쌉쌀한 맛", "적은 쓴맛" },
			"AFTERTASTE", new String[] { "긴 여운", "깔끔한 끝맛" });

	private final Map<String, String> categoryNames;
	private final ScoreWeights weights;

	/**
	 * @param categoryNames 향미 대분류 코드 → 한글 이름 (예: BERRY → 베리)
	 */
	public ReasonGenerator(Map<String, String> categoryNames, ScoreWeights weights) {
		this.categoryNames = categoryNames;
		this.weights = weights;
	}

	public List<Reason> generate(ScoredCandidate scored, UserContext user) {
		List<Scored> options = new ArrayList<>();
		double[] taste = user.taste();
		double[] bean = scored.candidate().vector();
		double[] contributions = scored.contributions();

		for (int i = 0; i < contributions.length; i++) {
			if (contributions[i] <= 0) {
				continue;
			}
			Reason reason = dimensionReason(i, taste[i], bean[i], scored.candidate(), user.surveyBased());
			if (reason != null) {
				options.add(new Scored(reason, contributions[i]));
			}
		}
		if (scored.newProcess()) {
			options.add(new Scored(new Reason(Reason.Type.EXPLORE,
					"아직 안 마셔 본 " + Josa.ieyo(scored.candidate().process().nameKo() + " 가공")),
					weights.explore() * 0.5));
		}
		if (scored.newOrigin()) {
			String origin = OriginCountry.fromCode(scored.candidate().singleOrigin())
					.map(OriginCountry::nameKo).orElse(scored.candidate().singleOrigin());
			options.add(new Scored(new Reason(Reason.Type.EXPLORE, "아직 안 마셔 본 " + origin + " 원두예요"),
					weights.explore() * 0.5));
		}
		if (scored.collab() > 0) {
			options.add(new Scored(new Reason(Reason.Type.COLLAB, "취향이 비슷한 분들이 높게 평가했어요"),
					weights.collab() * scored.collab()));
		}

		options.sort(Comparator.comparingDouble(Scored::value).reversed());
		List<Reason> picked = new ArrayList<>();
		Set<Reason.Type> types = EnumSet.noneOf(Reason.Type.class);
		for (Scored option : options) {
			if (picked.size() >= MAX_REASONS) {
				break;
			}
			if (types.add(option.reason().type())) {
				picked.add(option.reason());
			}
		}
		if (picked.isEmpty()) {
			picked.add(new Reason(Reason.Type.PROFILE, "전반적인 취향과 비슷한 원두예요"));
		}
		return picked;
	}

	private Reason dimensionReason(int index, double tasteValue, double beanValue, Candidate candidate,
			boolean surveyBased) {
		return switch (VectorLayout.sectionOf(index)) {
			case FLAVOR -> {
				if (beanValue <= 0 || tasteValue <= 0) {
					yield null;
				}
				String name = categoryNames.getOrDefault(
						VectorLayout.CATEGORY_CODES.get(index - VectorLayout.FLAVOR_START), "");
				yield new Reason(Reason.Type.FLAVOR, surveyBased
						? "좋아한다고 고르신 " + name + " 향이 있어요"
						: "높게 평가하신 커피들처럼 " + name + " 향이 있어요");
			}
			case TASTE -> {
				String axis = VectorLayout.AXES.get(index - VectorLayout.TASTE_START);
				String[] phrases = AXIS_PHRASES.get(axis);
				yield beanValue > 0
						? new Reason(Reason.Type.TASTE_AXIS, "좋아하시는 " + Josa.iGa(phrases[0]) + " 특징이에요")
						: new Reason(Reason.Type.TASTE_AXIS, Josa.iGa(phrases[1]) + " 취향에 맞아요");
			}
			case PROCESS -> candidate.process() == ProcessMethod.OTHER ? null
					: new Reason(Reason.Type.PROCESS,
							"좋아하시는 " + candidate.process().nameKo() + " 가공 원두예요");
			case ROAST -> new Reason(Reason.Type.ROAST,
					"선호하시는 " + Josa.ieyo(candidate.roastLevel().nameKo() + " 로스팅"));
		};
	}

	private record Scored(Reason reason, double value) {
	}

}
