package com.beanote.bean.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.beanote.bean.domain.Bean;
import com.beanote.bean.domain.BeanStats;
import com.beanote.bean.domain.BeanStatus;
import com.beanote.bean.domain.OriginCountry;
import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;
import com.beanote.flavor.dto.FlavorDtos.FlavorLabel;
import com.beanote.flavor.service.FlavorService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class BeanDtos {

	private BeanDtos() {
	}

	public record RoasterySummary(Long id, String name) {
	}

	public record Origin(String code, String name) {

		static Origin of(String code) {
			String name = OriginCountry.fromCode(code).map(OriginCountry::nameKo).orElse(code);
			return new Origin(code, name);
		}

	}

	/**
	 * 사용자 노트 평균 (B-2). 노트가 없으면 평균 값은 null.
	 */
	public record StatsResponse(int noteCount, BigDecimal avgRating, AxisAverages axes) {

		static StatsResponse from(BeanStats stats) {
			if (stats == null || stats.getNoteCount() == 0) {
				return new StatsResponse(0, null, null);
			}
			return new StatsResponse(stats.getNoteCount(), stats.getAvgRating(),
					new AxisAverages(stats.getAvgAcidity(), stats.getAvgSweetness(), stats.getAvgBody(),
							stats.getAvgBitterness(), stats.getAvgAftertaste()));
		}

	}

	public record AxisAverages(BigDecimal acidity, BigDecimal sweetness, BigDecimal body, BigDecimal bitterness,
			BigDecimal aftertaste) {
	}

	public record BeanSummaryResponse(Long id, String name, RoasterySummary roastery, List<Origin> origins,
			String region, ProcessMethod process, RoastLevel roastLevel, boolean blend, BeanStatus status,
			boolean seed, List<FlavorLabel> flavors, int noteCount, BigDecimal avgRating) {

		public static BeanSummaryResponse from(Bean bean, BeanStats stats) {
			StatsResponse s = StatsResponse.from(stats);
			return new BeanSummaryResponse(bean.getId(), bean.getName(), roasteryOf(bean), originsOf(bean),
					bean.getRegion(), bean.getProcess(), bean.getRoastLevel(), bean.isBlend(), bean.getStatus(),
					bean.isSeed(), flavorsOf(bean), s.noteCount(), s.avgRating());
		}

	}

	public record BeanDetailResponse(Long id, String name, RoasterySummary roastery, List<Origin> origins,
			String region, String farm, List<String> varieties, ProcessMethod process, RoastLevel roastLevel,
			boolean blend, LocalDate roastedOn, String officialFlavorText, List<FlavorLabel> flavors,
			BeanStatus status, boolean seed, StatsResponse stats, Instant createdAt) {

		public static BeanDetailResponse from(Bean bean, BeanStats stats) {
			return new BeanDetailResponse(bean.getId(), bean.getName(), roasteryOf(bean), originsOf(bean),
					bean.getRegion(), bean.getFarm(), bean.getVarieties().stream().sorted().toList(),
					bean.getProcess(), bean.getRoastLevel(), bean.isBlend(), bean.getRoastedOn(),
					bean.getOfficialFlavorText(), flavorsOf(bean), bean.getStatus(), bean.isSeed(),
					StatsResponse.from(stats), bean.getCreatedAt());
		}

	}

	/**
	 * 사용자 원두 등록 (B-3). 로스터리는 목록에서 고르거나(roasteryId) 새 이름을 입력한다(roasteryName).
	 * 카페에서 마신 커피는 roasteryName = 카페 이름, name = 메뉴 이름으로 등록한다 (14장 Q3 기본안).
	 */
	public record BeanCreateRequest(
			Long roasteryId,
			@Size(max = 100) String roasteryName,
			@NotBlank @Size(max = 100) String name,
			boolean blend,
			OriginCountry originCountry,
			@Size(max = 5) List<@NotNull OriginCountry> blendOrigins,
			@Size(max = 100) String region,
			@Size(max = 100) String farm,
			@Size(max = 5) List<@NotBlank @Size(max = 50) String> varieties,
			@NotNull ProcessMethod process,
			@NotNull RoastLevel roastLevel,
			LocalDate roastedOn,
			@Size(max = 500) String officialFlavorText,
			List<Long> flavorTagIds,
			boolean force) {
	}

	public record DuplicateCandidate(Long id, String name, String roasteryName) {

		public static DuplicateCandidate from(Bean bean) {
			return new DuplicateCandidate(bean.getId(), bean.getName(), bean.getRoastery().getName());
		}

	}

	private static RoasterySummary roasteryOf(Bean bean) {
		return new RoasterySummary(bean.getRoastery().getId(), bean.getRoastery().getName());
	}

	private static List<Origin> originsOf(Bean bean) {
		if (bean.isBlend()) {
			return bean.getBlendOrigins().stream().sorted().map(Origin::of).toList();
		}
		return List.of(Origin.of(bean.getOriginCountry()));
	}

	private static List<FlavorLabel> flavorsOf(Bean bean) {
		return FlavorService.sorted(bean.getOfficialFlavors()).stream().map(FlavorLabel::from).toList();
	}

}
