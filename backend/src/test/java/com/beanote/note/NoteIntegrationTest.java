package com.beanote.note;

import static com.beanote.support.TestApi.noteJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.beanote.support.IntegrationTest;
import com.beanote.support.TestApi;
import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManagerFactory;

/**
 * 시음 노트 CRUD·검증·권한 (N-1 ~ N-4).
 */
@IntegrationTest
class NoteIntegrationTest {

	@Autowired
	MockMvcTester mvc;

	@Autowired
	EntityManagerFactory entityManagerFactory;

	@Autowired
	JdbcTemplate jdbc;

	TestApi api;
	String owner;
	String other;

	@BeforeEach
	void setUp() {
		api = new TestApi(mvc);
		owner = api.newUserToken();
		other = api.newUserToken();
	}

	@Test
	void createReturnsFullNote() {
		MvcTestResult result = api.post("/api/v1/notes", owner, noteJson(1, "4.5", 5, "PRIVATE", "[1, 2]"));

		assertThat(result).hasStatus(HttpStatus.CREATED);
		var body = assertThat(result).bodyJson();
		body.extractingPath("$.data.bean.name").isEqualTo("에티오피아 예가체프 코체레 G1");
		body.extractingPath("$.data.rating").isEqualTo(4.5);
		body.extractingPath("$.data.scores.acidity").isEqualTo(5);
		body.extractingPath("$.data.recipe.waterTempC").isEqualTo(92);
		body.extractingPath("$.data.flavors").asArray().hasSize(2);
		body.extractingPath("$.data.freeFlavors[0]").isEqualTo("살짝 자두 같은 느낌");
		body.extractingPath("$.data.visibility").isEqualTo("PRIVATE");
		body.extractingPath("$.data.mine").isEqualTo(true);
	}

	@Test
	void ratingMustBeHalfStepBetweenHalfAndFive() {
		for (String rating : List.of("4.3", "5.5", "0", "0.0", "-1")) {
			assertThat(api.post("/api/v1/notes", owner, noteJson(1, rating, 3, "PRIVATE", "[]")))
					.as("rating %s", rating)
					.hasStatus(HttpStatus.BAD_REQUEST)
					.bodyJson().extractingPath("$.error.code").isEqualTo("RATING_OUT_OF_RANGE");
		}
		for (String rating : List.of("0.5", "3", "3.50", "5.0")) {
			assertThat(api.post("/api/v1/notes", owner, noteJson(1, rating, 3, "PRIVATE", "[]")))
					.as("rating %s", rating)
					.hasStatus(HttpStatus.CREATED);
		}
	}

	@Test
	void atMostFiveFlavorTags() {
		assertThat(api.post("/api/v1/notes", owner, noteJson(1, "4", 3, "PRIVATE", "[1,2,3,4,5,6]")))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson().extractingPath("$.error.code").isEqualTo("FLAVOR_TAG_LIMIT_EXCEEDED");

		// 중복 id 는 하나로 합쳐서 센다
		assertThat(api.post("/api/v1/notes", owner, noteJson(1, "4", 3, "PRIVATE", "[1,1,2,3,4,5]")))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson().extractingPath("$.data.flavors").asArray().hasSize(5);
	}

