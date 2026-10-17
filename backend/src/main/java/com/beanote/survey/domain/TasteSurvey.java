package com.beanote.survey.domain;

import java.util.LinkedHashMap;
import java.util.Map;

import com.beanote.global.entity.BaseTimeEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

/**
 * 온보딩 취향 설문 (4-2). 사용자당 하나.
 */
@Entity
@Table(name = "taste_surveys")
public class TasteSurvey extends BaseTimeEntity {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "coffee_habit", nullable = false)
	private CoffeeHabit coffeeHabit;

	@Column(name = "pref_acidity", nullable = false)
	private int prefAcidity;

	@Column(name = "pref_sweetness", nullable = false)
	private int prefSweetness;

	@Column(name = "pref_body", nullable = false)
	private int prefBody;

	@Column(name = "pref_bitterness", nullable = false)
	private int prefBitterness;

	@Column(name = "pref_aftertaste", nullable = false)
	private int prefAftertaste;

	/** 향미 대분류 id → 좋아함/싫어함 */
	@ElementCollection
	@CollectionTable(name = "taste_survey_flavors", joinColumns = @JoinColumn(name = "user_id"))
	@MapKeyColumn(name = "category_id")
	@Enumerated(EnumType.STRING)
	@Column(name = "preference", nullable = false)
	private Map<Long, FlavorPreference> flavors = new LinkedHashMap<>();

	protected TasteSurvey() {
	}

	public TasteSurvey(Long userId) {
		this.userId = userId;
	}

	public void update(CoffeeHabit coffeeHabit, int[] preferences, Map<Long, FlavorPreference> flavors) {
		this.coffeeHabit = coffeeHabit;
		this.prefAcidity = preferences[0];
		this.prefSweetness = preferences[1];
		this.prefBody = preferences[2];
		this.prefBitterness = preferences[3];
		this.prefAftertaste = preferences[4];
		this.flavors.clear();
		this.flavors.putAll(flavors);
	}

	/** 산미, 단맛, 바디, 쓴맛, 여운 */
	public int[] preferences() {
		return new int[] { prefAcidity, prefSweetness, prefBody, prefBitterness, prefAftertaste };
	}

	public Long getUserId() {
		return userId;
	}

	public CoffeeHabit getCoffeeHabit() {
		return coffeeHabit;
	}

	public Map<Long, FlavorPreference> getFlavors() {
		return flavors;
	}

}
