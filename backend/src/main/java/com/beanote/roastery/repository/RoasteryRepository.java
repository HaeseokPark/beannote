package com.beanote.roastery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanote.roastery.domain.Roastery;

public interface RoasteryRepository extends JpaRepository<Roastery, Long> {

	List<Roastery> findAllByOrderByName();

	List<Roastery> findByNameContainingOrderByName(String name);

}
