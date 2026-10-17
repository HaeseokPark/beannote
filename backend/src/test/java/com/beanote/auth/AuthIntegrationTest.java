package com.beanote.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.beanote.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;

@IntegrationTest
class AuthIntegrationTest {

	private static final String PASSWORD = "coffee1234";

	@Autowired
	MockMvcTester mvc;

	@Autowired
	JwtEncoder jwtEncoder;

	@Test
	void signupReturnsTokensAndUser() {
		String email = uniqueEmail();

		MvcTestResult result = signup(email, PASSWORD, "민수");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.data.user.email").isEqualTo(email);
		assertThat(result).bodyJson().extractingPath("$.data.user.nickname").isEqualTo("민수");
		assertThat(result).bodyJson().extractingPath("$.data.accessTokenExpiresIn").isEqualTo(1800);
		assertThat(read(result, "$.data.accessToken")).isNotBlank();
		assertThat(read(result, "$.data.refreshToken")).isNotBlank();
	}

	@Test
	void signupNormalizesEmailAndRejectsDuplicate() {
		String email = uniqueEmail();
		assertThat(signup(email, PASSWORD, "민수")).hasStatus(HttpStatus.CREATED);

		assertThat(signup(email.toUpperCase(), PASSWORD, "다른사람"))
				.hasStatus(HttpStatus.CONFLICT)
				.bodyJson().extractingPath("$.error.code").isEqualTo("EMAIL_DUPLICATED");
	}

	@Test
	void signupRejectsWeakPasswordAndBadEmail() {
		assertThat(signup(uniqueEmail(), "onlyletters", "민수"))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		assertThat(signup(uniqueEmail(), "short1", "민수")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(signup("not-an-email", PASSWORD, "민수")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(signup(uniqueEmail(), PASSWORD, "민")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void loginSucceedsWithCorrectPasswordOnly() {
		String email = uniqueEmail();
		signup(email, PASSWORD, "지현");

		assertThat(login(email, PASSWORD)).hasStatusOk()
				.bodyJson().extractingPath("$.data.user.email").isEqualTo(email);

		assertThat(login(email, "wrong12345"))
				.hasStatus(HttpStatus.UNAUTHORIZED)
				.bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_CREDENTIALS");

		// 없는 계정도 같은 에러 코드 (계정 존재 여부를 드러내지 않음)
		assertThat(login(uniqueEmail(), PASSWORD))
				.hasStatus(HttpStatus.UNAUTHORIZED)
				.bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_CREDENTIALS");
	}

	@Test
	void meRequiresValidAccessToken() {
		String email = uniqueEmail();
		String accessToken = read(signup(email, PASSWORD, "서연"), "$.data.accessToken");

		assertThat(mvc.get().uri("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
				.hasStatusOk()
				.bodyJson().extractingPath("$.data.email").isEqualTo(email);

		assertThat(mvc.get().uri("/api/v1/users/me"))
				.hasStatus(HttpStatus.UNAUTHORIZED)
				.bodyJson().extractingPath("$.error.code").isEqualTo("UNAUTHORIZED");

		assertThat(mvc.get().uri("/api/v1/users/me").header("Authorization", "Bearer not.a.jwt"))
				.hasStatus(HttpStatus.UNAUTHORIZED)
				.bodyJson().extractingPath("$.error.code").isEqualTo("UNAUTHORIZED");
	}

	@Test
	void expiredAccessTokenReturnsTokenExpired() {
		Instant past = Instant.now().minusSeconds(3600);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("beanote").subject("1").issuedAt(past.minusSeconds(1800)).expiresAt(past)
				.claim("role", "USER").build();
		String expired = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
				claims)).getTokenValue();

		assertThat(mvc.get().uri("/api/v1/users/me").header("Authorization", "Bearer " + expired))
				.hasStatus(HttpStatus.UNAUTHORIZED)
				.bodyJson().extractingPath("$.error.code").isEqualTo("TOKEN_EXPIRED");
	}

	@Test
	void refreshRotatesTokenAndDetectsReuse() {
		String firstRefresh = read(signup(uniqueEmail(), PASSWORD, "민수"), "$.data.refreshToken");

		MvcTestResult refreshed = refresh(firstRefresh);
		assertThat(refreshed).hasStatusOk();
		String secondRefresh = read(refreshed, "$.data.refreshToken");
		assertThat(secondRefresh).isNotEqualTo(firstRefresh);

		// 이미 교체된 토큰을 다시 쓰면 거부하고, 그 사용자의 토큰을 모두 폐기한다
		assertThat(refresh(firstRefresh)).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(refresh(secondRefresh)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void refreshWithUnknownTokenFails() {
		assertThat(refresh("unknown-token"))
				.hasStatus(HttpStatus.UNAUTHORIZED)
				.bodyJson().extractingPath("$.error.code").isEqualTo("UNAUTHORIZED");
	}

	@Test
	void logoutRevokesRefreshToken() {
		MvcTestResult signup = signup(uniqueEmail(), PASSWORD, "지현");
		String accessToken = read(signup, "$.data.accessToken");
		String refreshToken = read(signup, "$.data.refreshToken");

		assertThat(mvc.post().uri("/api/v1/auth/logout")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("refreshToken", refreshToken))))
				.hasStatusOk();

		assertThat(refresh(refreshToken)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void logoutRequiresAuthentication() {
		assertThat(mvc.post().uri("/api/v1/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("refreshToken", "x"))))
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private MvcTestResult signup(String email, String password, String nickname) {
		return mvc.post().uri("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("email", email, "password", password, "nickname", nickname)))
				.exchange();
	}

	private MvcTestResult login(String email, String password) {
		return mvc.post().uri("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("email", email, "password", password)))
				.exchange();
	}

	private MvcTestResult refresh(String refreshToken) {
		return mvc.post().uri("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("refreshToken", refreshToken)))
				.exchange();
	}

	private static String read(MvcTestResult result, String path) {
		try {
			return JsonPath.read(result.getResponse().getContentAsString(), path);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static String json(Map<String, String> fields) {
		StringBuilder sb = new StringBuilder("{");
		fields.forEach((k, v) -> sb.append(sb.length() > 1 ? "," : "").append('"').append(k).append("\":\"")
				.append(v.replace("\"", "\\\"")).append('"'));
		return sb.append('}').toString();
	}

	private static String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@beanote.test";
	}

}
