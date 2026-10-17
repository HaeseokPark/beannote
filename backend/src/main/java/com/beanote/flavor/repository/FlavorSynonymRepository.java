package com.beanote.flavor.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanote.flavor.domain.FlavorSynonym;

public interface FlavorSynonymRepository extends JpaRepository<FlavorSynonym, Long> {
}
