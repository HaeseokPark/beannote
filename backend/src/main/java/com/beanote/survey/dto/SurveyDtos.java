package com.beanote.survey.dto;

import java.util.List;

import com.beanote.survey.domain.CoffeeHabit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class SurveyDtos {

	private SurveyDtos() {
	}

	/**
	 * 온보딩 설문 (O-1 ~ O-3). 좋아하는·싫어하는 향은 향미 대분류 코드로 각각 최대 3개.
	 */
	public record SurveyRequest(
			@NotNull CoffeeHabit coffeeHabit,
			@NotNull @Valid Preferences preferences,
			@Size(max = 3) List<@NotBlank String> likedCategories,
			@Size(max = 3) List<@NotBlank String> dislikedCategories) {
	}

	/** 맛 축별 "좋아함" 정도 1~5 */
	public record Preferences(
			@NotNull @Min(1) @Max(5) Integer acidity,
			@NotNull @Min(1) @Max(5) Integer sweetness,
			@NotNull @Min(1) @Max(5) Integer body,
			@NotNull @Min(1) @Max(5) Integer bitterness,
			@NotNull @Min(1) @Max(5) Integer aftertaste) {

		public int[] toArray() {
			return new int[] { acidity, sweetness, body, bitterness, aftertaste };
		}

		public static Preferences of(int[] values) {
			return new Preferences(values[0], values[1], values[2], values[3], values[4]);
		}

	}

	public record CategoryLabel(String code, String name) {
	}

	public record SurveyResponse(CoffeeHabit coffeeHabit, Preferences preferences, List<CategoryLabel> likedCategories,
			List<CategoryLabel> dislikedCategories) {
	}

}
