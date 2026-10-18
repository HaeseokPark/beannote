package com.beanote.recommendation.eval;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;
import com.beanote.taste.model.TasteEstimateTable;
import com.beanote.taste.model.VectorLayout;

/**
 * 오프라인 평가용 가상 세계 (기획서 13-1). 원두·사용자·노트를 고정 시드 난수로 만든다.
 *
 * <p>
 * 평점이 원두 벡터에서만 나오면 콘텐츠 기반 추천이 당연히 이긴다(순환 평가). 그래서 평점에는 추천기가 볼 수 없는 요인을 섞는다.
 * <ul>
 * <li>숨은 요인: 취향 유형마다 선호 원산지가 있다 (원산지는 원두 벡터에 없다)</li>
 * <li>개인차: 사람마다 향미 선호가 유형과 조금씩 다르다</li>
 * <li>평점 습관: 사람마다 기준점(후하게·짜게)과 폭이 다르다</li>
 * <li>잡음: 같은 원두도 그날 컨디션에 따라 평점이 흔들린다</li>
 * </ul>
 * 모든 데이터는 메모리에만 있고 DB 에 넣지 않는다 (운영 DB 오염 방지).
 */
public final class SyntheticWorld {

	public static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
	public static final double DEFAULT_HIDDEN_ORIGIN_WEIGHT = 0.8;

	private static final String[] ORIGINS = { "ET", "KE", "CO", "BR", "GT", "CR", "PA", "RW", "PE", "ID" };
	private static final ProcessMethod[] PROCESSES = { ProcessMethod.WASHED, ProcessMethod.NATURAL,
			ProcessMethod.HONEY, ProcessMethod.ANAEROBIC };

	/** 취향 유형 (13-1). 향미 선호(+)·비선호(−), 선호 로스팅, 선호 가공, 숨은 선호 원산지 */
	enum TasteType {
		BRIGHT_FLORAL("밝은 산미·꽃향파", Map.of("FLORAL", 1.0, "CITRUS", 0.9, "BERRY", 0.5, "ROASTED", -0.8, "SPICE", -0.4),
				0, Set.of(ProcessMethod.WASHED), Set.of("ET", "KE")),
		NUTTY_CHOCOLATE("고소함·초콜릿파", Map.of("NUTTY_COCOA", 1.0, "SWEET", 0.7, "ROASTED", 0.4, "FERMENTED", -0.8, "CITRUS", -0.4),
				3, Set.of(ProcessMethod.NATURAL, ProcessMethod.HONEY), Set.of("BR", "GT")),
		FERMENTED_WINE("발효·와인파", Map.of("FERMENTED", 1.0, "BERRY", 0.8, "STONE_TROPICAL", 0.5, "GREEN", -0.6, "ROASTED", -0.5),
				1, Set.of(ProcessMethod.ANAEROBIC, ProcessMethod.NATURAL), Set.of("CO", "CR")),
		SWEET_FRUIT("달콤 과일파", Map.of("STONE_TROPICAL", 1.0, "SWEET", 0.8, "CITRUS", 0.4, "SPICE", -0.5, "ROASTED", -0.4),
				1, Set.of(ProcessMethod.HONEY, ProcessMethod.NATURAL), Set.of("CR", "PA")),
		BALANCED_CLASSIC("클래식 밸런스파", Map.of("SWEET", 0.8, "NUTTY_COCOA", 0.7, "FLORAL", 0.3, "FERMENTED", -0.6),
				2, Set.of(ProcessMethod.WASHED, ProcessMethod.HONEY), Set.of("CO", "PE")),
		SMOKY_DARK("스모키 다크파", Map.of("ROASTED", 1.0, "SPICE", 0.7, "NUTTY_COCOA", 0.6, "FLORAL", -0.7, "CITRUS", -0.6),
				4, Set.of(ProcessMethod.NATURAL), Set.of("ID", "BR"));

		final String label;
		final Map<String, Double> flavor;
		final int roast;
		final Set<ProcessMethod> processes;
		final Set<String> origins;

		TasteType(String label, Map<String, Double> flavor, int roast, Set<ProcessMethod> processes,
				Set<String> origins) {
			this.label = label;
			this.flavor = flavor;
			this.roast = roast;
			this.processes = processes;
			this.origins = origins;
		}
	}

	public record Bean(long id, String origin, ProcessMethod process, RoastLevel roast,
			Map<String, Integer> officialCategories, double[] trueAxes) {
	}

	public record User(long id, TasteType type, Map<String, Double> flavorPrefs, double bias, double scale) {
	}

