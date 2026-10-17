package com.beanote.note.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 추출 레시피 (모두 선택).
 */
@Embeddable
public record BrewRecipe(
		@Column(name = "dose_g") BigDecimal doseG,
		@Column(name = "water_g") BigDecimal waterG,
		@Column(name = "water_temp_c") Integer waterTempC,
		@Column(name = "grind_text") String grindText,
		@Column(name = "brew_time_sec") Integer brewTimeSec) {
}
