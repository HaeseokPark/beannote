package com.beanote.flavor.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanote.flavor.domain.FlavorTag;
import com.beanote.flavor.dto.FlavorDtos.CategoryResponse;
import com.beanote.flavor.dto.FlavorDtos.TagResponse;
import com.beanote.flavor.repository.FlavorCategoryRepository;
import com.beanote.flavor.repository.FlavorTagRepository;

@Service
@Transactional(readOnly = true)
public class FlavorService {

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

	public static List<FlavorTag> sorted(Collection<FlavorTag> tags) {
		return tags.stream()
				.sorted(Comparator
						.comparingInt((FlavorTag tag) -> tag.getCategory().getSortOrder())
						.thenComparingInt(FlavorTag::getSortOrder))
				.toList();
	}

}
