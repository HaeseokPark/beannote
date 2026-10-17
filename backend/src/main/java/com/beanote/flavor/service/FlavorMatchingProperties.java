package com.beanote.flavor.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * beanote.flavor.* — 판정 기준 점수 (F-3). 이 점수 이상이면 AUTO, review 이상이면 REVIEW, 미만이면 UNMAPPED.
 */
@ConfigurationProperties(prefix = "beanote.flavor")
public record FlavorMatchingProperties(double autoThreshold, double reviewThreshold) {
}
