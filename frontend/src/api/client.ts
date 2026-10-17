import { tokenStore } from '../auth/tokenStore.ts'
import type { TokenResponse } from './types.ts'

// 백엔드 공통 응답 형식: { success, data, error }
export type ApiErrorBody = { code: string; message: string }
export type ApiEnvelope<T> = { success: boolean; data: T | null; error: ApiErrorBody | null }

export class ApiError extends Error {
  readonly code: string
  readonly status: number

  constructor(status: number, code: string, message: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

const BASE_URL = '/api/v1'

type Options = Omit<RequestInit, 'body'> & { body?: unknown; auth?: boolean }

/**
 * API 호출. Access 토큰이 만료되면 Refresh 토큰으로 한 번 재발급받고 다시 시도한다.
 */
export async function api<T>(path: string, options: Options = {}): Promise<T> {
  const response = await send(path, options)
  if (response.status === 401 && options.auth !== false && tokenStore.get()) {
    const refreshed = await refreshSession()
    if (refreshed) return unwrap<T>(await send(path, options))
    tokenStore.set(null)
  }
  return unwrap<T>(response)
}

function send(path: string, { body, auth = true, headers, ...init }: Options) {
  const h = new Headers(headers)
  if (body !== undefined) h.set('Content-Type', 'application/json')
  const session = tokenStore.get()
  if (auth && session) h.set('Authorization', `Bearer ${session.accessToken}`)
  return fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: h,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}

async function unwrap<T>(response: Response): Promise<T> {
  const envelope = (await response.json().catch(() => null)) as ApiEnvelope<T> | null
  if (!response.ok || !envelope?.success) {
    const error = envelope?.error
    throw new ApiError(
      response.status,
      error?.code ?? 'NETWORK_ERROR',
      error?.message ?? '서버와 연결하지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
  }
  return envelope.data as T
}

// 동시에 여러 요청이 401 을 받아도 재발급은 한 번만 한다
let refreshing: Promise<boolean> | null = null

function refreshSession(): Promise<boolean> {
  refreshing ??= (async () => {
    const session = tokenStore.get()
    if (!session) return false
    try {
      const response = await send('/auth/refresh', {
        method: 'POST',
        body: { refreshToken: session.refreshToken },
        auth: false,
      })
      const tokens = await unwrap<TokenResponse>(response)
      tokenStore.set({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken, user: tokens.user })
      return true
    } catch {
      return false
    } finally {
      refreshing = null
    }
  })()
  return refreshing
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '알 수 없는 오류가 발생했어요.'
}
