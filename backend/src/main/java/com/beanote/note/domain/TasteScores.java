package com.beanote.note.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 맛 축 점수 5개 (3-1), 각 1~5.
 */
@Embeddable
public record TasteScores(
		@Column(name = "acidity", nullable = false) int acidity,
		@Column(name = "sweetness", nullable = false) int sweetness,
		@Column(name = "body", nullable = false) int body,
		@Column(name = "bitterness", nullable = false) int bitterness,
		@Column(name = "aftertaste", nullable = false) int aftertaste) {
}
