package com.beanote.taste.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 벡터 ↔ JSON 배열 문자열. DB 에는 JSON 컬럼으로 저장한다 (6-3).
 */
public final class VectorCodec {

	private VectorCodec() {
	}

	public static String toJson(double[] vector) {
		return Arrays.stream(vector)
				.mapToObj(v -> String.format(Locale.ROOT, "%.6f", v))
				.collect(Collectors.joining(",", "[", "]"));
	}

	public static double[] fromJson(String json) {
		if (json == null || json.isBlank()) {
			return null;
		}
		String body = json.strip();
		body = body.substring(1, body.length() - 1).strip();
		if (body.isEmpty()) {
			return new double[0];
		}
		return Arrays.stream(body.split(",")).mapToDouble(s -> Double.parseDouble(s.strip())).toArray();
	}

}
