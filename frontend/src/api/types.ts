// 백엔드 응답 타입 (backend/src/main/java/com/beanote/**/dto)

export type CursorPage<T> = { items: T[]; nextCursor: string | null }

export type User = { id: number; email: string; nickname: string; profileImageUrl: string | null; role: string }

export type TokenResponse = { accessToken: string; accessTokenExpiresIn: number; refreshToken: string; user: User }

// ---- 향미 ----

export type CategoryCode =
  | 'FLORAL'
  | 'BERRY'
  | 'CITRUS'
  | 'STONE_TROPICAL'
  | 'SWEET'
  | 'NUTTY_COCOA'
  | 'SPICE'
  | 'ROASTED'
  | 'FERMENTED'
  | 'GREEN'

export type FlavorTag = { id: number; code: string; name: string; nameEn: string }
export type FlavorCategory = { code: CategoryCode; name: string; tags: FlavorTag[] }
export type FlavorLabel = { id: number; code: string; name: string; categoryCode: CategoryCode }

export type MappingResult = 'EXACT' | 'AUTO' | 'REVIEW' | 'UNMAPPED'
export type FlavorMatch = {
  raw: string
  normalized: string
  result: MappingResult
  score: number
  tag: FlavorLabel | null
  category: { code: CategoryCode; name: string } | null
}

// ---- 원두 ----

export type ProcessMethod = 'WASHED' | 'NATURAL' | 'HONEY' | 'ANAEROBIC' | 'OTHER'
export type RoastLevel = 'LIGHT' | 'MEDIUM_LIGHT' | 'MEDIUM' | 'MEDIUM_DARK' | 'DARK'
export type BeanSort = 'LATEST' | 'NAME' | 'POPULAR' | 'RATING'

export type Origin = { code: string; name: string }
export type Axes = { acidity: number; sweetness: number; body: number; bitterness: number; aftertaste: number }

export type BeanSummary = {
  id: number
  name: string
  roastery: { id: number; name: string }
  origins: Origin[]
  region: string | null
  process: ProcessMethod
  roastLevel: RoastLevel
  blend: boolean
  status: 'ACTIVE' | 'PENDING' | 'DISCONTINUED'
  seed: boolean
  flavors: FlavorLabel[]
  noteCount: number
  avgRating: number | null
}

export type BeanDetail = Omit<BeanSummary, 'noteCount' | 'avgRating'> & {
  farm: string | null
  varieties: string[]
  roastedOn: string | null
  officialFlavorText: string | null
  stats: { noteCount: number; avgRating: number | null; axes: Axes | null }
  createdAt: string
}

export type Roastery = { id: number; name: string; region: string | null; websiteUrl: string | null }
export type DuplicateCandidate = { id: number; name: string; roasteryName: string }

export type BeanCreateRequest = {
  roasteryId?: number
  roasteryName?: string
  name: string
  blend: boolean
  originCountry?: string
  blendOrigins?: string[]
  region?: string
  farm?: string
  varieties?: string[]
  process: ProcessMethod
  roastLevel: RoastLevel
  officialFlavorText?: string
  flavorTagIds?: number[]
  force: boolean
}

// ---- 노트 ----

export type BrewMethod =
  | 'POUR_OVER'
  | 'ESPRESSO'
  | 'AEROPRESS'
  | 'FRENCH_PRESS'
  | 'COLD_BREW'
  | 'MOKA_POT'
  | 'CAFE'
  | 'OTHER'
export type Visibility = 'PRIVATE' | 'PUBLIC'

export type Recipe = {
  doseG: number | null
  waterG: number | null
  waterTempC: number | null
  grindText: string | null
  brewTimeSec: number | null
}

export type NoteRequest = {
  beanId: number
  tastedOn: string
  brewMethod: BrewMethod
  recipe: Partial<Recipe> | null
  scores: Axes
  flavorTagIds: number[]
  freeFlavors: string[]
  rating: number
  wouldBuyAgain: boolean | null
  memo: string | null
  visibility: Visibility
}

export type NoteSummary = {
  id: number
  bean: { id: number; name: string; roasteryName: string }
  tastedOn: string
  brewMethod: BrewMethod
  scores: Axes
  rating: number
  flavors: FlavorLabel[]
  visibility: Visibility
  createdAt: string
}

export type NoteDetail = NoteSummary & {
  author: { id: number; nickname: string }
  mine: boolean
  recipe: Recipe | null
  freeFlavors: FlavorMatch[]
  wouldBuyAgain: boolean | null
  memo: string | null
  photoUrl: string | null
  updatedAt: string
}

// ---- 설문·취향 ----

export type CoffeeHabit = 'AMERICANO' | 'LATTE' | 'POUR_OVER' | 'ESPRESSO'
export type Confidence = 'NONE' | 'SURVEY' | 'FORMING' | 'STABLE'

export type SurveyRequest = {
  coffeeHabit: CoffeeHabit
  preferences: Axes
  likedCategories: CategoryCode[]
  dislikedCategories: CategoryCode[]
}

export type SurveyResponse = {
  coffeeHabit: CoffeeHabit
  preferences: Axes
  likedCategories: { code: CategoryCode; name: string }[]
  dislikedCategories: { code: CategoryCode; name: string }[]
}

export type AttributeRating = { code: string; name: string; avgRating: number; noteCount: number }

export type TasteProfile = {
  confidence: Confidence
  noteCount: number
  notesUntilStable: number
  radar: Axes | null
  radarSource: 'NOTES' | 'SURVEY' | 'NONE'
  likedFlavors: { code: string; name: string; categoryCode: CategoryCode; score: number }[]
  dislikedFlavors: { code: string; name: string; categoryCode: CategoryCode; score: number }[]
  likedCategories: { code: CategoryCode; name: string; score: number; fromSurvey: boolean }[]
  dislikedCategories: { code: CategoryCode; name: string; score: number; fromSurvey: boolean }[]
  processes: AttributeRating[]
  origins: AttributeRating[]
  roastLevels: AttributeRating[]
}

// ---- 추천 ----

export type ReasonType = 'FLAVOR' | 'TASTE_AXIS' | 'ROAST' | 'PROCESS' | 'EXPLORE' | 'COLLAB' | 'POPULAR' | 'PROFILE'

export type Recommendation = {
  bean: BeanSummary
  score: number | null
  reasons: { type: ReasonType; text: string }[]
  isExploration: boolean
}

export type RecommendationResponse = {
  confidence: Confidence
  basedOnNotes: number
  popular: boolean
  items: Recommendation[]
}

export type SimilarBean = { bean: BeanSummary; similarity: number }
