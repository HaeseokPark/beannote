package com.beanote.note.domain;

import java.util.Objects;

import com.beanote.flavor.domain.FlavorMappingLog;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * 향미 자유 입력 한 조각과 그 표준화 결과.
 */
@Embeddable
public class FreeFlavor {

	@Column(name = "raw_text", nullable = false, length = 50)
	private String rawText;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mapping_log_id")
	private FlavorMappingLog mappingLog;

	protected FreeFlavor() {
	}

	public FreeFlavor(String rawText, FlavorMappingLog mappingLog) {
		this.rawText = rawText;
		this.mappingLog = mappingLog;
	}

	public String getRawText() {
		return rawText;
	}

	public FlavorMappingLog getMappingLog() {
		return mappingLog;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof FreeFlavor other && rawText.equals(other.rawText);
	}

	@Override
	public int hashCode() {
		return Objects.hash(rawText);
	}

}
