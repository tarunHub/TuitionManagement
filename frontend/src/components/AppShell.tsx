import { useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import {
  Bell,
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
import { Button } from '@/components/ui/button'
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
]

/** Routes whose data is per month; the top bar shows the month picker for them. */
const MONTH_ROUTES = ['/dashboard', '/calendar', '/attendance', '/fees', '/feedback']

function TopBarMonthPicker() {
  const [month, setMonth] = useMonthParam()
  return <MonthPicker month={month} onChange={setMonth} />
}

export function AppShell() {
  const { user, logout } = useAuth()
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [pwOpen, setPwOpen] = useState(false)
  const showMonth = MONTH_ROUTES.includes(location.pathname)

  const nav = (
    <nav aria-label="Main" className="flex flex-col gap-1 p-3">
      {NAV.map(({ to, label, icon: Icon, end }) => (
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
      ))}
    </nav>
  )

  return (
    <ChatProvider>
      <div className="flex min-h-screen">
        <aside className="no-print hidden w-56 shrink-0 bg-slate-900 lg:block">
          <div className="px-5 py-5 text-lg font-bold text-white">Smart Start</div>
          {nav}
        </aside>

        {mobileOpen && (
          <div className="fixed inset-0 z-40 lg:hidden" role="dialog" aria-modal="true" aria-label="Navigation">
            <div className="absolute inset-0 bg-black/50" onClick={() => setMobileOpen(false)} />
            <aside className="relative h-full w-64 bg-slate-900">
              <div className="flex items-center justify-between px-5 py-5 text-lg font-bold text-white">
                Smart Start
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
          <main className="flex-1 p-4 md:p-6">
            <Outlet />
          </main>
        </div>
        <ChangePasswordDialog open={pwOpen} onOpenChange={setPwOpen} />
        {location.pathname !== '/ask' && <ChatWidget />}
      </div>
    </ChatProvider>
  )
}
