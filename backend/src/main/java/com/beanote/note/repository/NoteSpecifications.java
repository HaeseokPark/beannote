package com.beanote.note.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import com.beanote.note.domain.TastingNote;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public final class NoteSpecifications {

	private NoteSpecifications() {
	}

	public static Specification<TastingNote> ownedBy(Long userId) {
		return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
	}

	public static Specification<TastingNote> bean(Long beanId) {
		return beanId == null ? null : (root, query, cb) -> cb.equal(root.get("bean").get("id"), beanId);
	}

	public static Specification<TastingNote> minRating(BigDecimal minRating) {
		return minRating == null ? null
				: (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("rating"), minRating);
	}

	public static Specification<TastingNote> flavorCategory(String categoryCode) {
		if (categoryCode == null || categoryCode.isBlank()) {
			return null;
		}
		return (root, query, cb) -> {
			Subquery<Long> sub = query.subquery(Long.class);
			Root<TastingNote> inner = sub.from(TastingNote.class);
			Join<Object, Object> tag = inner.join("flavors");
			sub.select(inner.get("id")).where(
					cb.equal(inner, root),
					cb.equal(tag.get("category").get("code"), categoryCode.strip()));
			return cb.exists(sub);
		};
	}

}
