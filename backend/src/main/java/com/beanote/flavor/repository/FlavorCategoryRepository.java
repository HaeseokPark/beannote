package com.beanote.flavor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanote.flavor.domain.FlavorCategory;

public interface FlavorCategoryRepository extends JpaRepository<FlavorCategory, Long> {

	List<FlavorCategory> findAllByOrderBySortOrder();

	Optional<FlavorCategory> findByCode(String code);

}
