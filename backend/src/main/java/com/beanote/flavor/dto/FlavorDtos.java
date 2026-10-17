package com.beanote.flavor.dto;

import java.util.List;

import com.beanote.flavor.domain.FlavorTag;

public final class FlavorDtos {

	private FlavorDtos() {
	}

	public record CategoryResponse(String code, String name, List<TagResponse> tags) {
	}

	public record TagResponse(Long id, String code, String name, String nameEn) {

		public static TagResponse from(FlavorTag tag) {
			return new TagResponse(tag.getId(), tag.getCode(), tag.getNameKo(), tag.getNameEn());
		}

	}

	/**
	 * 원두·노트에 붙는 향미 태그 표시용 (대분류 코드 포함).
	 */
	public record FlavorLabel(Long id, String code, String name, String categoryCode) {

		public static FlavorLabel from(FlavorTag tag) {
			return new FlavorLabel(tag.getId(), tag.getCode(), tag.getNameKo(), tag.getCategory().getCode());
		}

	}

}
