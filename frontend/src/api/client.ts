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

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const t = tokenStore.get()
  if (t) config.headers.Authorization = `Bearer ${t}`
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
