package com.beanote.bean.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanote.bean.domain.Bean;
import com.beanote.bean.domain.BeanStats;
import com.beanote.bean.domain.OriginCountry;
import com.beanote.bean.dto.BeanDtos.BeanCreateRequest;
import com.beanote.bean.dto.BeanDtos.BeanDetailResponse;
import com.beanote.bean.dto.BeanDtos.BeanSummaryResponse;
import com.beanote.bean.dto.BeanDtos.DuplicateCandidate;
import com.beanote.bean.dto.BeanSearchCondition;
import com.beanote.bean.repository.BeanRepository;
import com.beanote.bean.repository.BeanSpecifications;
import com.beanote.bean.repository.BeanStatsRepository;
import com.beanote.flavor.domain.FlavorMappingLog;
import com.beanote.flavor.domain.FlavorTag;
import com.beanote.flavor.domain.MappingSource;
import com.beanote.flavor.service.FlavorNormalizationService;
import com.beanote.flavor.service.FlavorService;
import com.beanote.global.error.BusinessException;
import com.beanote.global.error.ErrorCode;
import com.beanote.global.response.CursorPage;
import com.beanote.roastery.domain.Roastery;
import com.beanote.taste.event.TasteInputChangedEvent;
import com.beanote.roastery.repository.RoasteryRepository;

@Service
@Transactional(readOnly = true)
public class BeanService {

	private final BeanRepository beanRepository;
	private final BeanStatsRepository beanStatsRepository;
	private final RoasteryRepository roasteryRepository;
	private final FlavorService flavorService;
	private final FlavorNormalizationService normalizationService;
	private final ApplicationEventPublisher events;

	public BeanService(BeanRepository beanRepository, BeanStatsRepository beanStatsRepository,
			RoasteryRepository roasteryRepository, FlavorService flavorService,
			FlavorNormalizationService normalizationService, ApplicationEventPublisher events) {
		this.beanRepository = beanRepository;
		this.beanStatsRepository = beanStatsRepository;
		this.roasteryRepository = roasteryRepository;
		this.flavorService = flavorService;
		this.normalizationService = normalizationService;
		this.events = events;
	}

	public CursorPage<BeanSummaryResponse> search(BeanSearchCondition condition) {
		// 값이 없는 조건은 null 이므로 빼고 결합한다. 정렬은 BeanOrdering 이 ORDER BY 로 건다
		Specification<Bean> spec = Specification.allOf(Stream.of(
				BeanSpecifications.listed(),
				BeanSpecifications.keyword(condition.q()),
				BeanSpecifications.origin(condition.origin()),
				BeanSpecifications.process(condition.process()),
				BeanSpecifications.roastLevel(condition.roastLevel()),
				BeanSpecifications.flavorCategory(condition.flavorCategory()),
				BeanOrdering.apply(condition.sort(), condition.cursor()))
				.filter(Objects::nonNull)
				.toList());

		// 한 개 더 읽어서 다음 페이지가 있는지 판단한다
		List<Bean> rows = beanRepository.findBy(spec, query -> query.limit(condition.size() + 1).all());

		boolean hasNext = rows.size() > condition.size();
		List<Bean> page = hasNext ? rows.subList(0, condition.size()) : rows;
		Map<Long, BeanStats> stats = statsOf(page);
		String nextCursor = hasNext
				? BeanOrdering.encode(condition.sort(), page.getLast(), stats.get(page.getLast().getId()))
				: null;
		return new CursorPage<>(
				page.stream().map(bean -> BeanSummaryResponse.from(bean, stats.get(bean.getId()))).toList(),
				nextCursor);
	}

	/**
	 * 주어진 순서대로 원두 요약을 만든다 (추천·비슷한 원두 목록용). 없는 id 는 건너뛴다.
	 */
	public List<BeanSummaryResponse> summaries(List<Long> ids) {
		Map<Long, Bean> beans = beanRepository.findAllById(ids).stream()
				.collect(Collectors.toMap(Bean::getId, Function.identity()));
		List<Bean> ordered = ids.stream().map(beans::get).filter(Objects::nonNull).toList();
		Map<Long, BeanStats> stats = statsOf(ordered);
		return ordered.stream().map(bean -> BeanSummaryResponse.from(bean, stats.get(bean.getId()))).toList();
	}

	public boolean isListed(Long id) {
		return beanRepository.findDetailById(id).isPresent();
	}

	public BeanDetailResponse getDetail(Long id) {
		Bean bean = beanRepository.findDetailById(id)
				.orElseThrow(() -> new BusinessException(ErrorCode.BEAN_NOT_FOUND));
		return BeanDetailResponse.from(bean, beanStatsRepository.findById(id).orElse(null));
	}

