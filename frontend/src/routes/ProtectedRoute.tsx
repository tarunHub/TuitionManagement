import type { ReactNode } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { token } = useAuth()
  const location = useLocation()
  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  return <>{children}</>
}

/** Centre pages need a centre: a platform admin who hasn't switched into one goes to the centre list. */
export function CentreRoute() {
  const { user, centre, centreReady } = useAuth()
  if (!user || !centreReady) return null
  if (!centre) return <Navigate to="/platform/centres" replace />
  return <Outlet />
}

/** Platform pages are for platform admins only. */
export function PlatformRoute() {
  const { user, isPlatformAdmin } = useAuth()
  if (!user) return null
  if (!isPlatformAdmin) return <Navigate to="/dashboard" replace />
  return <Outlet />
}

/** "/" goes to the dashboard inside a centre, or to the centre list for a platform admin. */
export function HomeRedirect() {
  const { user, centre, centreReady } = useAuth()
  if (!user || !centreReady) return null
  return <Navigate to={centre ? '/dashboard' : '/platform/centres'} replace />
}
