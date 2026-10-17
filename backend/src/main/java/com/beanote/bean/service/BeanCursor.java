package com.beanote.bean.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.data.jpa.domain.Specification;

import com.beanote.bean.domain.Bean;
import com.beanote.bean.dto.BeanSort;
import com.beanote.global.error.BusinessException;
import com.beanote.global.error.ErrorCode;

/**
 * 원두 목록 커서. 정렬 키와 id 를 함께 담아 같은 이름이 있어도 순서가 흔들리지 않게 한다.
 * 형식(base64url): LATEST → "L|id", NAME → "N|id|name"
 */
final class BeanCursor {

	private BeanCursor() {
	}

	static String encode(BeanSort sort, Bean last) {
		String raw = switch (sort) {
			case LATEST -> "L|" + last.getId();
			case NAME -> "N|" + last.getId() + "|" + last.getName();
		};
		return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * 커서 이후의 항목만 남기는 조건. 커서가 없으면 null.
	 */
	static Specification<Bean> after(BeanSort sort, String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		String[] parts = decode(cursor).split("\\|", 3);
		try {
			if (sort == BeanSort.LATEST && parts.length == 2 && parts[0].equals("L")) {
				long id = Long.parseLong(parts[1]);
				return (root, query, cb) -> cb.lessThan(root.get("id"), id);
			}
			if (sort == BeanSort.NAME && parts.length == 3 && parts[0].equals("N")) {
				long id = Long.parseLong(parts[1]);
				String name = parts[2];
				return (root, query, cb) -> cb.or(
						cb.greaterThan(root.get("name"), name),
						cb.and(cb.equal(root.get("name"), name), cb.greaterThan(root.get("id"), id)));
			}
		}
		catch (NumberFormatException e) {
			// 아래에서 INVALID_INPUT
		}
		throw new BusinessException(ErrorCode.INVALID_INPUT, "올바르지 않은 커서입니다.");
	}

	private static String decode(String cursor) {
		try {
			return new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
		}
		catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "올바르지 않은 커서입니다.");
		}
	}

}
