package com.beanote.note.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;

import org.springframework.data.jpa.domain.Specification;

import com.beanote.global.error.BusinessException;
import com.beanote.global.error.ErrorCode;
import com.beanote.note.domain.TastingNote;

/**
 * 내 노트 목록 커서 (작성 시각 내림차순, 같으면 id 내림차순). 형식(base64url): "createdAt|id"
 */
final class NoteCursor {

	private NoteCursor() {
	}

	static String encode(TastingNote last) {
		String raw = last.getCreatedAt() + "|" + last.getId();
		return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	static Specification<TastingNote> after(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		try {
			String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|");
			if (parts.length != 2) {
				throw invalid();
			}
			Instant createdAt = Instant.parse(parts[0]);
			long id = Long.parseLong(parts[1]);
			return (root, query, cb) -> cb.or(
					cb.lessThan(root.get("createdAt"), createdAt),
					cb.and(cb.equal(root.get("createdAt"), createdAt), cb.lessThan(root.get("id"), id)));
		}
		catch (IllegalArgumentException | DateTimeParseException e) {
			throw invalid();
		}
	}

	private static BusinessException invalid() {
		return new BusinessException(ErrorCode.INVALID_INPUT, "올바르지 않은 커서입니다.");
	}

}
