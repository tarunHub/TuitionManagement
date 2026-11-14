import axios from 'axios'

const TOKEN_KEY = 'ss.token'

/** JWT kept in memory and mirrored to sessionStorage so a refresh keeps the session (tab-scoped). */
let token: string | null = sessionStorage.getItem(TOKEN_KEY)
const listeners = new Set<() => void>()

export const tokenStore = {
  get: () => token,
  set(value: string | null) {
    token = value
    if (value) sessionStorage.setItem(TOKEN_KEY, value)
    else sessionStorage.removeItem(TOKEN_KEY)
    listeners.forEach((l) => l())
  },
  subscribe(listener: () => void) {
    listeners.add(listener)
    return () => listeners.delete(listener)
  },
}

const CENTRE_KEY = 'ss.centre'

export interface SelectedCentre {
  id: number
  name: string
}

/** The centre a platform admin has switched into (staff always work in their own centre). Tab-scoped like the token. */
let centre: SelectedCentre | null = (() => {
  try {
    return JSON.parse(sessionStorage.getItem(CENTRE_KEY) ?? 'null') as SelectedCentre | null
  } catch {
    return null
  }
})()
const centreListeners = new Set<() => void>()

export const centreStore = {
  get: () => centre,
  set(value: SelectedCentre | null) {
    centre = value
    if (value) sessionStorage.setItem(CENTRE_KEY, JSON.stringify(value))
    else sessionStorage.removeItem(CENTRE_KEY)
    centreListeners.forEach((l) => l())
  },
  subscribe(listener: () => void) {
    centreListeners.add(listener)
    return () => centreListeners.delete(listener)
  },
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const t = tokenStore.get()
  if (t) config.headers.Authorization = `Bearer ${t}`
  const c = centreStore.get()
  if (c) config.headers['X-Centre-Id'] = String(c.id)
  return config
})

http.interceptors.response.use(
  (res) => res,
  async (error) => {
    const status = error.response?.status
    const isLogin = error.config?.url?.includes('/auth/login')
    if (status === 401 && !isLogin) {
      // Expired or invalid token: clear it; ProtectedRoute redirects to /login.
      tokenStore.set(null)
    }
    // Blob responses (exports) carry JSON errors as blobs; parse them so callers get the API error.
    if (error.response?.data instanceof Blob && error.response.data.type.includes('json')) {
      try {
        error.response.data = JSON.parse(await error.response.data.text())
      } catch {
        // keep original
      }
    }
    return Promise.reject(error)
  },
)
