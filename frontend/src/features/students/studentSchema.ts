import { z } from 'zod'
import { todayIso } from '@/lib/dates'
import type { Student, StudentInput } from '@/types'

/** Mirrors StudentDtos.PHONE_REGEX on the backend. */
export const PHONE_REGEX = /^\+?[0-9][0-9 ().-]{5,18}[0-9]$/
const phone = z.string().trim().regex(PHONE_REGEX, 'Must be a valid phone number')
/** Local (America/Toronto for the centre) date, not UTC, so evenings don't roll over to tomorrow. */
const today = todayIso

export const studentSchema = z.object({
  fullName: z.string().trim().min(1, 'Full name is required').max(100),
  dateOfBirth: z.string().refine((v) => !v || v < today(), 'Must be in the past'),
  grade: z.string().min(1, 'Grade is required'),
  school: z.string().max(150).optional(),
  subjectsText: z.string().max(500).optional(),
  address: z.string().max(2000).optional(),
  guardianName: z.string().trim().min(1, 'Guardian name is required').max(100),
  primaryContact: z.union([z.literal(''), phone]),
  alternateContact: z.union([z.literal(''), phone]).optional(),
  email: z.union([z.literal(''), z.string().trim().email('Must be a valid email').max(150)]),
  smsOptIn: z.boolean(),
  emergencyContactName: z.string().trim().max(100),
  emergencyContactPhone: z.union([z.literal(''), phone]),
  enrollmentDate: z.string(),
  perClassRate: z.coerce.number({ invalid_type_error: 'Must be a number' }).min(0, 'Cannot be negative'),
  status: z.enum(['ACTIVE', 'INACTIVE']),
  classGroupId: z.string().optional(),
  notes: z.string().max(4000).optional(),
})

export type StudentFormValues = z.infer<typeof studentSchema>

/** Form field names that can receive server-side fieldErrors. */
export const STUDENT_FIELDS = [
  'fullName', 'dateOfBirth', 'grade', 'school', 'address', 'guardianName', 'primaryContact', 'alternateContact', 'email',
  'emergencyContactName', 'emergencyContactPhone', 'enrollmentDate', 'perClassRate', 'status', 'classGroupId', 'notes',
]

export const emptyStudent: StudentFormValues = {
  fullName: '',
  dateOfBirth: '',
  grade: '',
  school: '',
  subjectsText: '',
  address: '',
  guardianName: '',
  primaryContact: '',
  alternateContact: '',
  email: '',
  smsOptIn: false,
  emergencyContactName: '',
  emergencyContactPhone: '',
  enrollmentDate: today(),
  perClassRate: 0,
  status: 'ACTIVE',
  classGroupId: '',
  notes: '',
}

export function toFormValues(s: Student): StudentFormValues {
  return {
    fullName: s.fullName,
    dateOfBirth: s.dateOfBirth ?? '',
    grade: s.grade,
    school: s.school ?? '',
    subjectsText: s.subjects.join(', '),
    address: s.address ?? '',
    guardianName: s.guardianName,
    primaryContact: s.primaryContact ?? '',
    alternateContact: s.alternateContact ?? '',
    email: s.email ?? '',
    smsOptIn: s.smsOptIn,
    emergencyContactName: s.emergencyContactName ?? '',
    emergencyContactPhone: s.emergencyContactPhone ?? '',
    enrollmentDate: s.enrollmentDate ?? '',
    perClassRate: s.perClassRate,
    status: s.status,
    classGroupId: s.classGroupId ? String(s.classGroupId) : '',
    notes: s.notes ?? '',
  }
}

const blank = (v?: string) => (v && v.trim() ? v.trim() : null)

export function toInput(v: StudentFormValues): StudentInput {
  return {
    fullName: v.fullName.trim(),
    dateOfBirth: blank(v.dateOfBirth),
    grade: v.grade,
    school: blank(v.school),
    subjects: (v.subjectsText ?? '').split(',').map((s) => s.trim()).filter(Boolean),
    address: blank(v.address),
    guardianName: v.guardianName.trim(),
    primaryContact: blank(v.primaryContact),
    alternateContact: blank(v.alternateContact),
    email: blank(v.email),
    smsOptIn: v.smsOptIn,
    emergencyContactName: blank(v.emergencyContactName),
    emergencyContactPhone: blank(v.emergencyContactPhone),
    enrollmentDate: blank(v.enrollmentDate),
    perClassRate: v.perClassRate,
    status: v.status,
    classGroupId: v.classGroupId ? Number(v.classGroupId) : null,
    notes: blank(v.notes),
  }
}
