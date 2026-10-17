package com.beanote.note.repository;

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

}
