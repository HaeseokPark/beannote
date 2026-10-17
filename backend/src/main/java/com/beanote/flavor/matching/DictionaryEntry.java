package com.beanote.flavor.matching;

/**
 * 동의어 사전의 한 항목. expression 은 TextNormalizer 로 정규화된 값이고, tagId 와 categoryId 중 하나만 있다.
 */
public record DictionaryEntry(String expression, Long tagId, Long categoryId) {

	public DictionaryEntry {
		if ((tagId == null) == (categoryId == null)) {
			throw new IllegalArgumentException("tagId 와 categoryId 중 정확히 하나가 있어야 합니다: " + expression);
		}
	}

	public static DictionaryEntry tag(String expression, long tagId) {
		return new DictionaryEntry(expression, tagId, null);
	}

	public static DictionaryEntry category(String expression, long categoryId) {
		return new DictionaryEntry(expression, null, categoryId);
	}

}
