package com.beanote.roastery.dto;

import com.beanote.roastery.domain.Roastery;

public record RoasteryResponse(Long id, String name, String region, String websiteUrl) {

	public static RoasteryResponse from(Roastery roastery) {
		return new RoasteryResponse(roastery.getId(), roastery.getName(), roastery.getRegion(),
				roastery.getWebsiteUrl());
	}

}
