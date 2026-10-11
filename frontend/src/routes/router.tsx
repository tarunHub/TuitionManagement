import { createBrowserRouter, Link, Navigate, Outlet } from 'react-router-dom'
import { AppShell } from '@/components/AppShell'
import { Button } from '@/components/ui/button'
import { LoginPage } from '@/features/auth/LoginPage'
import { AttendancePage } from '@/features/attendance/AttendancePage'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { FeedbackPage } from '@/features/feedback/FeedbackPage'
import { FeedbackReportPage } from '@/features/feedback/FeedbackReportPage'
import { FeesPage } from '@/features/fees/FeesPage'
import { ClassGroupsPage } from '@/features/groups/ClassGroupsPage'
import { ComposeNotificationPage } from '@/features/notifications/ComposeNotificationPage'
import { NotificationsPage } from '@/features/notifications/NotificationsPage'
import { ChatPage } from '@/features/chat/ChatPage'
import { TemplatesPage } from '@/features/notifications/TemplatesPage'
import { GradeRatesPage } from '@/features/rates/GradeRatesPage'
import { SchedulePage } from '@/features/schedule/SchedulePage'
import { ClassCalendarPage } from '@/features/calendar/ClassCalendarPage'
import { StudentDetailPage } from '@/features/students/StudentDetailPage'
import { StudentFormPage } from '@/features/students/StudentFormPage'
import { StudentsListPage } from '@/features/students/StudentsListPage'
import { AuthProvider } from '@/hooks/useAuth'
import { ProtectedRoute } from './ProtectedRoute'

function NotFoundPage() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-3 text-center" data-testid="not-found">
      <p className="text-5xl font-bold text-muted-foreground">404</p>
      <p className="text-lg">Page not found</p>
      <Button asChild>
        <Link to="/dashboard">Back to dashboard</Link>
      </Button>
    </div>
  )
}

function Root() {
  return (
    <AuthProvider>
      <Outlet />
    </AuthProvider>
  )
}

export const router = createBrowserRouter([
  {
    element: <Root />,
    children: [
      { path: '/login', element: <LoginPage /> },
      {
        element: (
          <ProtectedRoute>
            <AppShell />
          </ProtectedRoute>
        ),
        children: [
          { path: '/', element: <Navigate to="/dashboard" replace /> },
          { path: '/dashboard', element: <DashboardPage /> },
          { path: '/students', element: <StudentsListPage /> },
          { path: '/students/new', element: <StudentFormPage /> },
          { path: '/students/:id', element: <StudentDetailPage /> },
          { path: '/students/:id/edit', element: <StudentFormPage /> },
          { path: '/attendance', element: <AttendancePage /> },
          { path: '/fees', element: <FeesPage /> },
          { path: '/fees/rates', element: <GradeRatesPage /> },
          { path: '/class-groups', element: <ClassGroupsPage /> },
          { path: '/feedback', element: <FeedbackPage /> },
          { path: '/feedback/:studentId/report', element: <FeedbackReportPage /> },
          { path: '/schedule', element: <SchedulePage /> },
          { path: '/calendar', element: <ClassCalendarPage /> },
          { path: '/notifications', element: <NotificationsPage /> },
          { path: '/ask', element: <ChatPage /> },
          { path: '/notifications/compose', element: <ComposeNotificationPage /> },
          { path: '/notifications/:id/edit', element: <ComposeNotificationPage /> },
          { path: '/notifications/templates', element: <TemplatesPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
])
