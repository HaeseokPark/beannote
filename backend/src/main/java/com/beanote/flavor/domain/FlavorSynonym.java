package com.beanote.flavor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 동의어 사전 항목 (F-1). 소분류 태그 또는 대분류 중 하나를 가리킨다.
 */
@Entity
@Table(name = "flavor_synonyms")
public class FlavorSynonym {

	@Id
	private Long id;

	@Column(name = "flavor_tag_id")
	private Long flavorTagId;

	@Column(name = "category_id")
	private Long categoryId;

	@Column(name = "expression_normalized", nullable = false)
	private String expressionNormalized;

	@Column(nullable = false)
	private String source;

	protected FlavorSynonym() {
	}

	public Long getId() {
		return id;
	}

	public Long getFlavorTagId() {
		return flavorTagId;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public String getExpressionNormalized() {
		return expressionNormalized;
	}

}
