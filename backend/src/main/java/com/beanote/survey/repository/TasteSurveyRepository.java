package com.beanote.survey.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanote.survey.domain.TasteSurvey;

public interface TasteSurveyRepository extends JpaRepository<TasteSurvey, Long> {
}
