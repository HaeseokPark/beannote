package com.beanote.bean.domain;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import com.beanote.flavor.domain.FlavorTag;
import com.beanote.global.entity.BaseTimeEntity;
import com.beanote.roastery.domain.Roastery;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "beans")
public class Bean extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "roastery_id")
	private Roastery roastery;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "name_normalized", nullable = false, length = 100)
	private String nameNormalized;

	/** 싱글오리진의 원산지 국가 코드. 블렌드는 null 이고 blendOrigins 를 쓴다. */
	@Column(name = "origin_country", length = 2)
	private String originCountry;

	private String region;

	private String farm;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProcessMethod process;

	@Enumerated(EnumType.STRING)
	@Column(name = "roast_level", nullable = false)
	private RoastLevel roastLevel;

	@Column(name = "is_blend", nullable = false)
	private boolean blend;

	@Column(name = "roasted_on")
	private LocalDate roastedOn;

	@Column(name = "official_flavor_text", length = 500)
	private String officialFlavorText;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BeanStatus status;

	@Column(name = "created_by")
	private Long createdBy;

	@Column(name = "merged_into_id")
	private Long mergedIntoId;

	@Column(name = "is_seed", nullable = false)
	private boolean seed;

	@ElementCollection
	@CollectionTable(name = "bean_varieties", joinColumns = @JoinColumn(name = "bean_id"))
	@Column(name = "variety")
	private Set<String> varieties = new LinkedHashSet<>();

	@ElementCollection
	@CollectionTable(name = "bean_blend_origins", joinColumns = @JoinColumn(name = "bean_id"))
	@Column(name = "country_code")
	private Set<String> blendOrigins = new LinkedHashSet<>();

	@ManyToMany
	@JoinTable(name = "bean_official_flavors", joinColumns = @JoinColumn(name = "bean_id"),
			inverseJoinColumns = @JoinColumn(name = "flavor_tag_id"))
	private Set<FlavorTag> officialFlavors = new LinkedHashSet<>();

	protected Bean() {
	}

	public Long getId() {
		return id;
	}

	public Roastery getRoastery() {
		return roastery;
	}

	public String getName() {
		return name;
	}

	public String getNameNormalized() {
		return nameNormalized;
	}

	public String getOriginCountry() {
		return originCountry;
	}

	public String getRegion() {
		return region;
	}

	public String getFarm() {
		return farm;
	}

	public ProcessMethod getProcess() {
		return process;
	}

	public RoastLevel getRoastLevel() {
		return roastLevel;
	}

	public boolean isBlend() {
		return blend;
	}

	public LocalDate getRoastedOn() {
		return roastedOn;
	}

	public String getOfficialFlavorText() {
		return officialFlavorText;
	}

	public BeanStatus getStatus() {
		return status;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public Long getMergedIntoId() {
		return mergedIntoId;
	}

	public boolean isSeed() {
		return seed;
	}

	public Set<String> getVarieties() {
		return varieties;
	}

	public Set<String> getBlendOrigins() {
		return blendOrigins;
	}

	public Set<FlavorTag> getOfficialFlavors() {
		return officialFlavors;
	}

}
