package com.beanote.roastery.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanote.roastery.dto.RoasteryResponse;
import com.beanote.roastery.repository.RoasteryRepository;

@Service
@Transactional(readOnly = true)
public class RoasteryService {

	private final RoasteryRepository roasteryRepository;

	public RoasteryService(RoasteryRepository roasteryRepository) {
		this.roasteryRepository = roasteryRepository;
	}

	/**
	 * 로스터리 목록·검색. 수가 적어(수십 곳) 페이지네이션 없이 전체를 돌려준다.
	 */
	public List<RoasteryResponse> search(String query) {
		var roasteries = (query == null || query.isBlank())
				? roasteryRepository.findAllByOrderByName()
				: roasteryRepository.findByNameContainingOrderByName(query.strip());
		return roasteries.stream().map(RoasteryResponse::from).toList();
	}

}
