package com.beanote.roastery.domain;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roasteries")
public class Roastery {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(name = "website_url")
	private String websiteUrl;

	private String region;

	@Column(name = "is_seed", nullable = false)
	private boolean seed;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Roastery() {
	}

	/**
	 * 원두 등록 중 목록에 없는 로스터리(또는 카페)를 새로 만든다.
	 */
	public static Roastery create(String name) {
		Roastery roastery = new Roastery();
		roastery.name = name;
		roastery.seed = false;
		return roastery;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getWebsiteUrl() {
		return websiteUrl;
	}

	public String getRegion() {
		return region;
	}

	public boolean isSeed() {
		return seed;
	}

}
