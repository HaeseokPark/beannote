package com.beanote.bean.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.beanote.global.util.TextNormalizer;

/**
 * 커피 원산지 국가 (ISO 3166-1 alpha-2). DB 에는 코드만 저장한다.
 */
public enum OriginCountry {

	ET("에티오피아"), KE("케냐"), RW("르완다"), BI("부룬디"), TZ("탄자니아"), UG("우간다"), YE("예멘"),
	CO("콜롬비아"), BR("브라질"), PE("페루"), BO("볼리비아"), EC("에콰도르"),
	GT("과테말라"), CR("코스타리카"), PA("파나마"), HN("온두라스"), SV("엘살바도르"), NI("니카라과"), MX("멕시코"),
	JM("자메이카"),
	ID("인도네시아"), IN("인도"), VN("베트남"), PG("파푸아뉴기니"), CN("중국"), TH("태국"), US("미국");

	private final String nameKo;

	OriginCountry(String nameKo) {
		this.nameKo = nameKo;
	}

	public String nameKo() {
		return nameKo;
	}

	public static Optional<OriginCountry> fromCode(String code) {
		return Arrays.stream(values()).filter(country -> country.name().equalsIgnoreCase(code)).findFirst();
	}

	/**
	 * 검색어가 국가 이름의 일부와 일치하는 국가들. 예: "에티오" → [ET]
	 */
	public static List<OriginCountry> matchingName(String normalizedQuery) {
		if (normalizedQuery.isEmpty()) {
			return List.of();
		}
		return Arrays.stream(values())
				.filter(country -> TextNormalizer.normalize(country.nameKo).contains(normalizedQuery))
				.toList();
	}

}
