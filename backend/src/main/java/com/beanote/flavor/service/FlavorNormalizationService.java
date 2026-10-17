package com.beanote.flavor.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanote.flavor.domain.FlavorCategory;
import com.beanote.flavor.domain.FlavorMappingLog;
import com.beanote.flavor.domain.FlavorTag;
import com.beanote.flavor.domain.MappingSource;
import com.beanote.flavor.dto.FlavorDtos.FlavorLabel;
import com.beanote.flavor.dto.NormalizeDtos.CategoryLabel;
import com.beanote.flavor.dto.NormalizeDtos.MatchResponse;
import com.beanote.flavor.matching.DictionaryEntry;
import com.beanote.flavor.matching.FlavorMatch;
import com.beanote.flavor.matching.FlavorMatcher;
import com.beanote.flavor.repository.FlavorCategoryRepository;
import com.beanote.flavor.repository.FlavorMappingLogRepository;
import com.beanote.flavor.repository.FlavorSynonymRepository;
import com.beanote.flavor.repository.FlavorTagRepository;

/**
 * 향미 표현 표준화 (4-6). 동의어 사전을 메모리에 올려 두고 FlavorMatcher 로 판정한다.
 * 사전이 바뀌면(검토 큐 확정, P1) reload() 로 다시 읽는다.
 */
@Service
@EnableConfigurationProperties(FlavorMatchingProperties.class)
public class FlavorNormalizationService {

	private final FlavorSynonymRepository synonymRepository;
	private final FlavorTagRepository tagRepository;
	private final FlavorCategoryRepository categoryRepository;
	private final FlavorMappingLogRepository logRepository;
	private final FlavorMatchingProperties properties;

	private volatile Snapshot snapshot;

	public FlavorNormalizationService(FlavorSynonymRepository synonymRepository, FlavorTagRepository tagRepository,
			FlavorCategoryRepository categoryRepository, FlavorMappingLogRepository logRepository,
			FlavorMatchingProperties properties) {
		this.synonymRepository = synonymRepository;
		this.tagRepository = tagRepository;
		this.categoryRepository = categoryRepository;
		this.logRepository = logRepository;
		this.properties = properties;
	}

	/**
	 * 저장하지 않고 판정만 한다. 노트 작성 화면의 "태그 후보 제안"용.
	 */
	@Transactional(readOnly = true)
	public List<MatchResponse> preview(String text) {
		Snapshot current = snapshot();
		return current.matcher().matchAll(text).stream().map(match -> toResponse(match, current)).toList();
	}

	/**
	 * 표현 조각 하나를 판정하고 기록을 남긴다. sourceId 는 저장 대상(노트·원두) id 가 생긴 뒤 attachTo 로 붙인다.
	 */
	@Transactional
	public FlavorMappingLog record(String segment, MappingSource source) {
		FlavorMatch match = snapshot().matcher().match(segment);
		DictionaryEntry entry = match.entry();
		FlavorTag tag = entry == null || entry.tagId() == null ? null : tagRepository.getReferenceById(entry.tagId());
		FlavorCategory category = entry == null || entry.categoryId() == null ? null
				: categoryRepository.getReferenceById(entry.categoryId());
		return logRepository.save(new FlavorMappingLog(segment, match.normalized(), match.result(),
				BigDecimal.valueOf(match.score()).setScale(2, RoundingMode.HALF_UP), tag, category, source));
	}

	/**
	 * 여러 표현이 섞인 글(예: 원두 공식 설명 "자스민, 복숭아, 꿀")을 조각마다 판정하고 기록한다.
	 */
	@Transactional
	public List<FlavorMappingLog> recordAll(String text, MappingSource source) {
		return FlavorMatcher.splitSegments(text).stream().map(segment -> record(segment, source)).toList();
	}

	/** 판정기 (리포트·테스트용) */
	@Transactional(readOnly = true)
	public FlavorMatcher matcher() {
		return snapshot().matcher();
	}

	public synchronized void reload() {
		snapshot = null;
	}

	private Snapshot snapshot() {
		Snapshot current = snapshot;
		if (current == null) {
			synchronized (this) {
				current = snapshot;
				if (current == null) {
					current = load();
					snapshot = current;
				}
			}
		}
		return current;
	}

	private Snapshot load() {
		List<DictionaryEntry> entries = synonymRepository.findAll().stream()
				.map(s -> new DictionaryEntry(s.getExpressionNormalized(), s.getFlavorTagId(), s.getCategoryId()))
				.toList();
		Map<Long, FlavorLabel> tags = tagRepository.findAllByOrderBySortOrder().stream()
				.collect(Collectors.toMap(FlavorTag::getId, FlavorLabel::from));
		Map<Long, CategoryLabel> categories = categoryRepository.findAllByOrderBySortOrder().stream()
				.collect(Collectors.toMap(FlavorCategory::getId,
						c -> new CategoryLabel(c.getCode(), c.getNameKo()), (a, b) -> a));
		return new Snapshot(new FlavorMatcher(entries, properties.autoThreshold(), properties.reviewThreshold()),
				tags, categories);
	}

	private static MatchResponse toResponse(FlavorMatch match, Snapshot snapshot) {
		DictionaryEntry entry = match.entry();
		FlavorLabel tag = entry == null || entry.tagId() == null ? null : snapshot.tags().get(entry.tagId());
		CategoryLabel category = entry == null || entry.categoryId() == null ? null
				: snapshot.categories().get(entry.categoryId());
		return new MatchResponse(match.raw(), match.normalized(), match.result(), match.score(), tag, category);
	}

	private record Snapshot(FlavorMatcher matcher, Map<Long, FlavorLabel> tags, Map<Long, CategoryLabel> categories) {
	}

}
