package com.beanote.global.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 비교용 문자열 정규화: NFKC → 소문자 → 공백 제거.
 * 향미 동의어(expression_normalized)와 원두 이름(name_normalized)에 같은 규칙을 쓴다.
 */
public final class TextNormalizer {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private TextNormalizer() {
	}

	public static String normalize(String text) {
		if (text == null) {
			return "";
		}
		String nfkc = Normalizer.normalize(text, Normalizer.Form.NFKC);
		return WHITESPACE.matcher(nfkc.toLowerCase(Locale.ROOT)).replaceAll("");
	}

}
