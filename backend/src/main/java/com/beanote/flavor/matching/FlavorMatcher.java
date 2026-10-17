package com.beanote.flavor.matching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.beanote.flavor.domain.MappingResult;
import com.beanote.global.util.TextNormalizer;

/**
 * 향미 표현을 표준 태그로 바꾸는 판정기 (F-2, F-3). JPA 에 의존하지 않는 순수 클래스다.
 *
 * <ol>
 * <li>입력을 쉼표·슬래시 등으로 나눠 표현 조각마다 판정한다.</li>
 * <li>정규화(NFKC·소문자·공백 제거) 값이 사전과 같으면 EXACT.</li>
 * <li>단어 조합(n-gram)에서 "향/맛/느낌" 같은 꼬리말을 떼고 사전과 같으면 AUTO (긴 조합 우선).
 * 예: "살짝 자두 같은 느낌" → "자두"</li>
 * <li>그 밖에는 자모 분해 후 편집 거리 유사도(0~100)로 판정한다. 사전 표현이 단어 안에 들어 있으면
 * 검토가 필요한 점수(REVIEW 구간)를 준다. 예: "리치한" 은 과일 리치가 아닐 수 있다.</li>
 * </ol>
 */
public class FlavorMatcher {

	/** 하나의 입력을 여러 표현으로 나누는 구분자 */
	private static final Pattern SEGMENT_DELIMITER = Pattern.compile("[,，/;·・&+\\n]|\\s+and\\s+|\\s+그리고\\s+");
	private static final Pattern WORD_DELIMITER = Pattern.compile("\\s+");
	/** 비교 전에 지우는 문장부호 (글자·숫자 외) */
	private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}]");
	/** 표현 끝에 붙는 꼬리말. 긴 것부터 뗀다 */
	private static final List<String> SUFFIXES = List.of("같은느낌", "느낌", "같은", "같이", "처럼", "스러운", "뉘앙스",
			"계열", "노트", "향기", "향", "맛", "의");

	private static final double TOKEN_EXACT_SCORE = 95.0;
	private static final double CONTAINS_SCORE = 80.0;
	private static final int MIN_COMPARE_LENGTH = 2;
	private static final int MAX_SEGMENTS = 20;

	private final Map<String, DictionaryEntry> exact = new HashMap<>();
	private final List<IndexedEntry> indexed = new ArrayList<>();
	private final double autoThreshold;
	private final double reviewThreshold;

	public FlavorMatcher(Collection<DictionaryEntry> entries, double autoThreshold, double reviewThreshold) {
		if (!(0 <= reviewThreshold && reviewThreshold <= autoThreshold && autoThreshold <= 100)) {
			throw new IllegalArgumentException("0 <= review <= auto <= 100 이어야 합니다.");
		}
		this.autoThreshold = autoThreshold;
		this.reviewThreshold = reviewThreshold;
		for (DictionaryEntry entry : entries) {
			exact.put(entry.expression(), entry);
			if (entry.expression().length() >= MIN_COMPARE_LENGTH) {
				indexed.add(new IndexedEntry(entry, HangulJamo.decompose(entry.expression())));
			}
		}
	}

	/**
	 * 입력을 표현 조각으로 나눈다. 예: "자스민, 복숭아 / 꿀" → [자스민, 복숭아, 꿀]
	 */
	public static List<String> splitSegments(String text) {
		if (text == null) {
			return List.of();
		}
		return Arrays.stream(SEGMENT_DELIMITER.split(text))
				.map(String::strip)
				.filter(segment -> !clean(segment).isEmpty())
				.limit(MAX_SEGMENTS)
				.toList();
	}

	public List<FlavorMatch> matchAll(String text) {
		return splitSegments(text).stream().map(this::match).toList();
	}

	/**
	 * 표현 조각 하나를 판정한다.
	 */
	public FlavorMatch match(String segment) {
		String normalized = clean(segment);
		if (normalized.isEmpty()) {
			return FlavorMatch.unmapped(segment, normalized, 0);
		}

		DictionaryEntry direct = exact.get(normalized);
		if (direct != null) {
			return new FlavorMatch(segment, normalized, MappingResult.EXACT, 100.0, direct);
		}

		List<String> candidates = candidates(segment);
		for (String candidate : candidates) {
			DictionaryEntry hit = exact.get(candidate);
			if (hit != null) {
				return new FlavorMatch(segment, normalized, judge(TOKEN_EXACT_SCORE), TOKEN_EXACT_SCORE, hit);
			}
		}

		Scored best = new Scored(null, 0);
		for (String candidate : candidates) {
			if (candidate.length() < MIN_COMPARE_LENGTH) {
				continue;
			}
			String candidateJamo = HangulJamo.decompose(candidate);
			for (IndexedEntry entry : indexed) {
				double score = Math.max(similarity(candidateJamo, entry.jamo()),
						candidate.contains(entry.entry().expression()) ? CONTAINS_SCORE : 0);
				if (score > best.score()
						|| (score == best.score() && best.entry() != null
								&& entry.entry().expression().length() > best.entry().expression().length())) {
					best = new Scored(entry.entry(), score);
				}
			}
		}

		MappingResult result = judge(best.score());
		if (result == MappingResult.UNMAPPED) {
			return FlavorMatch.unmapped(segment, normalized, round(best.score()));
		}
		return new FlavorMatch(segment, normalized, result, round(best.score()), best.entry());
	}

	private MappingResult judge(double score) {
		if (score >= autoThreshold) {
			return MappingResult.AUTO;
		}
		if (score >= reviewThreshold) {
			return MappingResult.REVIEW;
		}
		return MappingResult.UNMAPPED;
	}

	/**
	 * 비교 후보: 긴 단어 조합부터 짧은 조합 순서로, 각각 원형과 꼬리말을 뗀 형태.
	 */
	static List<String> candidates(String segment) {
		List<String> words = Arrays.stream(WORD_DELIMITER.split(segment.strip()))
				.map(FlavorMatcher::clean)
				.filter(word -> !word.isEmpty())
				.toList();
		Set<String> result = new LinkedHashSet<>();
		for (int n = words.size(); n >= 1; n--) {
			for (int i = 0; i + n <= words.size(); i++) {
				String joined = String.join("", words.subList(i, i + n));
				result.add(joined);
				String stripped = stripSuffixes(joined);
				if (!stripped.isEmpty()) {
					result.add(stripped);
				}
			}
		}
		return List.copyOf(result);
	}

	static String stripSuffixes(String word) {
		String current = word;
		boolean changed = true;
		while (changed) {
			changed = false;
			for (String suffix : SUFFIXES) {
				if (current.length() > suffix.length() && current.endsWith(suffix)) {
					current = current.substring(0, current.length() - suffix.length());
					changed = true;
					break;
				}
			}
		}
		return current;
	}

	static String clean(String text) {
		return NON_WORD.matcher(TextNormalizer.normalize(text)).replaceAll("");
	}

	/** 1 - (편집 거리 / 긴 쪽 길이), 0~100 */
	static double similarity(String a, String b) {
		int longer = Math.max(a.length(), b.length());
		if (longer == 0) {
			return 100.0;
		}
		// 길이 차이만으로 기준에 못 미치면 계산하지 않는다
		if (Math.abs(a.length() - b.length()) > longer / 2) {
			return 0.0;
		}
		return (1.0 - (double) levenshtein(a, b) / longer) * 100.0;
	}

	private static int levenshtein(String a, String b) {
		int[] previous = new int[b.length() + 1];
		int[] current = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) {
			previous[j] = j;
		}
		for (int i = 1; i <= a.length(); i++) {
			current[0] = i;
			for (int j = 1; j <= b.length(); j++) {
				int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
			}
			int[] swap = previous;
			previous = current;
			current = swap;
		}
		return previous[b.length()];
	}

	private static double round(double score) {
		return Math.round(score * 100) / 100.0;
	}

	private record IndexedEntry(DictionaryEntry entry, String jamo) {
	}

	private record Scored(DictionaryEntry entry, double score) {
	}

}
