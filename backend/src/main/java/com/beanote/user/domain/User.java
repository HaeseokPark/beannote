package com.beanote.user.domain;

import java.time.Instant;

import com.beanote.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String email;

	@Column(name = "password_hash")
	private String passwordHash;

	@Column(nullable = false, length = 20)
	private String nickname;

	@Column(name = "profile_image_url")
	private String profileImageUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuthProvider provider;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected User() {
	}

	private User(String email, String passwordHash, String nickname, Role role, AuthProvider provider) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.nickname = nickname;
		this.role = role;
		this.provider = provider;
	}

	public static User createLocal(String email, String passwordHash, String nickname) {
		return new User(email, passwordHash, nickname, Role.USER, AuthProvider.LOCAL);
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getNickname() {
		return nickname;
	}

	public String getProfileImageUrl() {
		return profileImageUrl;
	}

	public Role getRole() {
		return role;
	}

	public AuthProvider getProvider() {
		return provider;
	}

}
