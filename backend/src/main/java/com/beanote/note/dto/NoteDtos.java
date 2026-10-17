package com.beanote.note.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.beanote.bean.domain.Bean;
import com.beanote.flavor.dto.FlavorDtos.FlavorLabel;
import com.beanote.flavor.service.FlavorService;
import com.beanote.note.domain.BrewMethod;
import com.beanote.note.domain.BrewRecipe;
import com.beanote.note.domain.TasteScores;
import com.beanote.note.domain.TastingNote;
import com.beanote.note.domain.Visibility;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class NoteDtos {

	private NoteDtos() {
	}

	/**
	 * 노트 작성·수정 요청 (4-4 노트 입력 항목). 별점 범위·단위와 향미 개수는 전용 에러 코드를 쓰기 위해 서비스에서 검증한다.
	 */
	public record NoteRequest(
			@NotNull Long beanId,
			@NotNull LocalDate tastedOn,
			@NotNull BrewMethod brewMethod,
			@Valid RecipeRequest recipe,
			@NotNull @Valid ScoresRequest scores,
			List<Long> flavorTagIds,
			List<@NotBlank @Size(max = 50) String> freeFlavors,
			@NotNull BigDecimal rating,
			Boolean wouldBuyAgain,
			@Size(max = 1000) String memo,
			@NotNull Visibility visibility) {
	}

	public record RecipeRequest(
			@Positive @DecimalMax("200.0") BigDecimal doseG,
			@Positive @DecimalMax("2000.0") BigDecimal waterG,
			@Min(0) @Max(100) Integer waterTempC,
			@Size(max = 50) String grindText,
			@Positive @Max(86400) Integer brewTimeSec) {

		public BrewRecipe toRecipe() {
			return new BrewRecipe(doseG, waterG, waterTempC, grindText == null ? null : grindText.strip(),
					brewTimeSec);
		}

	}

	public record ScoresRequest(
			@NotNull @Min(1) @Max(5) Integer acidity,
			@NotNull @Min(1) @Max(5) Integer sweetness,
			@NotNull @Min(1) @Max(5) Integer body,
			@NotNull @Min(1) @Max(5) Integer bitterness,
			@NotNull @Min(1) @Max(5) Integer aftertaste) {

		public TasteScores toScores() {
			return new TasteScores(acidity, sweetness, body, bitterness, aftertaste);
		}

	}

	public record BeanRef(Long id, String name, String roasteryName) {

		static BeanRef from(Bean bean) {
			return new BeanRef(bean.getId(), bean.getName(), bean.getRoastery().getName());
		}

	}

	public record Author(Long id, String nickname) {
	}

	public record NoteSummaryResponse(Long id, BeanRef bean, LocalDate tastedOn, BrewMethod brewMethod,
			TasteScores scores, BigDecimal rating, List<FlavorLabel> flavors, Visibility visibility,
			Instant createdAt) {

		public static NoteSummaryResponse from(TastingNote note) {
			return new NoteSummaryResponse(note.getId(), BeanRef.from(note.getBean()), note.getTastedOn(),
					note.getBrewMethod(), note.getScores(), note.getRating(), flavorsOf(note), note.getVisibility(),
					note.getCreatedAt());
		}

	}

	public record NoteDetailResponse(Long id, Author author, boolean mine, BeanRef bean, LocalDate tastedOn,
			BrewMethod brewMethod, BrewRecipe recipe, TasteScores scores, BigDecimal rating,
			List<FlavorLabel> flavors, List<String> freeFlavors, Boolean wouldBuyAgain, String memo,
			Visibility visibility, String photoUrl, Instant createdAt, Instant updatedAt) {

		public static NoteDetailResponse from(TastingNote note, Long viewerId) {
			return new NoteDetailResponse(note.getId(),
					new Author(note.getUser().getId(), note.getUser().getNickname()),
					viewerId != null && note.isOwnedBy(viewerId), BeanRef.from(note.getBean()), note.getTastedOn(),
					note.getBrewMethod(), note.getRecipe(), note.getScores(), note.getRating(), flavorsOf(note),
					List.copyOf(note.getFreeFlavors()), note.getWouldBuyAgain(), note.getMemo(),
					note.getVisibility(), note.getPhotoUrl(), note.getCreatedAt(), note.getUpdatedAt());
		}

	}

	/**
	 * 내 노트 목록 필터 (N-2).
	 */
	public record MyNoteCondition(Long beanId, @DecimalMin("0.5") @DecimalMax("5.0") BigDecimal minRating,
			String flavorCategory, String cursor, int size) {

		public static final int DEFAULT_SIZE = 20;
		public static final int MAX_SIZE = 50;

		public MyNoteCondition {
			size = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
		}

	}

	private static List<FlavorLabel> flavorsOf(TastingNote note) {
		return FlavorService.sorted(note.getFlavors()).stream().map(FlavorLabel::from).toList();
	}

}
