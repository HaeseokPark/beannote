package com.beanote.flavor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "flavor_tags")
public class FlavorTag {

	@Id
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id")
	private FlavorCategory category;

	@Column(nullable = false, unique = true)
	private String code;

	@Column(name = "name_ko", nullable = false)
	private String nameKo;

	@Column(name = "name_en", nullable = false)
	private String nameEn;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected FlavorTag() {
	}

	public Long getId() {
		return id;
	}

	public FlavorCategory getCategory() {
		return category;
	}

	public String getCode() {
		return code;
	}

	public String getNameKo() {
		return nameKo;
	}

	public String getNameEn() {
		return nameEn;
	}

	public int getSortOrder() {
		return sortOrder;
	}

}
