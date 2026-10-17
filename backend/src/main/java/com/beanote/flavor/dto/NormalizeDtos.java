package com.beanote.flavor.dto;

import com.beanote.flavor.domain.FlavorMappingLog;
import com.beanote.flavor.domain.MappingResult;
import com.beanote.flavor.dto.FlavorDtos.FlavorLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class NormalizeDtos {

	private NormalizeDtos() {
	}

	public record NormalizeRequest(@NotBlank @Size(max = 500) String text) {
	}

	public record CategoryLabel(String code, String name) {
	}

	/**
	 * 표현 조각 하나의 판정. EXACT·AUTO 이면 tag/category 가 확정 매핑, REVIEW 이면 제안, UNMAPPED 이면 둘 다 null.
	 */
	public record MatchResponse(String raw, String normalized, MappingResult result, double score, FlavorLabel tag,
			CategoryLabel category) {

		public static MatchResponse from(FlavorMappingLog log) {
			FlavorLabel tag = log.getFlavorTag() == null ? null : FlavorLabel.from(log.getFlavorTag());
			CategoryLabel category = log.getCategory() == null ? null
					: new CategoryLabel(log.getCategory().getCode(), log.getCategory().getNameKo());
			return new MatchResponse(log.getRawText(), log.getNormalized(), log.getResult(),
					log.getScore().doubleValue(), tag, category);
		}

	}

}
