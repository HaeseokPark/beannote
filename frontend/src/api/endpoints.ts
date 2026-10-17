import { api } from './client.ts'
import type {
  BeanCreateRequest,
  BeanDetail,
  BeanSort,
  BeanSummary,
  CategoryCode,
  CursorPage,
  DuplicateCandidate,
  FlavorCategory,
  FlavorMatch,
  NoteDetail,
  NoteRequest,
  NoteSummary,
  ProcessMethod,
  RecommendationResponse,
  RoastLevel,
  Roastery,
  SimilarBean,
  SurveyRequest,
  SurveyResponse,
  TasteProfile,
  TokenResponse,
  User,
} from './types.ts'

function query(params: Record<string, string | number | undefined | null>) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  })
  const text = search.toString()
  return text ? `?${text}` : ''
}

export const authApi = {
  signup: (body: { email: string; password: string; nickname: string }) =>
    api<TokenResponse>('/auth/signup', { method: 'POST', body, auth: false }),
  login: (body: { email: string; password: string }) =>
    api<TokenResponse>('/auth/login', { method: 'POST', body, auth: false }),
  logout: (refreshToken: string) => api<void>('/auth/logout', { method: 'POST', body: { refreshToken } }),
  me: () => api<User>('/users/me'),
}

export const flavorApi = {
  all: () => api<FlavorCategory[]>('/flavors', { auth: false }),
  normalize: (text: string) => api<FlavorMatch[]>('/flavors/normalize', { method: 'POST', body: { text } }),
}

export type BeanSearch = {
  q?: string
  origin?: string
  process?: ProcessMethod
  roastLevel?: RoastLevel
  flavorCategory?: CategoryCode
  sort?: BeanSort
  cursor?: string | null
  size?: number
}

export const beanApi = {
  search: (params: BeanSearch) => api<CursorPage<BeanSummary>>(`/beans${query(params)}`),
  detail: (id: number) => api<BeanDetail>(`/beans/${id}`),
  similar: (id: number) => api<SimilarBean[]>(`/beans/${id}/similar`),
  duplicates: (params: { roasteryId?: number; roasteryName?: string; name: string }) =>
    api<DuplicateCandidate[]>(`/beans/duplicates${query(params)}`),
  create: (body: BeanCreateRequest) => api<BeanDetail>('/beans', { method: 'POST', body }),
  roasteries: (q?: string) => api<Roastery[]>(`/roasteries${query({ q })}`),
}

export const noteApi = {
  create: (body: NoteRequest) => api<NoteDetail>('/notes', { method: 'POST', body }),
  update: (id: number, body: NoteRequest) => api<NoteDetail>(`/notes/${id}`, { method: 'PUT', body }),
  remove: (id: number) => api<void>(`/notes/${id}`, { method: 'DELETE' }),
  detail: (id: number) => api<NoteDetail>(`/notes/${id}`),
  mine: (params: { beanId?: number; minRating?: number; flavorCategory?: CategoryCode; cursor?: string | null }) =>
    api<CursorPage<NoteSummary>>(`/notes/me${query({ ...params, size: 10 })}`),
}

export const surveyApi = {
  get: () => api<SurveyResponse | null>('/survey'),
  save: (body: SurveyRequest) => api<SurveyResponse>('/survey', { method: 'PUT', body }),
}

export const tasteApi = {
  profile: () => api<TasteProfile>('/taste-profile/me'),
}

export const recommendationApi = {
  list: () => api<RecommendationResponse>('/recommendations'),
  feedback: (beanId: number, type: 'NOT_INTERESTED' | 'ALREADY_TRIED') =>
    api<void>('/recommendations/feedback', { method: 'POST', body: { beanId, type } }),
}
