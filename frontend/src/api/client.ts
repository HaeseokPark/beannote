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

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${BASE_URL}${path}`, { ...init, headers })
  const envelope = (await response.json().catch(() => null)) as ApiEnvelope<T> | null

  if (!response.ok || !envelope?.success) {
    const error = envelope?.error
    throw new ApiError(
      response.status,
      error?.code ?? 'NETWORK_ERROR',
      error?.message ?? '서버와 통신하지 못했어요.',
    )
  }
  return envelope.data as T
}

export async function fetchHealth(): Promise<string> {
  const response = await fetch('/actuator/health')
  if (!response.ok) throw new Error(`health ${response.status}`)
  const body = (await response.json()) as { status: string }
  return body.status
}
