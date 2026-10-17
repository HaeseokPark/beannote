package com.beanote.bean.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beanote.bean.domain.Bean;

public interface BeanRepository extends JpaRepository<Bean, Long>, JpaSpecificationExecutor<Bean> {

	@Query("select b from Bean b join fetch b.roastery where b.id = :id and b.mergedIntoId is null")
	Optional<Bean> findDetailById(@Param("id") Long id);

}
