package com.beanote.note.controller;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.beanote.global.response.ApiResponse;
import com.beanote.global.response.CursorPage;
import com.beanote.global.security.CurrentUserId;
import com.beanote.note.dto.NoteDtos.MyNoteCondition;
import com.beanote.note.dto.NoteDtos.NoteDetailResponse;
import com.beanote.note.dto.NoteDtos.NoteRequest;
import com.beanote.note.dto.NoteDtos.NoteSummaryResponse;
import com.beanote.note.service.NoteService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

@Tag(name = "Note")
@Validated
@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

	private final NoteService noteService;

	public NoteController(NoteService noteService) {
		this.noteService = noteService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<NoteDetailResponse> create(@CurrentUserId Long userId,
			@Valid @RequestBody NoteRequest request) {
		Long noteId = noteService.create(userId, request);
		return ApiResponse.ok(noteService.get(userId, noteId));
	}

	@GetMapping("/me")
	public ApiResponse<CursorPage<NoteSummaryResponse>> myNotes(@CurrentUserId Long userId,
			@RequestParam(required = false) Long beanId,
			@RequestParam(required = false) @DecimalMin("0.5") @DecimalMax("5.0") BigDecimal minRating,
			@RequestParam(required = false) String flavorCategory,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(noteService.getMyNotes(userId,
				new MyNoteCondition(beanId, minRating, flavorCategory, cursor, size)));
	}

	/**
	 * 공개 노트는 누구나, 비공개 노트는 작성자만 조회할 수 있다. 로그인은 선택이다.
	 */
	@GetMapping("/{id:\\d+}")
	public ApiResponse<NoteDetailResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		Long viewerId = jwt == null ? null : Long.valueOf(jwt.getSubject());
		return ApiResponse.ok(noteService.get(viewerId, id));
	}

	@PutMapping("/{id:\\d+}")
	public ApiResponse<NoteDetailResponse> update(@CurrentUserId Long userId, @PathVariable Long id,
			@Valid @RequestBody NoteRequest request) {
		noteService.update(userId, id, request);
		return ApiResponse.ok(noteService.get(userId, id));
	}

	@DeleteMapping("/{id:\\d+}")
	public ApiResponse<Void> delete(@CurrentUserId Long userId, @PathVariable Long id) {
		noteService.delete(userId, id);
		return ApiResponse.ok();
	}

}
