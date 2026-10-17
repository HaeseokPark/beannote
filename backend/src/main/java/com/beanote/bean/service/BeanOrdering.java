package com.beanote.bean.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.data.jpa.domain.Specification;

import com.beanote.bean.domain.Bean;
import com.beanote.bean.domain.BeanStats;
import com.beanote.bean.dto.BeanSort;
import com.beanote.global.error.BusinessException;
import com.beanote.global.error.ErrorCode;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * 원두 목록 정렬과 커서를 함께 다룬다. 정렬 키와 id 를 커서에 담아 같은 값이 있어도 순서가 흔들리지 않게 한다.
 * 커서 형식(base64url): "정렬|id|정렬값"
 */
final class BeanOrdering {

	private BeanOrdering() {
	}

	/**
	 * 정렬(ORDER BY)을 걸고, 커서가 있으면 커서 이후만 남기는 조건을 돌려준다.
	 * 통계 정렬은 bean_stats 를 LEFT JOIN 하고 통계가 없는 원두는 0 으로 본다.
	 */
	static Specification<Bean> apply(BeanSort sort, String cursor) {
		Cursor after = decode(sort, cursor);
		return (root, query, cb) -> {
			Predicate afterCursor = switch (sort) {
				case LATEST -> {
					query.orderBy(cb.desc(root.get("id")));
					yield after == null ? null : cb.lessThan(root.get("id"), after.id());
				}
				case NAME -> {
					query.orderBy(cb.asc(root.get("name")), cb.asc(root.get("id")));
					yield after == null ? null : cb.or(
							cb.greaterThan(root.get("name"), after.value()),
							cb.and(cb.equal(root.get("name"), after.value()), cb.greaterThan(root.get("id"), after.id())));
				}
				case POPULAR -> {
					Expression<Integer> count = cb.coalesce(statsJoin(root, cb).get("noteCount"), 0);
					query.orderBy(cb.desc(count), cb.desc(root.get("id")));
					yield after == null ? null : descendingAfter(cb, root, count, Integer.valueOf(after.value()), after.id());
				}
				case RATING -> {
					Expression<BigDecimal> rating = cb.coalesce(statsJoin(root, cb).get("avgRating"), BigDecimal.ZERO);
					query.orderBy(cb.desc(rating), cb.desc(root.get("id")));
					yield after == null ? null : descendingAfter(cb, root, rating, new BigDecimal(after.value()), after.id());
				}
			};
			return afterCursor == null ? cb.conjunction() : afterCursor;
		};
	}

	static String encode(BeanSort sort, Bean last, BeanStats stats) {
		String value = switch (sort) {
			case LATEST -> "";
			case NAME -> last.getName();
			case POPULAR -> String.valueOf(stats == null ? 0 : stats.getNoteCount());
			case RATING -> (stats == null || stats.getAvgRating() == null ? BigDecimal.ZERO : stats.getAvgRating())
					.toPlainString();
		};
		String raw = sort.name() + "|" + last.getId() + "|" + value;
		return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	private static Join<Bean, BeanStats> statsJoin(Root<Bean> root, CriteriaBuilder cb) {
		Join<Bean, BeanStats> stats = root.join(BeanStats.class, JoinType.LEFT);
		stats.on(cb.equal(stats.get("beanId"), root.get("id")));
		return stats;
	}

	private static <T extends Comparable<? super T>> Predicate descendingAfter(CriteriaBuilder cb, Root<Bean> root,
			Expression<T> key, T value, long id) {
		return cb.or(cb.lessThan(key, value), cb.and(cb.equal(key, value), cb.lessThan(root.get("id"), id)));
	}

	private static Cursor decode(BeanSort sort, String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		try {
			String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
			String[] parts = raw.split("\\|", 3);
			if (parts.length != 3 || !parts[0].equals(sort.name())) {
				throw invalid();
			}
			Cursor decoded = new Cursor(Long.parseLong(parts[1]), parts[2]);
			// 숫자 정렬값 형식 확인
			if (sort == BeanSort.POPULAR) {
				Integer.parseInt(decoded.value());
			}
			if (sort == BeanSort.RATING) {
				new BigDecimal(decoded.value());
			}
			return decoded;
		}
		catch (IllegalArgumentException e) {
			throw invalid();
		}
	}

	private static BusinessException invalid() {
		return new BusinessException(ErrorCode.INVALID_INPUT, "올바르지 않은 커서입니다.");
	}

	private record Cursor(long id, String value) {
	}

}
