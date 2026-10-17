package com.beanote.taste.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanote.taste.domain.TasteProfile;

public interface TasteProfileRepository extends JpaRepository<TasteProfile, Long> {
}
