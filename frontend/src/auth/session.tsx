import { useSyncExternalStore } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router'
import { authApi } from '../api/endpoints.ts'
import type { TokenResponse } from '../api/types.ts'
import { tokenStore } from './tokenStore.ts'

export function useSession() {
  return useSyncExternalStore(tokenStore.subscribe, tokenStore.get)
}

export function startSession(tokens: TokenResponse) {
  tokenStore.set({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken, user: tokens.user })
}

export async function endSession() {
  const session = tokenStore.get()
  if (session) {
    await authApi.logout(session.refreshToken).catch(() => undefined)
  }
  tokenStore.set(null)
}

/** 로그인이 필요한 화면. 로그인 후 원래 가려던 곳으로 돌아온다. */
export function RequireAuth() {
  const session = useSession()
  const location = useLocation()
  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  return <Outlet />
}