	public record Note(long userId, long beanId, double rating, LocalDate tastedOn, double[] axes,
			Map<String, Integer> categories) {
	}

	final List<Bean> beans = new ArrayList<>();
	final List<User> users = new ArrayList<>();
	final List<Note> notes = new ArrayList<>();
	final Map<Long, Bean> beanById = new HashMap<>();

	private final Random random;
	/** 숨은 원산지 선호의 세기. 0 이면 평점이 원두 벡터로 설명되는 요인만으로 정해진다 (비교 실험용) */
	private final double hiddenOriginWeight;

	private SyntheticWorld(long seed, double hiddenOriginWeight) {
		this.random = new Random(seed);
		this.hiddenOriginWeight = hiddenOriginWeight;
	}

	/**
	 * @param beanCount     원두 수
	 * @param usersPerType  유형별 사용자 수 범위 [min, max]
	 * @param notesPerUser  사용자별 노트 수 범위 [min, max]
	 */
	public static SyntheticWorld generate(long seed, TasteEstimateTable estimates, int beanCount, int[] usersPerType,
			int[] notesPerUser) {
		return generate(seed, estimates, beanCount, usersPerType, notesPerUser, DEFAULT_HIDDEN_ORIGIN_WEIGHT);
	}

	public static SyntheticWorld generate(long seed, TasteEstimateTable estimates, int beanCount, int[] usersPerType,
			int[] notesPerUser, double hiddenOriginWeight) {
		SyntheticWorld world = new SyntheticWorld(seed, hiddenOriginWeight);
		world.makeBeans(beanCount, estimates);
		world.makeUsers(usersPerType);
		world.makeNotes(notesPerUser);
		return world;
	}

	// ---- 원두 ----

	private void makeBeans(int count, TasteEstimateTable estimates) {
		for (int id = 1; id <= count; id++) {
			ProcessMethod process = PROCESSES[random.nextInt(PROCESSES.length)];
			RoastLevel roast = RoastLevel.values()[random.nextInt(5)];
			Map<String, Integer> categories = new LinkedHashMap<>();
			for (int t = 0; t < 3; t++) {
				categories.merge(pickCategory(process, roast), 1, Integer::sum);
			}
			double[] axes = estimates.estimate(process, roast);
			for (int i = 0; i < axes.length; i++) {
				axes[i] = Math.clamp(axes[i] + gaussian(0.5), 1, 5);
			}
			Bean bean = new Bean(id, ORIGINS[random.nextInt(ORIGINS.length)], process, roast, categories, axes);
			beans.add(bean);
			beanById.put(bean.id(), bean);
		}
	}

	/** 가공·로스팅에 어울리는 향미가 더 자주 나오게 한다 (실제 원두 설명의 경향) */
	private String pickCategory(ProcessMethod process, RoastLevel roast) {
		Map<String, Double> weights = new LinkedHashMap<>();
		VectorLayout.CATEGORY_CODES.forEach(code -> weights.put(code, 1.0));
		int r = roast.ordinal();
		if (r <= 1) {
			boost(weights, 2.5, "FLORAL", "CITRUS", "BERRY");
		}
		if (r >= 3) {
			boost(weights, 2.5, "NUTTY_COCOA", "ROASTED", "SPICE");
		}
		if (r == 2) {
			boost(weights, 1.5, "SWEET", "NUTTY_COCOA");
		}
		switch (process) {
			case NATURAL -> boost(weights, 2.0, "BERRY", "STONE_TROPICAL", "FERMENTED");
			case ANAEROBIC -> boost(weights, 3.0, "FERMENTED", "STONE_TROPICAL");
			case HONEY -> boost(weights, 2.0, "SWEET", "STONE_TROPICAL");
			case WASHED -> boost(weights, 1.5, "CITRUS", "FLORAL", "GREEN");
			default -> {
			}
		}
		double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
		double x = random.nextDouble() * total;
		for (Map.Entry<String, Double> e : weights.entrySet()) {
			x -= e.getValue();
			if (x <= 0) {
				return e.getKey();
			}
		}
		return "SWEET";
	}

	private static void boost(Map<String, Double> weights, double factor, String... codes) {
		for (String code : codes) {
			weights.computeIfPresent(code, (k, v) -> v * factor);
		}
	}

	// ---- 사용자 ----

