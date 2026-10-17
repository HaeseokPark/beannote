package com.beanote.bean.domain;

/**
 * 가공 방식 (3-3). 블렌드는 OTHER 로 둔다.
 */
public enum ProcessMethod {

	WASHED("워시드"), NATURAL("내추럴"), HONEY("허니"), ANAEROBIC("무산소"), OTHER("기타");

	private final String nameKo;

	ProcessMethod(String nameKo) {
		this.nameKo = nameKo;
	}

	public String nameKo() {
		return nameKo;
	}

}
