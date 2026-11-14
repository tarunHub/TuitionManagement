import { createContext, useCallback, useContext, useEffect, useMemo, useState, useSyncExternalStore, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { authApi } from '@/api/authApi'
import { centreStore, tokenStore, type SelectedCentre } from '@/api/client'
import type { AdminUser } from '@/types'

interface AuthState {
  token: string | null
  user: AdminUser | null
  isPlatformAdmin: boolean
  /** The centre whose data the app shows: a staff user's own centre, or the one a platform admin switched into. */
  centre: SelectedCentre | null
  login: (username: string, password: string) => Promise<AdminUser>
  logout: () => void
  /** Platform admin only: enter a centre (or leave with null). Clears cached data from the previous centre. */
  switchCentre: (centre: SelectedCentre | null) => void
}

const AuthContext = createContext<AuthState | null>(null)

/** Per-centre things kept in the tab that must not carry over to another centre. */
const CENTRE_SCOPED_KEYS = ['smartstart.ask.conversation', 'smartstart.ask.popupOpen']

export function AuthProvider({ children }: { children: ReactNode }) {
  const token = useSyncExternalStore(tokenStore.subscribe, tokenStore.get)
  const selected = useSyncExternalStore(centreStore.subscribe, centreStore.get)
  const [user, setUser] = useState<AdminUser | null>(null)
  const queryClient = useQueryClient()

  useEffect(() => {
    if (!token) {
      setUser(null)
      centreStore.set(null)
      queryClient.clear()
      return
    }
    if (!user) {
      authApi.me().then(setUser).catch(() => undefined)
    }
  }, [token, user, queryClient])

  const login = useCallback(async (username: string, password: string) => {
    const res = await authApi.login(username, password)
    centreStore.set(null)
    queryClient.clear()
    tokenStore.set(res.token)
    setUser(res.user)
    return res.user
  }, [queryClient])

  const logout = useCallback(() => tokenStore.set(null), [])

  const switchCentre = useCallback((c: SelectedCentre | null) => {
    CENTRE_SCOPED_KEYS.forEach((k) => sessionStorage.removeItem(k))
    queryClient.clear()
    centreStore.set(c)
  }, [queryClient])

  const isPlatformAdmin = user?.role === 'PLATFORM_ADMIN'
  const centre = useMemo<SelectedCentre | null>(() => {
    if (!user) return null
    if (user.role === 'STAFF') return user.centreId ? { id: user.centreId, name: user.centreName ?? '' } : null
    return selected
  }, [user, selected])

  const value = useMemo(
    () => ({ token, user, isPlatformAdmin, centre, login, logout, switchCentre }),
    [token, user, isPlatformAdmin, centre, login, logout, switchCentre],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
