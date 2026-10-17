package com.beanote.flavor.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanote.flavor.domain.FlavorTag;
import com.beanote.flavor.dto.FlavorDtos.CategoryResponse;
import com.beanote.flavor.dto.FlavorDtos.TagResponse;
import com.beanote.flavor.repository.FlavorCategoryRepository;
import com.beanote.flavor.repository.FlavorTagRepository;
import com.beanote.global.error.BusinessException;
import com.beanote.global.error.ErrorCode;

@Service
@Transactional(readOnly = true)
public class FlavorService {

	/** 노트·원두 하나에 붙일 수 있는 향미 태그 최대 개수 (3-2) */
	public static final int MAX_TAGS = 5;

	private final FlavorCategoryRepository categoryRepository;
	private final FlavorTagRepository tagRepository;

	public FlavorService(FlavorCategoryRepository categoryRepository, FlavorTagRepository tagRepository) {
		this.categoryRepository = categoryRepository;
		this.tagRepository = tagRepository;
	}

	public List<CategoryResponse> getAll() {
		Map<Long, List<TagResponse>> tagsByCategory = tagRepository.findAllByOrderBySortOrder().stream()
				.collect(Collectors.groupingBy(tag -> tag.getCategory().getId(),
						Collectors.mapping(TagResponse::from, Collectors.toList())));

		return categoryRepository.findAllByOrderBySortOrder().stream()
				.map(category -> new CategoryResponse(category.getCode(), category.getNameKo(),
						tagsByCategory.getOrDefault(category.getId(), List.of())))
				.toList();
	}

	/**
	 * 향미 태그 id 목록을 엔티티로 바꾼다. 중복은 합치고, 최대 개수를 넘거나 없는 id 가 있으면 거부한다.
	 */
	public Set<FlavorTag> resolveTags(List<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return Set.of();
		}
		Set<Long> distinct = new LinkedHashSet<>(tagIds);
		if (distinct.contains(null)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		if (distinct.size() > MAX_TAGS) {
			throw new BusinessException(ErrorCode.FLAVOR_TAG_LIMIT_EXCEEDED);
		}
		List<FlavorTag> tags = tagRepository.findAllById(distinct);
		if (tags.size() != distinct.size()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "존재하지 않는 향미 태그가 있습니다.");
		}
		return new LinkedHashSet<>(tags);
	}

	public static List<FlavorTag> sorted(Collection<FlavorTag> tags) {
		return tags.stream()
				.sorted(Comparator
						.comparingInt((FlavorTag tag) -> tag.getCategory().getSortOrder())
						.thenComparingInt(FlavorTag::getSortOrder))
				.toList();
	}

}
