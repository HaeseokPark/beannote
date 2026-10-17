package com.beanote.bean.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanote.bean.domain.Bean;
import com.beanote.bean.dto.BeanDtos.BeanDetailResponse;
import com.beanote.bean.dto.BeanDtos.BeanSummaryResponse;
import com.beanote.bean.dto.BeanSearchCondition;
import com.beanote.bean.dto.BeanSort;
import com.beanote.bean.repository.BeanRepository;
import com.beanote.bean.repository.BeanSpecifications;
import com.beanote.global.error.BusinessException;
import com.beanote.global.error.ErrorCode;
import com.beanote.global.response.CursorPage;

@Service
@Transactional(readOnly = true)
public class BeanService {

	private final BeanRepository beanRepository;

	public BeanService(BeanRepository beanRepository) {
		this.beanRepository = beanRepository;
	}

	public CursorPage<BeanSummaryResponse> search(BeanSearchCondition condition) {
		// 값이 없는 조건은 null 이므로 빼고 결합한다
		Specification<Bean> spec = Specification.allOf(Stream.of(
				BeanSpecifications.listed(),
				BeanSpecifications.keyword(condition.q()),
				BeanSpecifications.origin(condition.origin()),
				BeanSpecifications.process(condition.process()),
				BeanSpecifications.roastLevel(condition.roastLevel()),
				BeanSpecifications.flavorCategory(condition.flavorCategory()),
				BeanCursor.after(condition.sort(), condition.cursor()))
				.filter(Objects::nonNull)
				.toList());

		// 한 개 더 읽어서 다음 페이지가 있는지 판단한다
		List<Bean> rows = beanRepository.findBy(spec,
				query -> query.sortBy(sortOf(condition.sort())).limit(condition.size() + 1).all());

		boolean hasNext = rows.size() > condition.size();
		List<Bean> page = hasNext ? rows.subList(0, condition.size()) : rows;
		String nextCursor = hasNext ? BeanCursor.encode(condition.sort(), page.getLast()) : null;
		return new CursorPage<>(page.stream().map(BeanSummaryResponse::from).toList(), nextCursor);
	}

	public BeanDetailResponse getDetail(Long id) {
		Bean bean = beanRepository.findDetailById(id)
				.orElseThrow(() -> new BusinessException(ErrorCode.BEAN_NOT_FOUND));
		return BeanDetailResponse.from(bean);
	}

	private static Sort sortOf(BeanSort sort) {
		return switch (sort) {
			case LATEST -> Sort.by(Sort.Order.desc("id"));
			case NAME -> Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));
		};
	}

}
