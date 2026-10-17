package com.beanote.flavor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "flavor_categories")
public class FlavorCategory {

	@Id
	private Long id;

	@Column(nullable = false, unique = true)
	private String code;

	@Column(name = "name_ko", nullable = false)
	private String nameKo;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected FlavorCategory() {
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getNameKo() {
		return nameKo;
	}

	public int getSortOrder() {
		return sortOrder;
	}

}