	private void makeUsers(int[] perType) {
		long id = 1;
		for (TasteType type : TasteType.values()) {
			int n = between(perType[0], perType[1]);
			for (int i = 0; i < n; i++) {
				// 개인차: 유형 선호에 ±0.35 정도의 개인 취향을 더한다
				Map<String, Double> prefs = new HashMap<>();
				for (String code : VectorLayout.CATEGORY_CODES) {
					prefs.put(code, type.flavor.getOrDefault(code, 0.0) + gaussian(0.35));
				}
				double bias = (random.nextDouble() - 0.5) * 1.2;   // 기준점: −0.6 ~ +0.6
				double scale = 0.6 + random.nextDouble() * 0.7;    // 폭: 0.6 ~ 1.3
				users.add(new User(id++, type, prefs, bias, scale));
			}
		}
	}

	// ---- 노트 ----

	/** 사용자가 원두를 얼마나 좋아하는지 (평점의 바탕). 숨은 원산지 요인 포함 */
	double utility(User user, Bean bean) {
		double flavor = 0;
		int total = bean.officialCategories().values().stream().mapToInt(Integer::intValue).sum();
		for (Map.Entry<String, Integer> e : bean.officialCategories().entrySet()) {
			flavor += user.flavorPrefs().get(e.getKey()) * e.getValue() / total;
		}
		double roast = 1 - Math.abs(bean.roast().ordinal() - user.type().roast) / 2.0;
		double process = user.type().processes.contains(bean.process()) ? 0.5 : 0;
		double origin = user.type().origins.contains(bean.origin()) ? hiddenOriginWeight : 0;
		return 1.2 * flavor + 0.8 * roast + process + origin;
	}

	private void makeNotes(int[] perUser) {
		for (User user : users) {
			int n = between(perUser[0], perUser[1]);
			List<Bean> chosen = chooseBeans(user, n);
			List<Note> mine = new ArrayList<>();
			for (Bean bean : chosen) {
				double u = utility(user, bean) + gaussian(0.35);
				// 유틸리티 −0.5 ~ 2.5 를 대략 별점 1 ~ 5 로 옮기고 개인 습관을 반영한다
				double raw = 3.0 + user.bias() + user.scale() * (u - 1.0) * 1.2;
				double rating = Math.clamp(Math.round(raw * 2) / 2.0, 0.5, 5.0);
				double[] axes = new double[5];
				for (int i = 0; i < 5; i++) {
					axes[i] = Math.clamp(Math.round(bean.trueAxes()[i] + gaussian(0.7)), 1, 5);
				}
				mine.add(new Note(user.id(), bean.id(), rating, TODAY.minusDays(random.nextInt(365)), axes,
						perceivedCategories(bean)));
			}
			mine.sort(Comparator.comparing(Note::tastedOn));
			notes.addAll(mine);
		}
	}

	/** 60%는 좋아할 것 같은 원두를, 40%는 아무거나 고른다 (사람은 끌리는 원두를 더 자주 산다) */
	private List<Bean> chooseBeans(User user, int n) {
		Set<Long> picked = new HashSet<>();
		List<Bean> result = new ArrayList<>();
		while (result.size() < n) {
			Bean bean;
			if (random.nextDouble() < 0.6) {
				// 후보 8개 중 기대 유틸리티가 가장 높은 것
				bean = null;
				double best = Double.NEGATIVE_INFINITY;
				for (int i = 0; i < 8; i++) {
					Bean b = beans.get(random.nextInt(beans.size()));
					double u = utility(user, b) + gaussian(0.5);
					if (u > best) {
						best = u;
						bean = b;
					}
				}
			}
			else {
				bean = beans.get(random.nextInt(beans.size()));
			}
			if (picked.add(bean.id())) {
				result.add(bean);
			}
		}
		return result;
	}

	/** 노트에 적는 향: 원두 향 중 1~2개를 알아채고, 가끔 다른 향을 적는다 */
	private Map<String, Integer> perceivedCategories(Bean bean) {
		Map<String, Integer> result = new HashMap<>();
		List<String> official = new ArrayList<>(bean.officialCategories().keySet());
		int k = 1 + random.nextInt(2);
		for (int i = 0; i < k; i++) {
			result.merge(official.get(random.nextInt(official.size())), 1, Integer::sum);
		}
		if (random.nextDouble() < 0.15) {
			result.merge(VectorLayout.CATEGORY_CODES.get(random.nextInt(10)), 1, Integer::sum);
		}
		return result;
	}

	private int between(int min, int max) {
		return min + random.nextInt(max - min + 1);
	}

	private double gaussian(double sd) {
		return random.nextGaussian() * sd;
	}

}
