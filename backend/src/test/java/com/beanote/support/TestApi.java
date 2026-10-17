package com.beanote.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

/**
 * 통합 테스트용 API 호출 도우미.
 */
public final class TestApi {

	private final MockMvcTester mvc;

	public TestApi(MockMvcTester mvc) {
		this.mvc = mvc;
	}

	/** 새 사용자로 가입하고 Access 토큰을 돌려준다. */
	public String newUserToken() {
		String email = "user-" + UUID.randomUUID() + "@beanote.test";
		MvcTestResult result = mvc.post().uri("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"coffee1234","nickname":"테스터"}""".formatted(email))
				.exchange();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return read(result, "$.data.accessToken");
	}

	public MvcTestResult post(String uri, String token, String json) {
		var request = mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json);
		return (token == null ? request : request.header("Authorization", "Bearer " + token)).exchange();
	}

	public MvcTestResult put(String uri, String token, String json) {
		var request = mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json);
		return (token == null ? request : request.header("Authorization", "Bearer " + token)).exchange();
	}

	public MvcTestResult get(String uri, String token) {
		var request = mvc.get().uri(uri);
		return (token == null ? request : request.header("Authorization", "Bearer " + token)).exchange();
	}

	public MvcTestResult delete(String uri, String token) {
		var request = mvc.delete().uri(uri);
		return (token == null ? request : request.header("Authorization", "Bearer " + token)).exchange();
	}

	/** 노트를 만들고 id 를 돌려준다. */
	public long createNote(String token, String json) {
		MvcTestResult result = post("/api/v1/notes", token, json);
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return ((Number) JsonPath.read(body(result), "$.data.id")).longValue();
	}

	/** 다른 테스트와 통계가 섞이지 않도록 새 원두를 등록하고 id 를 돌려준다. */
	public long createFreshBean(String token) {
		MvcTestResult result = post("/api/v1/beans", token, """
				{"roasteryId":1,"name":"테스트 원두 %s","blend":false,"originCountry":"ET",
				 "process":"WASHED","roastLevel":"LIGHT","force":true}""".formatted(UUID.randomUUID()));
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return ((Number) JsonPath.read(body(result), "$.data.id")).longValue();
	}

	public static String noteJson(long beanId, String rating, int acidity, String visibility, String flavorTagIds) {
		return """
				{"beanId":%d,"tastedOn":"2026-10-01","brewMethod":"POUR_OVER",
				 "recipe":{"doseG":15,"waterG":240,"waterTempC":92,"grindText":"중간","brewTimeSec":150},
				 "scores":{"acidity":%d,"sweetness":3,"body":3,"bitterness":2,"aftertaste":4},
				 "flavorTagIds":%s,"freeFlavors":["살짝 자두 같은 느낌"],
				 "rating":%s,"wouldBuyAgain":true,"memo":"맛있다","visibility":"%s"}"""
				.formatted(beanId, acidity, flavorTagIds, rating, visibility);
	}

	public static String read(MvcTestResult result, String path) {
		Object value = JsonPath.read(body(result), path);
		return value == null ? null : value.toString();
	}

	public static String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}
