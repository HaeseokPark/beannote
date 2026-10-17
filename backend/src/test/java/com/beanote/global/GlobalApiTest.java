package com.beanote.global;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.beanote.global.error.BusinessException;
import com.beanote.global.error.ErrorCode;
import com.beanote.global.logging.RequestIdFilter;
import com.beanote.global.response.ApiResponse;
import com.beanote.support.IntegrationTest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@IntegrationTest
@Import(GlobalApiTest.TestOnlyController.class)
class GlobalApiTest {

	@Autowired
	MockMvcTester mvc;

	@Test
	void healthIsUp() {
		assertThat(mvc.get().uri("/actuator/health"))
				.hasStatusOk()
				.bodyJson().extractingPath("$.status").isEqualTo("UP");
	}

	@Test
	void successResponseHasCommonShape() {
		assertThat(mvc.get().uri("/test-only/echo/hello"))
				.hasStatusOk()
				.bodyJson()
				.isLenientlyEqualTo("""
						{ "success": true, "data": "hello", "error": null }
						""");
	}

	@Test
	void businessExceptionMapsToErrorCode() {
		assertThat(mvc.get().uri("/test-only/forbidden"))
				.hasStatus(HttpStatus.FORBIDDEN)
				.bodyJson()
				.isLenientlyEqualTo("""
						{ "success": false, "data": null, "error": { "code": "FORBIDDEN" } }
						""");
	}

	@Test
	void validationFailureReturnsInvalidInput() {
		assertThat(mvc.post().uri("/test-only/validate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "name": "", "score": 9 }
						"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
	}

	@Test
	void malformedJsonReturnsInvalidInput() {
		assertThat(mvc.post().uri("/test-only/validate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ not json"))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
	}

	@Test
	void unknownPathReturnsNotFoundInCommonShape() {
		assertThat(mvc.get().uri("/api/v1/does-not-exist"))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson().extractingPath("$.error.code").isEqualTo("NOT_FOUND");
	}

	@Test
	void requestIdIsGeneratedOrPropagated() {
		assertThat(mvc.get().uri("/actuator/health"))
				.headers().containsHeader(RequestIdFilter.HEADER);

		assertThat(mvc.get().uri("/actuator/health").header(RequestIdFilter.HEADER, "abc-123"))
				.headers().hasValue(RequestIdFilter.HEADER, "abc-123");

		String replaced = mvc.get().uri("/actuator/health").header(RequestIdFilter.HEADER, "bad id\nvalue")
				.exchange().getResponse().getHeader(RequestIdFilter.HEADER);
		assertThat(replaced).isNotEqualTo("bad id\nvalue").matches("[0-9a-f-]{36}");
	}

	@Test
	void openApiDocsAreServed() {
		assertThat(mvc.get().uri("/v3/api-docs"))
				.hasStatusOk()
				.bodyJson().extractingPath("$.info.title").isEqualTo("Beanote API");
	}

	record ValidateRequest(@NotBlank String name, @Min(1) @Max(5) int score) {
	}

	@RestController
	static class TestOnlyController {

		@GetMapping("/test-only/echo/{value}")
		ApiResponse<String> echo(@PathVariable String value) {
			return ApiResponse.ok(value);
		}

		@GetMapping("/test-only/forbidden")
		ApiResponse<Void> forbidden() {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		@PostMapping("/test-only/validate")
		ApiResponse<Void> validate(@Valid @RequestBody ValidateRequest request) {
			return ApiResponse.ok();
		}

	}

}
