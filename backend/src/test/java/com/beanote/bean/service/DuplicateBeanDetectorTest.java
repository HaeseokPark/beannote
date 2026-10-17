package com.beanote.bean.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.beanote.global.util.TextNormalizer;

class DuplicateBeanDetectorTest {

	@Test
	void sameNameIgnoringSpacesAndCaseIsSimilar() {
		assertThat(similar("Kenya Nyeri AA", "kenya  nyeri aa")).isTrue();
	}

	@Test
	void oneNameContainingTheOtherIsSimilar() {
		assertThat(similar("예가체프 코체레", "에티오피아 예가체프 코체레 G1")).isTrue();
	}

	@Test
	void smallTypoIsSimilar() {
		assertThat(similar("케냐 니에리 AA", "케냐 니에리 AB")).isTrue();
	}

	@Test
	void differentBeansAreNotSimilar() {
		assertThat(similar("콜롬비아 우일라", "에티오피아 구지")).isFalse();
	}

	@Test
	void similarityIsOneForEqualStrings() {
		assertThat(DuplicateBeanDetector.similarity("abc", "abc")).isEqualTo(1.0);
		assertThat(DuplicateBeanDetector.similarity("abc", "abd")).isCloseTo(0.667, org.assertj.core.data.Offset.offset(0.01));
	}

	private static boolean similar(String a, String b) {
		return DuplicateBeanDetector.isSimilar(TextNormalizer.normalize(a), TextNormalizer.normalize(b));
	}

}
