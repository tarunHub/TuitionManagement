import { createContext, useCallback, useContext, useEffect, useMemo, useState, useSyncExternalStore, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { authApi } from '@/api/authApi'
import { tokenStore } from '@/api/client'
import type { AdminUser } from '@/types'

interface AuthState {
  token: string | null
  user: AdminUser | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const token = useSyncExternalStore(tokenStore.subscribe, tokenStore.get)
  const [user, setUser] = useState<AdminUser | null>(null)
  const queryClient = useQueryClient()

  useEffect(() => {
    if (!token) {
      setUser(null)
      queryClient.clear()
      return
    }
    if (!user) {
      authApi.me().then(setUser).catch(() => undefined)
    }
  }, [token, user, queryClient])

  const login = useCallback(async (username: string, password: string) => {
    const res = await authApi.login(username, password)
    tokenStore.set(res.token)
    setUser(res.user)
  }, [])

  const logout = useCallback(() => tokenStore.set(null), [])

  const value = useMemo(() => ({ token, user, login, logout }), [token, user, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