	/**
	 * 중복 후보 (B-3). 로스터리를 고른 경우 그 로스터리의 원두, 새 로스터리 이름이면 같은 이름의 로스터리가 있을 때만 찾는다.
	 */
	public List<DuplicateCandidate> findDuplicates(Long roasteryId, String roasteryName, String name) {
		return findRoastery(roasteryId, roasteryName)
				.map(roastery -> DuplicateBeanDetector.findCandidates(name,
						beanRepository.findAllByRoasteryId(roastery.getId())))
				.orElse(List.of())
				.stream()
				.map(DuplicateCandidate::from)
				.toList();
	}

	@Transactional
	public Long register(Long userId, BeanCreateRequest request) {
		validateOrigins(request);
		if (!request.force() && !findDuplicates(request.roasteryId(), request.roasteryName(), request.name()).isEmpty()) {
			throw new BusinessException(ErrorCode.BEAN_DUPLICATE_SUSPECTED);
		}

		Roastery roastery = findRoastery(request.roasteryId(), request.roasteryName())
				.orElseGet(() -> roasteryRepository.save(Roastery.create(request.roasteryName().strip())));

		Set<String> blendOrigins = request.blend()
				? request.blendOrigins().stream().map(Enum::name).collect(Collectors.toCollection(LinkedHashSet::new))
				: Set.of();
		Set<String> varieties = request.varieties() == null ? Set.of()
				: request.varieties().stream().map(String::strip).collect(Collectors.toCollection(LinkedHashSet::new));

		// 공식 향미 설명은 원문을 저장하고, 표준 태그로도 바꿔 둔다 (3-3)
		String officialText = strip(request.officialFlavorText());
		List<FlavorMappingLog> logs = officialText == null ? List.of()
				: normalizationService.recordAll(officialText, MappingSource.BEAN);
		Set<FlavorTag> officialFlavors = officialFlavors(flavorService.resolveTags(request.flavorTagIds()), logs);

		Bean bean = Bean.registerByUser(userId, roastery, request.name().strip(), request.blend(),
				request.originCountry() == null ? null : request.originCountry().name(), blendOrigins,
				strip(request.region()), strip(request.farm()), varieties, request.process(), request.roastLevel(),
				request.roastedOn(), officialText, officialFlavors);
		Long beanId = beanRepository.save(bean).getId();
		logs.forEach(log -> log.attachTo(beanId));
		events.publishEvent(TasteInputChangedEvent.ofBean(beanId));
		return beanId;
	}

	/**
	 * 사용자가 고른 태그를 먼저 두고, 공식 설명에서 확실하게(EXACT·AUTO) 찾은 태그를 최대 개수까지 더한다.
	 */
	private static Set<FlavorTag> officialFlavors(Set<FlavorTag> picked, List<FlavorMappingLog> logs) {
		Set<FlavorTag> result = new LinkedHashSet<>(picked);
		Set<Long> ids = picked.stream().map(FlavorTag::getId).collect(Collectors.toSet());
		for (FlavorMappingLog log : logs) {
			if (result.size() >= FlavorService.MAX_TAGS) {
				break;
			}
			FlavorTag tag = log.getFlavorTag();
			if (log.getResult().isConfident() && tag != null && ids.add(tag.getId())) {
				result.add(tag);
			}
		}
		return result;
	}

	private Optional<Roastery> findRoastery(Long roasteryId, String roasteryName) {
		if (roasteryId != null) {
			return Optional.of(roasteryRepository.findById(roasteryId)
					.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "로스터리를 찾을 수 없습니다.")));
		}
		if (roasteryName == null || roasteryName.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "로스터리를 선택하거나 이름을 입력해 주세요.");
		}
		return roasteryRepository.findByName(roasteryName.strip());
	}

	private static void validateOrigins(BeanCreateRequest request) {
		if (request.blend()) {
			List<OriginCountry> origins = request.blendOrigins();
			if (origins == null || origins.stream().distinct().count() < 2) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "블렌드는 원산지를 2개 이상 입력해 주세요.");
			}
		}
		else if (request.originCountry() == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "싱글오리진은 원산지 국가를 입력해 주세요.");
		}
	}

	private Map<Long, BeanStats> statsOf(List<Bean> beans) {
		return beanStatsRepository.findAllById(beans.stream().map(Bean::getId).toList()).stream()
				.collect(Collectors.toMap(BeanStats::getBeanId, Function.identity()));
	}

	private static String strip(String text) {
		return text == null || text.isBlank() ? null : text.strip();
	}

}
