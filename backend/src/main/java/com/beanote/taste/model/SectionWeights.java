package com.beanote.taste.model;

/**
 * 유사도 계산 때 구간별 가중치. 향미 대분류가 10차원이라 가중치 없이 코사인을 재면 향미가 유사도를 지배한다.
 */
public record SectionWeights(double taste, double flavor, double process, double roast) {

	public double of(VectorLayout.Section section) {
		return switch (section) {
			case TASTE -> taste;
			case FLAVOR -> flavor;
			case PROCESS -> process;
			case ROAST -> roast;
		};
	}

	public double ofIndex(int index) {
		return of(VectorLayout.sectionOf(index));
	}

}
