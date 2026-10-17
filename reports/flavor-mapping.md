# 향미 표현 표준화 매핑률 리포트

> `FlavorMappingReportTest` 가 테스트 실행 때마다 다시 만든다. 판정 기준: AUTO ≥ 85점, REVIEW 70~85점, UNMAPPED < 70점.
> 시드 원두와 말뭉치는 직접 만든 예시 데이터이며, 실제 로스터리 설명 전체를 대표하지 않는다.

## 1. 시드 원두 공식 설명

시드 설명은 표준 태그 이름으로 작성했기 때문에 높은 매핑률이 당연하다. 사전·정규화가 깨지지 않았는지 확인하는 회귀 지표로 보고, 실제 판정 성능은 2장 말뭉치로 본다. 말뭉치도 판정기와 함께 만들었으므로 낙관적인 수치일 수 있다.

| 항목 | 값 |
|---|---|
| 원두 수 | 60 |
| 표현 조각 수 | 180 |
| EXACT | 180 |
| AUTO | 0 |
| REVIEW | 0 |
| UNMAPPED | 0 |
| **자동 매핑률** (EXACT+AUTO) | **100.0%** |
| 자동 매핑 태그가 시드 태그와 일치 | 100.0% |

## 2. 평가용 말뭉치 (`src/test/resources/flavor/expression-corpus.tsv`)

| 유형 | 표현 수 | 정답 | 정확도 |
|---|---|---|---|
| variant | 10 | 10 | 100.0% |
| english | 8 | 8 | 100.0% |
| typo | 10 | 10 | 100.0% |
| phrase | 10 | 10 | 100.0% |
| category | 6 | 5 | 83.3% |
| unregistered | 10 | 10 | 100.0% |
| **전체** | 54 | 53 | **98.1%** |

### 표현별 결과

