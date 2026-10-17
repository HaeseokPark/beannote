package com.beanote.flavor.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanote.flavor.domain.FlavorMappingLog;

public interface FlavorMappingLogRepository extends JpaRepository<FlavorMappingLog, Long> {
}
