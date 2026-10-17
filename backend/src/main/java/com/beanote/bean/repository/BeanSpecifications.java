package com.beanote.bean.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.beanote.bean.domain.Bean;
import com.beanote.bean.domain.BeanStatus;
import com.beanote.bean.domain.OriginCountry;
import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;
import com.beanote.global.util.TextNormalizer;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * 원두 목록 검색 조건. 컬렉션 조건은 EXISTS 서브쿼리로 걸어 행이 중복되지 않게 한다.
 */
public final class BeanSpecifications {

	// MySQL 문자열 리터럴에서 역슬래시가 이스케이프로 해석되므로 LIKE 이스케이프 문자로 ! 를 쓴다
	private static final char ESCAPE = '!';

	private BeanSpecifications() {
	}

	/** 병합되지 않았고 승인 대기 중이 아닌 원두 (판매 종료 원두는 표시만 다르게 한다). */
	public static Specification<Bean> listed() {
		return (root, query, cb) -> cb.and(
				cb.isNull(root.get("mergedIntoId")),
				root.get("status").in(BeanStatus.ACTIVE, BeanStatus.DISCONTINUED));
	}

	public static Specification<Bean> keyword(String rawQuery) {
		if (rawQuery == null || rawQuery.isBlank()) {
			return null;
		}
		String trimmed = rawQuery.strip();
		String normalized = TextNormalizer.normalize(trimmed);
		List<String> countryCodes = OriginCountry.matchingName(normalized).stream().map(Enum::name).toList();

		return (root, query, cb) -> {
			List<Predicate> any = new ArrayList<>();
			any.add(cb.like(root.get("nameNormalized"), "%" + escape(normalized) + "%", ESCAPE));
			any.add(cb.like(root.get("roastery").get("name"), "%" + escape(trimmed) + "%", ESCAPE));
			any.add(cb.like(root.get("region"), "%" + escape(trimmed) + "%", ESCAPE));
			if (!countryCodes.isEmpty()) {
				any.add(root.get("originCountry").in(countryCodes));
				any.add(cb.exists(blendOriginSubquery(root, query, cb, countryCodes)));
			}
			return cb.or(any.toArray(Predicate[]::new));
		};
	}

	public static Specification<Bean> origin(OriginCountry origin) {
		if (origin == null) {
			return null;
		}
		List<String> codes = List.of(origin.name());
		return (root, query, cb) -> cb.or(
				cb.equal(root.get("originCountry"), origin.name()),
				cb.exists(blendOriginSubquery(root, query, cb, codes)));
	}

	public static Specification<Bean> process(ProcessMethod process) {
		return process == null ? null : (root, query, cb) -> cb.equal(root.get("process"), process);
	}

	public static Specification<Bean> roastLevel(RoastLevel roastLevel) {
		return roastLevel == null ? null : (root, query, cb) -> cb.equal(root.get("roastLevel"), roastLevel);
	}

	public static Specification<Bean> flavorCategory(String categoryCode) {
		if (categoryCode == null || categoryCode.isBlank()) {
			return null;
		}
		return (root, query, cb) -> {
			Subquery<Long> sub = query.subquery(Long.class);
			Root<Bean> inner = sub.from(Bean.class);
			Join<Object, Object> tag = inner.join("officialFlavors");
			sub.select(inner.get("id")).where(
					cb.equal(inner, root),
					cb.equal(tag.get("category").get("code"), categoryCode.strip()));
			return cb.exists(sub);
		};
	}

	private static Subquery<Long> blendOriginSubquery(Root<Bean> root, CriteriaQuery<?> query, CriteriaBuilder cb,
			List<String> codes) {
		Subquery<Long> sub = query.subquery(Long.class);
		Root<Bean> inner = sub.from(Bean.class);
		Join<Object, Object> blendOrigin = inner.join("blendOrigins");
		sub.select(inner.get("id")).where(cb.equal(inner, root), blendOrigin.in(codes));
		return sub;
	}

	private static String escape(String text) {
		return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
	}

}