	@Test
	void rejectsInvalidAxesUnknownBeanAndFutureDate() {
		assertThat(api.post("/api/v1/notes", owner, noteJson(1, "4", 6, "PRIVATE", "[]")))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		assertThat(api.post("/api/v1/notes", owner, noteJson(999999, "4", 3, "PRIVATE", "[]")))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson().extractingPath("$.error.code").isEqualTo("BEAN_NOT_FOUND");
		assertThat(api.post("/api/v1/notes", owner,
				noteJson(1, "4", 3, "PRIVATE", "[]").replace("2026-10-01", "2099-01-01")))
				.hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(api.post("/api/v1/notes", owner, noteJson(1, "4", 3, "PRIVATE", "[999999]")))
				.hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(api.post("/api/v1/notes", null, noteJson(1, "4", 3, "PRIVATE", "[]")))
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	// ---- 권한 규칙 고정 (N-4, 7-3) ----

	@Test
	void privateNoteIsInvisibleToEveryoneButOwnerAndLooksLikeMissingNote() {
		long noteId = api.createNote(owner, noteJson(1, "4", 3, "PRIVATE", "[]"));

		assertThat(api.get("/api/v1/notes/" + noteId, owner)).hasStatusOk();

		MvcTestResult byOther = api.get("/api/v1/notes/" + noteId, other);
		MvcTestResult anonymous = api.get("/api/v1/notes/" + noteId, null);
		MvcTestResult missing = api.get("/api/v1/notes/999999999", other);
		for (MvcTestResult result : List.of(byOther, anonymous, missing)) {
			assertThat(result).hasStatus(HttpStatus.NOT_FOUND)
					.bodyJson().extractingPath("$.error.code").isEqualTo("NOTE_NOT_FOUND");
		}
		// 비공개 노트와 없는 노트의 응답 본문이 같아야 존재 여부가 드러나지 않는다
		assertThat(TestApi.body(byOther)).isEqualTo(TestApi.body(missing));
	}

	@Test
	void publicNoteIsVisibleToOthersAndAnonymous() {
		long noteId = api.createNote(owner, noteJson(1, "4", 3, "PUBLIC", "[]"));

		assertThat(api.get("/api/v1/notes/" + noteId, other)).hasStatusOk()
				.bodyJson().extractingPath("$.data.mine").isEqualTo(false);
		assertThat(api.get("/api/v1/notes/" + noteId, null)).hasStatusOk()
				.bodyJson().extractingPath("$.data.author.nickname").isEqualTo("테스터");
	}

	@Test
	void onlyOwnerCanUpdateOrDeleteEvenPublicNotes() {
		long noteId = api.createNote(owner, noteJson(1, "4", 3, "PUBLIC", "[]"));

		assertThat(api.put("/api/v1/notes/" + noteId, other, noteJson(1, "1", 1, "PUBLIC", "[]")))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson().extractingPath("$.error.code").isEqualTo("NOTE_NOT_FOUND");
		assertThat(api.delete("/api/v1/notes/" + noteId, other))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson().extractingPath("$.error.code").isEqualTo("NOTE_NOT_FOUND");
		assertThat(api.delete("/api/v1/notes/" + noteId, null)).hasStatus(HttpStatus.UNAUTHORIZED);

		// 바뀌지 않았다
		assertThat(api.get("/api/v1/notes/" + noteId, owner)).hasStatusOk()
				.bodyJson().extractingPath("$.data.rating").isEqualTo(4.0);
	}

	@Test
	void myNotesNeverIncludeOthersNotes() {
		api.createNote(other, noteJson(1, "4", 3, "PUBLIC", "[]"));
		long mine = api.createNote(owner, noteJson(2, "4", 3, "PRIVATE", "[]"));

		assertThat(api.get("/api/v1/notes/me", owner)).hasStatusOk()
				.bodyJson().extractingPath("$.data.items[*].id").asArray().containsExactly((int) mine);
		assertThat(api.get("/api/v1/notes/me", null)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	// ---- 수정·삭제·목록 ----

	@Test
	void ownerCanUpdateAndDelete() {
		long noteId = api.createNote(owner, noteJson(1, "4", 3, "PRIVATE", "[1]"));

		assertThat(api.put("/api/v1/notes/" + noteId, owner, noteJson(2, "2.5", 1, "PUBLIC", "[3,4]")))
				.hasStatusOk()
				.bodyJson()
				.isLenientlyEqualTo("""
						{"data":{"bean":{"id":2},"rating":2.5,"scores":{"acidity":1},"visibility":"PUBLIC"}}
						""");

		assertThat(api.delete("/api/v1/notes/" + noteId, owner)).hasStatusOk();
		assertThat(api.get("/api/v1/notes/" + noteId, owner)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void myNotesAreNewestFirstWithCursorAndFilters() {
		long a = api.createNote(owner, noteJson(1, "4.5", 3, "PRIVATE", "[" + tagId("BLUEBERRY") + "]"));
		long b = api.createNote(owner, noteJson(2, "3", 3, "PRIVATE", "[" + tagId("LEMON") + "]"));
		long c = api.createNote(owner, noteJson(1, "5", 3, "PUBLIC", "[" + tagId("STRAWBERRY") + "]"));

		String first = TestApi.body(api.get("/api/v1/notes/me?size=2", owner));
		assertThat(JsonPath.<List<Integer>>read(first, "$.data.items[*].id")).containsExactly((int) c, (int) b);
		String cursor = JsonPath.read(first, "$.data.nextCursor");
		String second = TestApi.body(api.get("/api/v1/notes/me?size=2&cursor=" + cursor, owner));
		assertThat(JsonPath.<List<Integer>>read(second, "$.data.items[*].id")).containsExactly((int) a);
		assertThat((Object) JsonPath.read(second, "$.data.nextCursor")).isNull();

		assertThat(api.get("/api/v1/notes/me?beanId=1", owner)).bodyJson()
				.extractingPath("$.data.items[*].id").asArray().containsExactly((int) c, (int) a);
		assertThat(api.get("/api/v1/notes/me?minRating=4.5", owner)).bodyJson()
				.extractingPath("$.data.items[*].id").asArray().containsExactly((int) c, (int) a);
		assertThat(api.get("/api/v1/notes/me?flavorCategory=CITRUS", owner)).bodyJson()
				.extractingPath("$.data.items[*].id").asArray().containsExactly((int) b);
	}

	@Test
	void myNotesQueryCountDoesNotGrowWithPageSize() {
		for (int i = 0; i < 8; i++) {
			// 원두·향미를 섞어 지연 로딩 대상이 여러 개가 되게 한다
			api.createNote(owner, noteJson(1 + i, "4", 3, "PRIVATE", "[" + (1 + i) + "," + (10 + i) + "]"));
		}
		long small = statementsFor("/api/v1/notes/me?size=2");
		long large = statementsFor("/api/v1/notes/me?size=8");
		assertThat(large).isEqualTo(small);
	}

	private long tagId(String code) {
		return jdbc.queryForObject("SELECT id FROM flavor_tags WHERE code = ?", Long.class, code);
	}

	private long statementsFor(String uri) {
		Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		stats.clear();
		assertThat(api.get(uri, owner)).hasStatusOk();
		return stats.getPrepareStatementCount();
	}


}
