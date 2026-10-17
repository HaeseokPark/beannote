package com.beanote.note.service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanote.bean.domain.Bean;
import com.beanote.bean.repository.BeanRepository;
import com.beanote.bean.repository.BeanStatsRepository;
import com.beanote.flavor.domain.FlavorMappingLog;
import com.beanote.flavor.domain.FlavorTag;
import com.beanote.flavor.domain.MappingSource;
import com.beanote.flavor.service.FlavorNormalizationService;
import com.beanote.flavor.service.FlavorService;
import com.beanote.global.error.BusinessException;
import com.beanote.global.error.ErrorCode;
import com.beanote.global.response.CursorPage;
import com.beanote.note.domain.FreeFlavor;
import com.beanote.note.domain.TastingNote;
import com.beanote.note.dto.NoteDtos.MyNoteCondition;
import com.beanote.note.dto.NoteDtos.NoteDetailResponse;
import com.beanote.note.dto.NoteDtos.NoteRequest;
import com.beanote.note.dto.NoteDtos.NoteSummaryResponse;
import com.beanote.note.repository.NoteSpecifications;
import com.beanote.note.repository.TastingNoteRepository;
import com.beanote.user.domain.User;
import com.beanote.user.repository.UserRepository;

/**
 * 시음 노트 CRUD (N-1 ~ N-4).
 * 비공개 노트와 남의 노트는 "없는 노트"와 똑같이 NOTE_NOT_FOUND 로 응답해 존재 여부를 드러내지 않는다.
 */
@Service
@Transactional
public class NoteService {

	private final TastingNoteRepository noteRepository;
	private final BeanRepository beanRepository;
	private final BeanStatsRepository beanStatsRepository;
	private final UserRepository userRepository;
	private final NoteValidator validator;
	private final FlavorService flavorService;
	private final FlavorNormalizationService normalizationService;

	public NoteService(TastingNoteRepository noteRepository, BeanRepository beanRepository,
			BeanStatsRepository beanStatsRepository, UserRepository userRepository, NoteValidator validator,
			FlavorService flavorService, FlavorNormalizationService normalizationService) {
		this.noteRepository = noteRepository;
		this.beanRepository = beanRepository;
		this.beanStatsRepository = beanStatsRepository;
		this.userRepository = userRepository;
		this.validator = validator;
		this.flavorService = flavorService;
		this.normalizationService = normalizationService;
	}

	public Long create(Long userId, NoteRequest request) {
		User user = userRepository.findById(userId)
				.filter(u -> !u.isDeleted())
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		TastingNote note = new TastingNote(user, null);
		List<FlavorMappingLog> logs = applyRequest(note, request);
		noteRepository.save(note);
		logs.forEach(log -> log.attachTo(note.getId()));

		beanStatsRepository.recalculate(note.getBean().getId());
		return note.getId();
	}

	@Transactional(readOnly = true)
	public NoteDetailResponse get(Long viewerId, Long noteId) {
		TastingNote note = noteRepository.findDetailById(noteId)
				.filter(n -> n.isVisibleTo(viewerId))
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
		return NoteDetailResponse.from(note, viewerId);
	}

	public void update(Long userId, Long noteId, NoteRequest request) {
		TastingNote note = findOwned(userId, noteId);
		Long previousBeanId = note.getBean().getId();
		applyRequest(note, request).forEach(log -> log.attachTo(noteId));

		beanStatsRepository.recalculate(note.getBean().getId());
		if (!previousBeanId.equals(note.getBean().getId())) {
			beanStatsRepository.recalculate(previousBeanId);
		}
	}

	public void delete(Long userId, Long noteId) {
		TastingNote note = findOwned(userId, noteId);
		Long beanId = note.getBean().getId();
		noteRepository.delete(note);

		beanStatsRepository.recalculate(beanId);
	}

	@Transactional(readOnly = true)
	public CursorPage<NoteSummaryResponse> getMyNotes(Long userId, MyNoteCondition condition) {
		Specification<TastingNote> spec = Specification.allOf(Stream.of(
				NoteSpecifications.ownedBy(userId),
				NoteSpecifications.bean(condition.beanId()),
				NoteSpecifications.minRating(condition.minRating()),
				NoteSpecifications.flavorCategory(condition.flavorCategory()),
				NoteCursor.after(condition.cursor()))
				.filter(Objects::nonNull)
				.toList());

		List<TastingNote> rows = noteRepository.findBy(spec, query -> query
				.sortBy(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
				.limit(condition.size() + 1)
				.all());

		boolean hasNext = rows.size() > condition.size();
		List<TastingNote> page = hasNext ? rows.subList(0, condition.size()) : rows;
		String nextCursor = hasNext ? NoteCursor.encode(page.getLast()) : null;
		return new CursorPage<>(page.stream().map(NoteSummaryResponse::from).toList(), nextCursor);
	}

	private TastingNote findOwned(Long userId, Long noteId) {
		return noteRepository.findById(noteId)
				.filter(note -> note.isOwnedBy(userId))
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
	}

	/**
	 * 요청 값을 노트에 반영한다. 향미 자유 입력은 표준화 기록(mapping log)을 남기고, 그 기록을 돌려준다.
	 */
	private List<FlavorMappingLog> applyRequest(TastingNote note, NoteRequest request) {
		validator.validate(request);
		Bean bean = beanRepository.findDetailById(request.beanId())
				.orElseThrow(() -> new BusinessException(ErrorCode.BEAN_NOT_FOUND));
		Set<FlavorTag> flavors = flavorService.resolveTags(request.flavorTagIds());
		List<FlavorMappingLog> logs = validator.normalizeFreeFlavors(request.freeFlavors()).stream()
				.map(text -> normalizationService.record(text, MappingSource.NOTE))
				.toList();
		List<FreeFlavor> freeFlavors = logs.stream().map(log -> new FreeFlavor(log.getRawText(), log)).toList();

		note.apply(bean, request.tastedOn(), request.brewMethod(),
				request.recipe() == null ? null : request.recipe().toRecipe(),
				request.scores().toScores(), request.rating(), request.wouldBuyAgain(),
				request.memo() == null ? null : request.memo().strip(), request.visibility(), flavors, freeFlavors);
		return logs;
	}

}
