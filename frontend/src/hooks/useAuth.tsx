import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, useSyncExternalStore, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { authApi } from '@/api/authApi'
import { platformApi } from '@/api/platformApi'
import { centreStore, tokenStore, type SelectedCentre } from '@/api/client'
import type { AdminUser } from '@/types'

interface AuthState {
  token: string | null
  user: AdminUser | null
  isPlatformAdmin: boolean
  /** The centre whose data the app shows: a staff user's own centre, or the one a platform admin switched into. */
  centre: SelectedCentre | null
  /** False while a platform admin's centre is still being picked after sign-in; centre pages wait for it. */
  centreReady: boolean
  login: (username: string, password: string) => Promise<AdminUser>
  logout: () => void
  /** Platform admin only: enter a centre (or leave with null). Clears cached data from the previous centre. */
  switchCentre: (centre: SelectedCentre | null) => void
}

const AuthContext = createContext<AuthState | null>(null)

/** Per-centre things kept in the tab that must not carry over to another centre. */
const CENTRE_SCOPED_KEYS = ['smartstart.ask.conversation', 'smartstart.ask.popupOpen']

/** The centre a platform admin last worked in, so signing in again goes straight back to it. */
const LAST_CENTRE_KEY = 'ss.lastCentre'
const lastCentre = {
  get(): number | null {
    try { return Number(localStorage.getItem(LAST_CENTRE_KEY)) || null } catch { return null }
  },
  set(id: number | null) {
    try { if (id) localStorage.setItem(LAST_CENTRE_KEY, String(id)); else localStorage.removeItem(LAST_CENTRE_KEY) } catch { /* ignore */ }
  },
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const token = useSyncExternalStore(tokenStore.subscribe, tokenStore.get)
  const selected = useSyncExternalStore(centreStore.subscribe, centreStore.get)
  const [user, setUser] = useState<AdminUser | null>(null)
  const [autoPicked, setAutoPicked] = useState(false)
  const picking = useRef(false)
  const queryClient = useQueryClient()

  useEffect(() => {
    if (!token) {
      setUser(null)
      setAutoPicked(false)
      centreStore.set(null)
      queryClient.clear()
      return
    }
    if (!user) {
      // A session that can't be loaded (expired, or from before an upgrade) signs out instead of showing a blank page.
      authApi.me().then(setUser).catch(() => tokenStore.set(null))
    }
  }, [token, user, queryClient])

  // A platform admin who isn't in a centre yet goes back into the last one they used,
  // or into the only active centre, so student pages and old links keep working.
  useEffect(() => {
    if (!user || autoPicked || picking.current) return
    if (user.role !== 'PLATFORM_ADMIN' || centreStore.get()) {
      setAutoPicked(true)
      return
    }
    picking.current = true
    platformApi.centres()
      .then((all) => {
        const active = all.filter((c) => c.active)
        const pick = active.find((c) => c.id === lastCentre.get()) ?? (active.length === 1 ? active[0] : undefined)
        if (pick && !centreStore.get()) centreStore.set({ id: pick.id, name: pick.name })
      })
      .catch(() => undefined)
      .finally(() => { picking.current = false; setAutoPicked(true) })
  }, [user, autoPicked])

  const login = useCallback(async (username: string, password: string) => {
    const res = await authApi.login(username, password)
    centreStore.set(null)
    setAutoPicked(false)
    queryClient.clear()
    tokenStore.set(res.token)
    setUser(res.user)
    return res.user
  }, [queryClient])

  const logout = useCallback(() => tokenStore.set(null), [])

  const switchCentre = useCallback((c: SelectedCentre | null) => {
    CENTRE_SCOPED_KEYS.forEach((k) => sessionStorage.removeItem(k))
    queryClient.clear()
    lastCentre.set(c?.id ?? null)
    centreStore.set(c)
  }, [queryClient])

  const isPlatformAdmin = user?.role === 'PLATFORM_ADMIN'
  const centre = useMemo<SelectedCentre | null>(() => {
    if (!user) return null
    if (user.role === 'STAFF') return user.centreId ? { id: user.centreId, name: user.centreName ?? '' } : null
    return selected
  }, [user, selected])
  const centreReady = !!user && (user.role !== 'PLATFORM_ADMIN' || !!selected || autoPicked)

  const value = useMemo(
    () => ({ token, user, isPlatformAdmin, centre, centreReady, login, logout, switchCentre }),
    [token, user, isPlatformAdmin, centre, centreReady, login, logout, switchCentre],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
