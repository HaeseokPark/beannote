package com.beanote.note.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.beanote.bean.domain.Bean;
import com.beanote.flavor.domain.FlavorTag;
import com.beanote.global.entity.BaseTimeEntity;
import com.beanote.user.domain.User;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
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
@Table(name = "tasting_notes")
public class TastingNote extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bean_id")
	private Bean bean;

	@Column(name = "tasted_on", nullable = false)
	private LocalDate tastedOn;

	@Enumerated(EnumType.STRING)
	@Column(name = "brew_method", nullable = false)
	private BrewMethod brewMethod;

	@Embedded
	private BrewRecipe recipe;

	@Embedded
	private TasteScores scores;

	@Column(nullable = false, precision = 2, scale = 1)
	private BigDecimal rating;

	@Column(name = "would_buy_again")
	private Boolean wouldBuyAgain;

	@Column(length = 1000)
	private String memo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Visibility visibility;

	@Column(name = "photo_url")
	private String photoUrl;

	@ManyToMany
	@JoinTable(name = "tasting_note_flavors", joinColumns = @JoinColumn(name = "note_id"),
			inverseJoinColumns = @JoinColumn(name = "flavor_tag_id"))
	private Set<FlavorTag> flavors = new LinkedHashSet<>();

	@ElementCollection
	@CollectionTable(name = "tasting_note_free_flavors", joinColumns = @JoinColumn(name = "note_id"))
	@Column(name = "raw_text")
	private Set<String> freeFlavors = new LinkedHashSet<>();

	protected TastingNote() {
	}

	public TastingNote(User user, Bean bean) {
		this.user = user;
		this.bean = bean;
	}

	/**
	 * 작성·수정 공통. 검증은 서비스 계층(NoteValidator)에서 끝난 값만 받는다.
	 */
	public void apply(Bean bean, LocalDate tastedOn, BrewMethod brewMethod, BrewRecipe recipe, TasteScores scores,
			BigDecimal rating, Boolean wouldBuyAgain, String memo, Visibility visibility, Set<FlavorTag> flavors,
			List<String> freeFlavors) {
		this.bean = bean;
		this.tastedOn = tastedOn;
		this.brewMethod = brewMethod;
		this.recipe = recipe;
		this.scores = scores;
		this.rating = rating;
		this.wouldBuyAgain = wouldBuyAgain;
		this.memo = memo;
		this.visibility = visibility;
		this.flavors.clear();
		this.flavors.addAll(flavors);
		this.freeFlavors.clear();
		this.freeFlavors.addAll(freeFlavors);
	}

	public boolean isOwnedBy(Long userId) {
		return user.getId().equals(userId);
	}

	public boolean isVisibleTo(Long viewerId) {
		return visibility == Visibility.PUBLIC || (viewerId != null && isOwnedBy(viewerId));
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Bean getBean() {
		return bean;
	}

	public LocalDate getTastedOn() {
		return tastedOn;
	}

	public BrewMethod getBrewMethod() {
		return brewMethod;
	}

	public BrewRecipe getRecipe() {
		return recipe;
	}

	public TasteScores getScores() {
		return scores;
	}

	public BigDecimal getRating() {
		return rating;
	}

	public Boolean getWouldBuyAgain() {
		return wouldBuyAgain;
	}

	public String getMemo() {
		return memo;
	}

	public Visibility getVisibility() {
		return visibility;
	}

	public String getPhotoUrl() {
		return photoUrl;
	}

	public Set<FlavorTag> getFlavors() {
		return flavors;
	}

	public Set<String> getFreeFlavors() {
		return freeFlavors;
	}

}
