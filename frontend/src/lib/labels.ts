import type {
  Axes,
  BrewMethod,
  CategoryCode,
  CoffeeHabit,
  Confidence,
  ProcessMethod,
  RoastLevel,
} from '../api/types.ts'

export type AxisKey = keyof Axes

/** 맛 축 (3-1). 입문자를 위해 점수마다 쉬운 설명을 함께 보여 준다 */
export const AXES: { key: AxisKey; name: string; levels: [string, string, string, string, string] }[] = [
  {
    key: 'acidity',
    name: '산미',
    levels: [
      '산미가 거의 없어요',
      '은은한 산미',
      '적당히 상큼해요',
      '과일처럼 상큼해요',
      '레몬이나 베리처럼 새콤해요',
    ],
  },
  {
    key: 'sweetness',
    name: '단맛',
    levels: ['단맛이 거의 없어요', '살짝 달아요', '은은하게 달콤해요', '캐러멜처럼 달콤해요', '꿀처럼 단맛이 뚜렷해요'],
  },
  {
    key: 'body',
    name: '바디',
    levels: ['물처럼 가벼워요', '가볍고 산뜻해요', '적당한 무게감', '우유처럼 부드럽고 묵직해요', '크림처럼 진하고 묵직해요'],
  },
  {
    key: 'bitterness',
    name: '쓴맛',
    levels: ['쓴맛이 거의 없어요', '살짝 쌉쌀해요', '적당히 쌉쌀해요', '다크초콜릿처럼 쌉쌀해요', '쓴맛이 강해요'],
  },
  {
    key: 'aftertaste',
    name: '여운',
    levels: ['금방 사라져요', '짧게 남아요', '적당히 남아요', '오래 은은하게 남아요', '길고 진하게 남아요'],
  },
]

export const AXIS_NAMES: Record<AxisKey, string> = Object.fromEntries(AXES.map((a) => [a.key, a.name])) as Record<
  AxisKey,
  string
>

export const PROCESS_NAMES: Record<ProcessMethod, string> = {
  WASHED: '워시드',
  NATURAL: '내추럴',
  HONEY: '허니',
  ANAEROBIC: '무산소',
  OTHER: '기타',
}

export const PROCESS_HINTS: Record<ProcessMethod, string> = {
  WASHED: '깨끗하고 또렷한 맛',
  NATURAL: '과일 향과 단맛이 풍부',
  HONEY: '단맛과 부드러움의 균형',
  ANAEROBIC: '발효 향이 독특하고 화려',
  OTHER: '블렌드·기타 가공',
}

export const ROAST_LEVELS: RoastLevel[] = ['LIGHT', 'MEDIUM_LIGHT', 'MEDIUM', 'MEDIUM_DARK', 'DARK']

export const ROAST_NAMES: Record<RoastLevel, string> = {
  LIGHT: '라이트',
  MEDIUM_LIGHT: '미디엄 라이트',
  MEDIUM: '미디엄',
  MEDIUM_DARK: '미디엄 다크',
  DARK: '다크',
}

export const BREW_METHODS: { value: BrewMethod; name: string }[] = [
  { value: 'POUR_OVER', name: '핸드드립' },
  { value: 'ESPRESSO', name: '에스프레소' },
  { value: 'AEROPRESS', name: '에어로프레스' },
  { value: 'FRENCH_PRESS', name: '프렌치프레스' },
  { value: 'COLD_BREW', name: '콜드브루' },
  { value: 'MOKA_POT', name: '모카포트' },
  { value: 'CAFE', name: '카페에서' },
  { value: 'OTHER', name: '기타' },
]

export const BREW_NAMES = Object.fromEntries(BREW_METHODS.map((b) => [b.value, b.name])) as Record<BrewMethod, string>

export const HABITS: { value: CoffeeHabit; name: string; hint: string }[] = [
  { value: 'AMERICANO', name: '아메리카노', hint: '카페에서 가장 자주 마셔요' },
  { value: 'LATTE', name: '라떼', hint: '우유와 함께 부드럽게' },
  { value: 'POUR_OVER', name: '핸드드립', hint: '원두의 향을 즐겨요' },
  { value: 'ESPRESSO', name: '에스프레소', hint: '진하고 짧게' },
]

export const CONFIDENCE: Record<Confidence, { label: string; tone: 'ink' | 'crema' | 'good' }> = {
  NONE: { label: '취향 탐색 전', tone: 'ink' },
  SURVEY: { label: '설문 기반', tone: 'crema' },
  FORMING: { label: '취향 형성 중', tone: 'crema' },
  STABLE: { label: '취향 안정', tone: 'good' },
}

/** 향미 대분류 → CSS 색 변수 */
export function flavorColor(code: CategoryCode | string): string {
  return `var(--flavor-${code.toLowerCase().replaceAll('_', '-')})`
}

export const CATEGORY_HINTS: Record<CategoryCode, string> = {
  FLORAL: '자스민·장미·홍차',
  BERRY: '블루베리·딸기·라즈베리',
  CITRUS: '레몬·오렌지·자몽',
  STONE_TROPICAL: '복숭아·체리·망고',
  SWEET: '캐러멜·꿀·흑설탕',
  NUTTY_COCOA: '견과·초콜릿·코코아',
  SPICE: '시나몬·후추',
  ROASTED: '구운 빵·스모키',
  FERMENTED: '와인·럼·발효과일',
  GREEN: '허브·녹차',
}
