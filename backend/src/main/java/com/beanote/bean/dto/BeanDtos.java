package com.beanote.bean.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.beanote.bean.domain.Bean;
import com.beanote.bean.domain.BeanStatus;
import com.beanote.bean.domain.OriginCountry;
import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;
import com.beanote.flavor.dto.FlavorDtos.FlavorLabel;
import com.beanote.flavor.service.FlavorService;

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

	public record BeanSummaryResponse(Long id, String name, RoasterySummary roastery, List<Origin> origins,
			String region, ProcessMethod process, RoastLevel roastLevel, boolean blend, BeanStatus status,
			boolean seed, List<FlavorLabel> flavors) {

		public static BeanSummaryResponse from(Bean bean) {
			return new BeanSummaryResponse(bean.getId(), bean.getName(), roasteryOf(bean), originsOf(bean),
					bean.getRegion(), bean.getProcess(), bean.getRoastLevel(), bean.isBlend(), bean.getStatus(),
					bean.isSeed(), flavorsOf(bean));
		}

	}

	public record BeanDetailResponse(Long id, String name, RoasterySummary roastery, List<Origin> origins,
			String region, String farm, List<String> varieties, ProcessMethod process, RoastLevel roastLevel,
			boolean blend, LocalDate roastedOn, String officialFlavorText, List<FlavorLabel> flavors,
			BeanStatus status, boolean seed, Instant createdAt) {

		public static BeanDetailResponse from(Bean bean) {
			return new BeanDetailResponse(bean.getId(), bean.getName(), roasteryOf(bean), originsOf(bean),
					bean.getRegion(), bean.getFarm(), bean.getVarieties().stream().sorted().toList(),
					bean.getProcess(), bean.getRoastLevel(), bean.isBlend(), bean.getRoastedOn(),
					bean.getOfficialFlavorText(), flavorsOf(bean), bean.getStatus(), bean.isSeed(),
					bean.getCreatedAt());
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
