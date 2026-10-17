import type { User } from '../api/types.ts'

/**
 * 로그인 상태 저장소. 새로고침해도 로그인이 유지되도록 localStorage 에 둔다.
 * (포트폴리오 MVP 기준. 운영에서는 Refresh 토큰을 HttpOnly 쿠키로 옮기는 것이 안전하다)
 */
export type Session = { accessToken: string; refreshToken: string; user: User }

const KEY = 'beanote.session'
const listeners = new Set<() => void>()

let current: Session | null = read()

function read(): Session | null {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? (JSON.parse(raw) as Session) : null
  } catch {
    return null
  }
}

export const tokenStore = {
  get: () => current,

  set(session: Session | null) {
    current = session
    try {
      if (session) localStorage.setItem(KEY, JSON.stringify(session))
      else localStorage.removeItem(KEY)
    } catch {
      // 저장소를 못 쓰는 환경(사생활 보호 모드 등)에서는 메모리에만 둔다
    }
    listeners.forEach((listener) => listener())
  },

  subscribe(listener: () => void) {
    listeners.add(listener)
    return () => {
      listeners.delete(listener)
    }
  },
}
