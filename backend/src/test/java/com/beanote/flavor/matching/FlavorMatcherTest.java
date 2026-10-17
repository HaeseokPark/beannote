package com.beanote.flavor.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.beanote.flavor.domain.MappingResult;

/**
 * 향미 판정기 단위 테스트 (Phase 3 완료 조건: 표기 변형·오타·미등록 표현).
 */
class FlavorMatcherTest {

	private static final long JASMINE = 1, BLUEBERRY = 2, DARK_CHOCOLATE = 3, HONEY = 4, LYCHEE = 5, PLUM = 6,
			RASPBERRY = 7, CARAMEL = 8;
	private static final long FLORAL = 101, NUTTY_COCOA = 106;

	private final FlavorMatcher matcher = new FlavorMatcher(List.of(
			DictionaryEntry.tag("자스민", JASMINE), DictionaryEntry.tag("쟈스민", JASMINE),
			DictionaryEntry.tag("jasmine", JASMINE),
			DictionaryEntry.tag("블루베리", BLUEBERRY), DictionaryEntry.tag("blueberry", BLUEBERRY),
			DictionaryEntry.tag("다크초콜릿", DARK_CHOCOLATE), DictionaryEntry.tag("darkchocolate", DARK_CHOCOLATE),
			DictionaryEntry.tag("꿀", HONEY), DictionaryEntry.tag("honey", HONEY),
			DictionaryEntry.tag("리치", LYCHEE),
			DictionaryEntry.tag("자두", PLUM),
			DictionaryEntry.tag("라즈베리", RASPBERRY), DictionaryEntry.tag("raspberry", RASPBERRY),
			DictionaryEntry.tag("캐러멜", CARAMEL), DictionaryEntry.tag("카라멜", CARAMEL),
			DictionaryEntry.category("플로럴", FLORAL), DictionaryEntry.category("floral", FLORAL),
			DictionaryEntry.category("초콜릿", NUTTY_COCOA)),
			85, 70);

	@Nested
	class 사전_일치 {

		@Test
		void 정규화하면_사전과_같은_표현은_EXACT() {
			assertMatch("자스민", MappingResult.EXACT, JASMINE);
			assertMatch("JASMINE", MappingResult.EXACT, JASMINE);
			assertMatch(" Dark Chocolate ", MappingResult.EXACT, DARK_CHOCOLATE);
			assertMatch("다크 초콜릿", MappingResult.EXACT, DARK_CHOCOLATE);
			// 전각 문자는 NFKC 로 반각이 된다
			assertMatch("ＨＯＮＥＹ", MappingResult.EXACT, HONEY);
		}

		@Test
		void 표기_변형도_사전에_있으면_EXACT() {
			assertMatch("쟈스민", MappingResult.EXACT, JASMINE);
			assertMatch("카라멜", MappingResult.EXACT, CARAMEL);
		}

		@Test
		void 대분류_표현은_대분류로_매핑() {
			FlavorMatch match = matcher.match("플로럴");
			assertThat(match.result()).isEqualTo(MappingResult.EXACT);
			assertThat(match.entry().categoryId()).isEqualTo(FLORAL);
			assertThat(match.entry().tagId()).isNull();
		}

	}

	@Nested
	class 문장_속_표현 {

		@Test
		void 꼬리말과_수식어를_떼고_찾으면_AUTO() {
			assertMatch("살짝 자두 같은 느낌", MappingResult.AUTO, PLUM);
			assertMatch("자스민향", MappingResult.AUTO, JASMINE);
			assertMatch("은은한 꿀 향", MappingResult.AUTO, HONEY);
			assertMatch("꿀같은", MappingResult.AUTO, HONEY);
		}

		@Test
		void 긴_표현을_먼저_찾는다() {
			// "초콜릿"(대분류)보다 "다크 초콜릿"(소분류)이 우선
			assertMatch("진한 다크 초콜릿 느낌", MappingResult.AUTO, DARK_CHOCOLATE);
		}

		@Test
		void 단어_안에_들어있기만_하면_검토_대상() {
			// "리치한"은 과일 리치가 아니라 "rich" 일 수 있다
			FlavorMatch match = matcher.match("리치한");
			assertThat(match.result()).isEqualTo(MappingResult.REVIEW);
			assertThat(match.entry().tagId()).isEqualTo(LYCHEE);
		}

	}

	@Nested
	class 오타 {

		@Test
		void 자모_하나_다른_오타는_AUTO() {
			assertMatch("자스밍", MappingResult.AUTO, JASMINE);
			assertMatch("블루베라", MappingResult.AUTO, BLUEBERRY);
			assertMatch("jasmin", MappingResult.AUTO, JASMINE);
			assertMatch("rasberry", MappingResult.AUTO, RASPBERRY);
			assertMatch("라스베리", MappingResult.AUTO, RASPBERRY);
		}

		@Test
		void 여러_군데_틀리면_검토_대상() {
			// 자모 두 군데 차이 (ㅈ→ㅅ, ㄹ 추가)
			FlavorMatch match = matcher.match("라스벨리");
			assertThat(match.result()).isEqualTo(MappingResult.REVIEW);
			assertThat(match.entry().tagId()).isEqualTo(RASPBERRY);
			assertThat(match.score()).isBetween(70.0, 85.0);
		}

	}

	@Nested
	class 미등록 {

		@Test
		void 사전과_거리가_먼_표현은_UNMAPPED() {
			for (String text : List.of("타이어", "고무", "xyz", "깔끔함")) {
				FlavorMatch match = matcher.match(text);
				assertThat(match.result()).as(text).isEqualTo(MappingResult.UNMAPPED);
				assertThat(match.entry()).as(text).isNull();
			}
		}

		@Test
		void 문장부호뿐인_입력은_UNMAPPED() {
			assertThat(matcher.match("!!!").result()).isEqualTo(MappingResult.UNMAPPED);
		}

	}

	@Nested
	class 여러_표현 {

		@Test
		void 구분자로_나눠서_각각_판정() {
			List<FlavorMatch> matches = matcher.matchAll("자스민, 블루베리 / 꿀 · 타이어");
			assertThat(matches).extracting(FlavorMatch::raw).containsExactly("자스민", "블루베리", "꿀", "타이어");
			assertThat(matches).extracting(FlavorMatch::result).containsExactly(MappingResult.EXACT,
					MappingResult.EXACT, MappingResult.EXACT, MappingResult.UNMAPPED);
		}

		@Test
		void 빈_조각은_건너뛴다() {
			assertThat(FlavorMatcher.splitSegments("자스민,, ,꿀")).containsExactly("자스민", "꿀");
			assertThat(FlavorMatcher.splitSegments(null)).isEmpty();
		}

	}

	@Test
	void 자모_분해() {
		assertThat(HangulJamo.decompose("자스민")).isEqualTo("ㅈㅏㅅㅡㅁㅣㄴ");
		assertThat(HangulJamo.decompose("꿀a")).isEqualTo("ㄲㅜㄹa");
	}

	private void assertMatch(String text, MappingResult expected, long tagId) {
		FlavorMatch match = matcher.match(text);
		assertThat(match.result()).as("%s → %s (score %.1f)", text, match.result(), match.score()).isEqualTo(expected);
		assertThat(match.entry()).as(text).isNotNull();
		assertThat(match.entry().tagId()).as(text).isEqualTo(tagId);
	}

}
