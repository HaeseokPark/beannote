package com.beanote.flavor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanote.flavor.domain.FlavorTag;

public interface FlavorTagRepository extends JpaRepository<FlavorTag, Long> {

	List<FlavorTag> findAllByOrderBySortOrder();

}
