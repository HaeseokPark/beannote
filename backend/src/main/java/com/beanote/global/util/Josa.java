package com.beanote.global.util;

/**
 * 한국어 조사 선택. 마지막 글자에 받침이 있으면 "이/이에요", 없으면 "가/예요".
 * 한글이 아닌 글자로 끝나면 받침이 없는 것으로 본다.
 */
public final class Josa {

	private static final char SYLLABLE_START = 0xAC00;
	private static final char SYLLABLE_END = 0xD7A3;

	private Josa() {
	}

	public static boolean hasFinalConsonant(String word) {
		if (word == null || word.isEmpty()) {
			return false;
		}
		char last = word.charAt(word.length() - 1);
		return last >= SYLLABLE_START && last <= SYLLABLE_END && (last - SYLLABLE_START) % 28 != 0;
	}

	/** "산미가" / "바디감이" */
	public static String iGa(String word) {
		return word + (hasFinalConsonant(word) ? "이" : "가");
	}

	/** "원두예요" / "가공이에요" */
	public static String ieyo(String word) {
		return word + (hasFinalConsonant(word) ? "이에요" : "예요");
	}

}
