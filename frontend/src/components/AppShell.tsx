import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  Bell,
  Building2,
  Settings,
  ShieldCheck,
  CalendarCheck,
  CalendarRange,
  DollarSign,
  KeyRound,
  LayoutDashboard,
  LogOut,
  Menu,
  MessageSquareText,
  Percent,
  Sparkles,
  Users,
  UsersRound,
  X,
} from 'lucide-react'
import { platformApi } from '@/api/platformApi'
import { Button } from '@/components/ui/button'
import { NativeSelect } from '@/components/ui/native-select'
import { MonthPicker } from '@/components/MonthPicker'
import { ChangePasswordDialog } from '@/features/auth/ChangePasswordDialog'
import { ChatProvider } from '@/features/chat/ChatContext'
import { ChatWidget } from '@/features/chat/ChatWidget'
import { useAuth } from '@/hooks/useAuth'
import { useMonthParam } from '@/hooks/useMonthParam'
import { cn } from '@/lib/utils'

const NAV = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/students', label: 'Students', icon: Users },
  { to: '/calendar', label: 'Class Calendar', icon: CalendarRange },
  { to: '/attendance', label: 'Attendance', icon: CalendarCheck },
  { to: '/fees', label: 'Fees', icon: DollarSign, end: true },
  { to: '/fees/rates', label: 'Fee Rates', icon: Percent },
  { to: '/class-groups', label: 'Class Groups', icon: UsersRound },
  { to: '/feedback', label: 'Feedback', icon: MessageSquareText },
  { to: '/notifications', label: 'Notifications', icon: Bell },
  { to: '/ask', label: 'Ask', icon: Sparkles },
  { to: '/settings', label: 'Centre Settings', icon: Settings },
]

const PLATFORM_NAV = [
  { to: '/platform/centres', label: 'Centres', icon: Building2 },
  { to: '/platform/admins', label: 'Platform Admins', icon: ShieldCheck },
]

/** Platform admin: which centre they are in, a quick switch to another, and a way back out. */
function CentreBar() {
  const { centre, switchCentre } = useAuth()
  const navigate = useNavigate()
  const centres = useQuery({ queryKey: ['platform', 'centres'], queryFn: platformApi.centres })
  if (!centre) return null
  return (
    <div className="no-print flex flex-wrap items-center gap-2 border-b border-amber-300 bg-amber-50 px-4 py-1.5 text-sm text-amber-900" data-testid="centre-bar">
      <Building2 className="size-4" aria-hidden />
      <span>You are viewing <strong>{centre.name}</strong> as platform admin.</span>
      <NativeSelect
        aria-label="Switch centre"
        className="h-7 w-44 bg-white py-0 text-sm"
        value={centre.id}
        onChange={(e) => {
          const c = centres.data?.find((x) => x.id === Number(e.target.value))
          if (c) {
            switchCentre({ id: c.id, name: c.name })
            navigate('/dashboard')
          }
        }}
        data-testid="centre-switch"
      >
        {!centres.data?.some((c) => c.id === centre.id) && <option value={centre.id}>{centre.name}</option>}
        {centres.data?.map((c) => <option key={c.id} value={c.id}>{c.name}{c.active ? '' : ' (inactive)'}</option>)}
      </NativeSelect>
      <Button size="sm" variant="outline" className="ml-auto h-7" onClick={() => { switchCentre(null); navigate('/platform/centres') }} data-testid="centre-exit">
        Exit centre
      </Button>
    </div>
  )
}

/** Routes whose data is per month; the top bar shows the month picker for them. */
const MONTH_ROUTES = ['/dashboard', '/calendar', '/attendance', '/fees', '/feedback']

function TopBarMonthPicker() {
  const [month, setMonth] = useMonthParam()
  return <MonthPicker month={month} onChange={setMonth} />
}

export function AppShell() {
  const { user, logout, centre, isPlatformAdmin } = useAuth()
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [pwOpen, setPwOpen] = useState(false)
  const showMonth = MONTH_ROUTES.includes(location.pathname)
  const title = centre?.name || 'Tuition Portal'

  const link = ({ to, label, icon: Icon, end }: { to: string; label: string; icon: typeof Bell; end?: boolean }) => (
    <NavLink
      key={to}
      to={to}
      end={end}
      onClick={() => setMobileOpen(false)}
      data-testid={`nav-${label.toLowerCase().replace(/\s+/g, '-')}`}
      className={({ isActive }) =>
        cn(
          'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
          isActive ? 'bg-primary text-primary-foreground' : 'text-slate-200 hover:bg-white/10',
        )
      }
    >
      <Icon className="h-4 w-4" aria-hidden />
      {label}
    </NavLink>
  )

  const nav = (
    <nav aria-label="Main" className="flex flex-col gap-1 p-3">
      {isPlatformAdmin && (
        <>
          <p className="px-3 pb-1 pt-1 text-xs font-semibold uppercase tracking-wide text-slate-400">Platform</p>
          {PLATFORM_NAV.map(link)}
          {centre && <p className="mt-3 truncate px-3 pb-1 text-xs font-semibold uppercase tracking-wide text-slate-400">{centre.name}</p>}
        </>
      )}
      {centre && NAV.map(link)}
    </nav>
  )
  return (
    <ChatProvider key={centre?.id ?? 'none'}>
      <div className="flex min-h-screen">
        <aside className="no-print hidden w-56 shrink-0 bg-slate-900 lg:block">
          <div className="truncate px-5 py-5 text-lg font-bold text-white" title={title} data-testid="sidebar-title">{title}</div>
          {nav}
        </aside>

        {mobileOpen && (
          <div className="fixed inset-0 z-40 lg:hidden" role="dialog" aria-modal="true" aria-label="Navigation">
            <div className="absolute inset-0 bg-black/50" onClick={() => setMobileOpen(false)} />
            <aside className="relative h-full w-64 bg-slate-900">
              <div className="flex items-center justify-between px-5 py-5 text-lg font-bold text-white">
                <span className="truncate">{title}</span>
                <button aria-label="Close menu" onClick={() => setMobileOpen(false)}>
                  <X className="h-5 w-5" />
                </button>
              </div>
              {nav}
            </aside>
          </div>
        )}

        <div className="flex min-w-0 flex-1 flex-col">
          <header className="no-print sticky top-0 z-30 flex h-14 items-center gap-2 border-b bg-background/95 px-2 backdrop-blur sm:gap-3 sm:px-4">
            <Button variant="ghost" size="icon" className="lg:hidden" aria-label="Open menu" onClick={() => setMobileOpen(true)}>
              <Menu />
            </Button>
            {showMonth && <TopBarMonthPicker />}
            <div className="ml-auto flex items-center gap-1 sm:gap-2">
              <span className="hidden text-sm text-muted-foreground sm:inline" data-testid="admin-name">
                {user?.fullName ?? user?.username}
              </span>
              <Button variant="ghost" size="sm" onClick={() => setPwOpen(true)} aria-label="Change password">
                <KeyRound />
                <span className="hidden md:inline">Password</span>
              </Button>
              <Button variant="outline" size="sm" onClick={logout} aria-label="Logout" data-testid="logout-button">
                <LogOut /> <span className="hidden sm:inline">Logout</span>
              </Button>
            </div>
          </header>
          {isPlatformAdmin && <CentreBar />}
          <main className="flex-1 p-4 md:p-6">
            <Outlet />
          </main>
        </div>
        <ChangePasswordDialog open={pwOpen} onOpenChange={setPwOpen} />
        {centre && location.pathname !== '/ask' && <ChatWidget />}
      </div>
    </ChatProvider>
  )
}
