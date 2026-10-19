// Shared types mirroring the backend DTOs (/api/v1). Money arrives as JSON numbers.

export interface ApiError {
  status: number
  error: string
  message: string
  fieldErrors?: { field: string; message: string }[]
  timestamp: string
  path: string
  details?: unknown
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// ---- Auth
export interface AdminUser {
  id: number
  username: string
  fullName: string | null
  role: string
}

export interface LoginResponse {
  token: string
  expiresAt: string
  user: AdminUser
}

// ---- Students
export type StudentStatus = 'ACTIVE' | 'INACTIVE'

export interface Student {
  id: number
  studentCode: string
  fullName: string
  dateOfBirth: string | null
  grade: string
  school: string | null
  subjects: string[]
  address: string | null
  guardianName: string
  primaryContact: string | null
  alternateContact: string | null
  email: string | null
  smsOptIn: boolean
  emergencyContactName: string | null
  emergencyContactPhone: string | null
  enrollmentDate: string | null
  perClassRate: number
  status: StudentStatus
  classGroupId: number | null
  classGroupName: string | null
  notes: string | null
  hasHistory: boolean | null
  createdAt: string
  updatedAt: string
}

export interface StudentInput {
  fullName: string
  dateOfBirth: string | null
  grade: string
  school?: string | null
  subjects: string[]
  address?: string | null
  guardianName: string
  primaryContact: string | null
  alternateContact?: string | null
  email: string | null
  smsOptIn?: boolean
  emergencyContactName: string | null
  emergencyContactPhone: string | null
  enrollmentDate: string | null
  perClassRate?: number | null
  status: StudentStatus
  classGroupId?: number | null
  notes?: string | null
}

export interface StudentFilters {
  search?: string
  status?: StudentStatus | ''
  grade?: string
  groupId?: number | ''
}

// ---- Grade rates / groups
export interface GradeRate {
  id: number
  grade: string
  perClassRate: number
  notes: string | null
  studentCount: number
}

export interface ClassGroup {
  id: number
  name: string
  description: string | null
  activeStudentCount: number
}

// ---- Attendance
export type AttendanceStatus = 'PRESENT' | 'ABSENT'

export interface GridDay {
  day: number
  date: string
  dayLetter: string
  weekend: boolean
  future: boolean
}

export interface GridRow {
  studentId: number
  studentCode: string
  fullName: string
  classGroupName: string | null
  cells: (AttendanceStatus | null)[]
  /** Day index → the student's batch has a class that day on the class calendar. */
  scheduled: boolean[]
  present: number
  absent: number
  held: number
  attendancePct: number | null
}

export interface AttendanceGrid {
  month: string
  today: string
  days: GridDay[]
  rows: GridRow[]
  presentPerDay: number[]
  totals: { present: number; absent: number; held: number; attendancePct: number | null }
}

export interface AttendanceUpdate {
  studentId: number
  date: string
  status: AttendanceStatus | null
}

export interface StudentAttendanceHistory {
  months: { month: string; present: number; absent: number; held: number; attendancePct: number | null }[]
  entries: { date: string; status: AttendanceStatus }[]
}

// ---- Fees
export type FeeStatus = 'UNPAID' | 'PARTIAL' | 'PAID' | 'CREDIT'
export type PaymentMethod = 'CASH' | 'E_TRANSFER' | 'CHEQUE' | 'CARD' | 'OTHER'

export interface Payment {
  id: number
  monthlyFeeId: number
  amount: number
  paymentDate: string
  method: PaymentMethod
  reference: string | null
  createdAt: string
}

export interface FeeRow {
  id: number
  month: string
  studentId: number
  studentCode: string
  fullName: string
  grade: string
  classGroupName: string | null
  studentEmail: string
  rate: number
  classesHeld: number
  classesScheduled: number | null
  scheduledOverride: boolean
  amountDue: number
  amountPaid: number
  balance: number
  status: FeeStatus
  lastPaymentDate: string | null
  lastPaymentMethod: PaymentMethod | null
  notes: string | null
  payments: Payment[]
}

export interface FeeTotals {
  amountDue: number
  amountPaid: number
  balance: number
  count: number
}

export interface FeeList {
  month: string
  rows: FeeRow[]
  totals: FeeTotals
  activeStudentsWithoutFee: number
}

export interface PaymentInput {
  amount: number
  paymentDate: string
  method: PaymentMethod
  reference?: string | null
}

// ---- Feedback
export interface FeedbackRow {
  id: number | null
  month: string
  studentId: number
  studentCode: string
  fullName: string
  grade: string
  classGroupName: string | null
  guardianName: string
  topicsTaught: string | null
  improvementAreas: string | null
  achievements: string | null
  previousMonthTopics: string | null
  updatedAt: string | null
}

export interface Feedback {
  id: number
  month: string
  studentId: number
  topicsTaught: string
  improvementAreas: string | null
  achievements: string | null
  createdAt: string
  updatedAt: string
}

export interface FeedbackInput {
  topicsTaught: string
  improvementAreas?: string | null
  achievements?: string | null
}

// ---- Dashboard
export interface ClassOccurrence {
  slotId: number
  date: string
  dayOfWeek: ScheduleDay
  startTime: string
  endTime: string
  classGroupId: number
  classGroupName: string
  subject: string | null
  location: string | null
  teacher: string | null
}

export interface Dashboard {
  month: string
  students: { active: number; inactive: number }
  fees: { billed: number; collected: number; outstanding: number; collectionRatePct: number | null }
  attendance: { classesHeldMax: number; totalPresent: number; totalAbsent: number; averageAttendancePct: number | null }
  upcomingClasses: ClassOccurrence[]
  outstandingStudents: { studentId: number; studentCode: string; fullName: string; balance: number }[]
  feedbackMissing: { month: string; count: number }
}

export interface MonthSummary {
  month: string
  billed: number
  collected: number
  outstanding: number
}

export interface MonthlySummary {
  year: number
  months: MonthSummary[]
  total: MonthSummary
}

// ---- Schedule
export type ScheduleDay = 'MON' | 'TUE' | 'WED' | 'THU' | 'FRI' | 'SAT' | 'SUN'
export const SCHEDULE_DAYS: ScheduleDay[] = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN']

export interface Term {
  id: number
  name: string
  startDate: string
  endDate: string
  calendarName: string | null
  active: boolean
}

export interface TermInput {
  name: string
  startDate: string
  endDate: string
  calendarName?: string | null
  active?: boolean
}

export interface Slot {
  id: number
  termId: number
  termName: string
  classGroupId: number
  classGroupName: string
  dayOfWeek: ScheduleDay
  startTime: string
  endTime: string
  subject: string | null
  location: string | null
  teacher: string | null
  extraEmails: string | null
  notes: string | null
  googleEventId: string | null
  recipientCount: number
  overlappingSlotIds: number[]
  overlapWarning: string | null
}

export interface SlotInput {
  termId?: number | null
  classGroupId: number
  dayOfWeek: ScheduleDay
  startTime: string
  endTime: string
  subject?: string | null
  location?: string | null
  teacher?: string | null
  extraEmails?: string | null
  notes?: string | null
}

export interface Recipients {
  slotId: number
  emails: string[]
  fromGroup: number
  extra: number
}

export interface GoogleStatus {
  enabled: boolean
  message: string
}

export interface SyncResult {
  calendarName: string
  created: number
  updated: number
  skipped: number
  errors: string[]
}

// ---- Notifications
export type NotificationType = 'SCHEDULE' | 'SCHEDULE_CHANGE' | 'CLASS_CANCELLED' | 'FEE_REMINDER' | 'FEEDBACK' | 'GENERAL'
export const NOTIFICATION_TYPES: NotificationType[] = ['GENERAL', 'SCHEDULE', 'SCHEDULE_CHANGE', 'CLASS_CANCELLED', 'FEE_REMINDER', 'FEEDBACK']
export type NotificationStatus = 'DRAFT' | 'SCHEDULED' | 'SENT' | 'FAILED'
export type RecipientMode = 'ALL_ACTIVE' | 'CLASS_GROUPS' | 'STUDENTS' | 'NONE'
export type ComposeAction = 'DRAFT' | 'SEND_NOW' | 'SCHEDULE'
export type NotificationChannel = 'EMAIL' | 'SMS'

export interface Notification {
  id: number
  type: NotificationType
  channel: NotificationChannel
  subject: string
  body: string
  recipients: string[]
  recipientCount: number
  status: NotificationStatus
  scheduledAt: string | null
  sentAt: string | null
  errorMessage: string | null
  templateId: number | null
  createdAt: string
  updatedAt: string
}

export interface RecipientSelection {
  mode: RecipientMode
  classGroupIds?: number[]
  studentIds?: number[]
  extraEmails?: string
  extraPhones?: string
}

export interface ComposeInput {
  type: NotificationType
  templateId?: number | null
  subject: string
  body: string
  channel: NotificationChannel
  recipients: RecipientSelection
  action: ComposeAction
  scheduledAt?: string | null
  month?: string | null
}

export interface ChannelStatus {
  email: boolean
  sms: boolean
  smsMessage: string | null
}

export interface PreviewResult {
  subject: string
  body: string
  recipients: string[]
}

/** One email or SMS ready to go, shown for review before sending. */
export interface OutgoingMessage {
  studentId: number
  studentName: string
  className: string
  channel: NotificationChannel
  type: NotificationType
  templateId: number | null
  recipient: string
  subject: string
  body: string
}

export interface MessagePreview {
  messages: OutgoingMessage[]
  /** Students with no parent email or SMS opt-in. */
  skipped: string[]
}

export type MessageToSend = Pick<OutgoingMessage, 'studentId' | 'channel' | 'type' | 'templateId' | 'subject' | 'body'>

export interface BulkSendResult {
  created: number
  sent: number
  failed: number
  notifications: Notification[]
}

export interface Template {
  id: number
  name: string
  type: NotificationType
  subject: string
  body: string
  createdAt: string
  updatedAt: string
}

export interface TemplateInput {
  name: string
  type: NotificationType
  subject: string
  body: string
}

export interface Placeholder {
  name: string
  description: string
}

// ---- Assistant ("Ask")
export type ChatRole = 'USER' | 'ASSISTANT'

export interface ChatMessage {
  role: ChatRole
  content: string
}

export interface ChatSource {
  label: string
  link: string
}

export interface ChatAnswer {
  answer: string
  sources: ChatSource[]
  grounded: boolean
  toolsUsed: string[]
  model: string
  durationMs: number
}

export interface ChatStatus {
  enabled: boolean
  available: boolean
  model: string
  message: string | null
}

export interface FeedbackDraftInput {
  studentId: number
  month: string
  notes: string
  topicsTaught: string
  improvementAreas: string
  achievements: string
}

export interface FeedbackDraft {
  topicsTaught: string
  improvementAreas: string
  achievements: string
  model: string
  durationMs: number
}

// ---- Class calendar
export type Weekday = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export interface ClassSession {
  id: number
  classGroupId: number
  classGroupName: string
  date: string
  startTime: string
  endTime: string
  location: string | null
  notes: string | null
}

export interface ClassSessionInput {
  classGroupId: number
  date: string
  startTime: string
  endTime: string
  location?: string
  notes?: string
}

export interface RepeatWeeklyInput {
  classGroupId: number
  month: string
  weekdays: Weekday[]
  startTime: string
  endTime: string
  location?: string
}

export interface ReminderSettings {
  enabled: boolean
  /** 0 = morning of the class, 1 = day before, 2 = two days before */
  daysBefore: number
  sendTime: string
  sendEmail: boolean
  sendSms: boolean
  lastRunDate: string | null
  smsAvailable: boolean
}

export interface ClassChangeInput {
  kind: 'ADDED' | 'CHANGED' | 'CANCELLED'
  classGroupId: number
  date: string
  startTime: string
  endTime: string
  location: string | null
  previousDate?: string
  previousStartTime?: string
  previousEndTime?: string
  previousLocation?: string | null
}
