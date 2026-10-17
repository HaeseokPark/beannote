package com.beanote.bean.domain;

/**
 * 로스팅 포인트 (3-3). 선언 순서가 밝은 → 어두운 순서이며 원두 벡터의 순서형 값으로 쓴다.
 */
public enum RoastLevel {

	LIGHT("라이트"), MEDIUM_LIGHT("미디엄 라이트"), MEDIUM("미디엄"), MEDIUM_DARK("미디엄 다크"), DARK("다크");

	private final String nameKo;

	RoastLevel(String nameKo) {
		this.nameKo = nameKo;
	}

	public String nameKo() {
		return nameKo;
	}

}
