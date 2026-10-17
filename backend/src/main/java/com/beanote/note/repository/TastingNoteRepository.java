package com.beanote.note.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beanote.note.domain.TastingNote;

public interface TastingNoteRepository extends JpaRepository<TastingNote, Long>, JpaSpecificationExecutor<TastingNote> {

	@Query("""
			select n from TastingNote n
			join fetch n.user
			join fetch n.bean b
			join fetch b.roastery
			where n.id = :id
			""")
	Optional<TastingNote> findDetailById(@Param("id") Long id);

	/** 취향 계산용 사용자 노트 전체. 원두·향미는 배치 로딩된다. */
	@Query("select n from TastingNote n join fetch n.bean where n.user.id = :userId")
	List<TastingNote> findAllByUserId(@Param("userId") Long userId);

}
