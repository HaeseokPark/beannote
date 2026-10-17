package com.beanote.flavor.matching;

/**
 * 한글 음절을 자모로 분해한다. "자스민" → "ㅈㅏㅅㅡㅁㅣㄴ"
 * 음절 단위가 아니라 자모 단위로 편집 거리를 재야 "자스밍"과 "자스민"처럼 받침 하나 다른 오타가 가깝게 나온다.
 */
public final class HangulJamo {

	private static final char SYLLABLE_START = 0xAC00;
	private static final char SYLLABLE_END = 0xD7A3;
	private static final int JUNG_COUNT = 21;
	private static final int JONG_COUNT = 28;

	private static final char[] CHO = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ".toCharArray();
	private static final char[] JUNG = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ".toCharArray();
	// 0번은 받침 없음
	private static final char[] JONG = "\0ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ".toCharArray();

	private HangulJamo() {
	}

	public static String decompose(String text) {
		StringBuilder sb = new StringBuilder(text.length() * 3);
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c < SYLLABLE_START || c > SYLLABLE_END) {
				sb.append(c);
				continue;
			}
			int index = c - SYLLABLE_START;
			sb.append(CHO[index / (JUNG_COUNT * JONG_COUNT)]);
			sb.append(JUNG[(index % (JUNG_COUNT * JONG_COUNT)) / JONG_COUNT]);
			int jong = index % JONG_COUNT;
			if (jong != 0) {
				sb.append(JONG[jong]);
			}
		}
		return sb.toString();
	}

}