| 표현 | 기대 | 판정 | 점수 | 결과 | 정답 |
|---|---|---|---|---|---|
| 쟈스민 | TAG:JASMINE | EXACT | 100.0 | TAG:JASMINE | O |
| 카라멜 | TAG:CARAMEL | EXACT | 100.0 | TAG:CARAMEL | O |
| 헤즐넛 | TAG:HAZELNUT | EXACT | 100.0 | TAG:HAZELNUT | O |
| 다크 초콜렛 | TAG:DARK_CHOCOLATE | EXACT | 100.0 | TAG:DARK_CHOCOLATE | O |
| 밀크 쵸콜릿 | TAG:MILK_CHOCOLATE | EXACT | 100.0 | TAG:MILK_CHOCOLATE | O |
| 계피 | TAG:CINNAMON | EXACT | 100.0 | TAG:CINNAMON | O |
| 얼그레이 | TAG:BERGAMOT | EXACT | 100.0 | TAG:BERGAMOT | O |
| 카시스 | TAG:BLACKCURRANT | EXACT | 100.0 | TAG:BLACKCURRANT | O |
| 백향과 | TAG:PASSION_FRUIT | EXACT | 100.0 | TAG:PASSION_FRUIT | O |
| 감귤 | TAG:TANGERINE | EXACT | 100.0 | TAG:TANGERINE | O |
| Jasmine | TAG:JASMINE | EXACT | 100.0 | TAG:JASMINE | O |
| Dark Chocolate | TAG:DARK_CHOCOLATE | EXACT | 100.0 | TAG:DARK_CHOCOLATE | O |
| Brown Sugar | TAG:BROWN_SUGAR | EXACT | 100.0 | TAG:BROWN_SUGAR | O |
| Earl Grey | TAG:BERGAMOT | EXACT | 100.0 | TAG:BERGAMOT | O |
| Stone Fruit | CAT:STONE_TROPICAL | EXACT | 100.0 | CAT:STONE_TROPICAL | O |
| Red Wine | TAG:WINE | EXACT | 100.0 | TAG:WINE | O |
| Cacao Nibs | TAG:COCOA | EXACT | 100.0 | TAG:COCOA | O |
| Black Tea | TAG:BLACK_TEA | EXACT | 100.0 | TAG:BLACK_TEA | O |
| 자스밍 | TAG:JASMINE | AUTO | 85.7 | TAG:JASMINE | O |
| 블루베라 | TAG:BLUEBERRY | AUTO | 88.9 | TAG:BLUEBERRY | O |
| 라즈배리 | TAG:RASPBERRY | AUTO | 87.5 | TAG:RASPBERRY | O |
| 캐러맬 | TAG:CARAMEL | AUTO | 85.7 | TAG:CARAMEL | O |
| 헤이즐럿 | TAG:HAZELNUT | AUTO | 90.0 | TAG:HAZELNUT | O |
| 베르가뭇 | TAG:BERGAMOT | AUTO | 88.9 | TAG:BERGAMOT | O |
| jasmin | TAG:JASMINE | AUTO | 85.7 | TAG:JASMINE | O |
| rasberry | TAG:RASPBERRY | AUTO | 88.9 | TAG:RASPBERRY | O |
| carmel | TAG:CARAMEL | AUTO | 85.7 | TAG:CARAMEL | O |
| blackcurrent | TAG:BLACKCURRANT | AUTO | 91.7 | TAG:BLACKCURRANT | O |
| 은은한 자스민향 | TAG:JASMINE | AUTO | 95.0 | TAG:JASMINE | O |
| 살짝 자두 같은 느낌 | TAG:PLUM | AUTO | 95.0 | TAG:PLUM | O |
| 잘 익은 복숭아 | TAG:PEACH | AUTO | 95.0 | TAG:PEACH | O |
| 달콤한 꿀 향 | TAG:HONEY | AUTO | 95.0 | TAG:HONEY | O |
| 진한 다크 초콜릿 느낌 | TAG:DARK_CHOCOLATE | AUTO | 95.0 | TAG:DARK_CHOCOLATE | O |
| 상큼한 레몬 계열 | TAG:LEMON | AUTO | 95.0 | TAG:LEMON | O |
| 구운 빵 같은 | TAG:BAKED_BREAD | AUTO | 95.0 | TAG:BAKED_BREAD | O |
| 고소한 땅콩맛 | TAG:PEANUT | AUTO | 95.0 | TAG:PEANUT | O |
| 블루베리 잼 | TAG:BLUEBERRY | AUTO | 95.0 | TAG:BLUEBERRY | O |
| 와인 같은 산미 | TAG:WINE | AUTO | 95.0 | TAG:WINE | O |
| 플로럴 | CAT:FLORAL | EXACT | 100.0 | CAT:FLORAL | O |
| 베리류 | CAT:BERRY | EXACT | 100.0 | CAT:BERRY | O |
| 시트러스한 | CAT:CITRUS | REVIEW | 80.0 | CAT:CITRUS | **X** |
| 열대과일 느낌 | CAT:STONE_TROPICAL | AUTO | 95.0 | CAT:STONE_TROPICAL | O |
| 고소함 | CAT:NUTTY_COCOA | EXACT | 100.0 | CAT:NUTTY_COCOA | O |
| 스파이시 | CAT:SPICE | EXACT | 100.0 | CAT:SPICE | O |
| 타이어 | NONE | UNMAPPED | 50.0 | NONE | O |
| 고무 | NONE | UNMAPPED | 66.7 | NONE | O |
| 깔끔함 | NONE | UNMAPPED | 66.7 | NONE | O |
| 밸런스 | NONE | UNMAPPED | 46.2 | NONE | O |
| 산뜻함 | NONE | UNMAPPED | 44.4 | NONE | O |
| 바디감 | NONE | UNMAPPED | 44.4 | NONE | O |
| 클린컵 | NONE | UNMAPPED | 44.4 | NONE | O |
| 리치한 바디 | NONE | REVIEW | 80.0 | TAG:LYCHEE | O |
| 멜론 | NONE | UNMAPPED | 50.0 | NONE | O |
| 사과 | NONE | UNMAPPED | 60.0 | NONE | O |

NONE 표현은 표준 태그로 확정(EXACT·AUTO)하지 않으면 정답으로 본다. REVIEW 는 관리자 검토 큐(F-4)로 간다.
