import { readFileSync } from 'node:fs'
import { test as base, expect, type Page } from '@playwright/test'
import { AUTH_FILE, env } from './env'

type Json = Record<string, unknown> | unknown[]

/** Thin typed client for seeding and cleaning test data through the real API. */
export class Api {
  constructor(private readonly token: string) {}

  async call<T = any>(method: string, path: string, body?: Json, query?: Record<string, string | number>): Promise<T> {
    const url = new URL(`${env.baseUrl}/api/v1${path}`)
    Object.entries(query ?? {}).forEach(([k, v]) => url.searchParams.set(k, String(v)))
    const res = await fetch(url, {
      method,
      headers: { Authorization: `Bearer ${this.token}`, 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined,
    })
    const text = await res.text()
    if (!res.ok) throw new Error(`${method} ${path} -> ${res.status}: ${text}`)
    return (text ? JSON.parse(text) : undefined) as T
  }

  createGroup(name: string) {
    return this.call<{ id: number; name: string }>('POST', '/class-groups', { name })
  }

  createStudent(overrides: Record<string, unknown> = {}) {
    const n = Math.floor(Math.random() * 1e6)
    return this.call<{ id: number; studentCode: string; fullName: string; email: string; primaryContact: string }>('POST', '/students', {
      fullName: `E2E Student ${n}`,
      dateOfBirth: '2015-05-05',
      grade: 'Grade 5',
      guardianName: `E2E Guardian ${n}`,
      primaryContact: '905-555-0000',
      email: `e2e.parent.${n}@example.com`,
      emergencyContactName: 'E2E Emergency',
      emergencyContactPhone: '905-555-0001',
      enrollmentDate: '2026-01-05',
      subjects: ['Math'],
      ...overrides,
    })
  }

  attendance(updates: { studentId: number; date: string; status: 'PRESENT' | 'ABSENT' | null }[]) {
    return this.call('PUT', '/attendance/bulk', updates)
  }

  /** Students with history cannot be deleted (rule 2), so cleanup deactivates them. */
  async retireStudent(id: number) {
    try {
      await this.call('DELETE', `/students/${id}`)
    } catch {
      await this.call('PATCH', `/students/${id}/status`, { status: 'INACTIVE' })
    }
  }
}

export function storedToken(): string {
  return JSON.parse(readFileSync(AUTH_FILE, 'utf8')).token
}

/** Re-uses the stored login by seeding sessionStorage before the app boots. */
export async function authenticate(page: Page) {
  const token = storedToken()
  await page.addInitScript((t) => window.sessionStorage.setItem('ss.token', t), token)
}

export const test = base.extend<{ api: Api; authedPage: Page }>({
  api: async ({}, use) => {
    await use(new Api(storedToken()))
  },
  authedPage: async ({ page }, use) => {
    await authenticate(page)
    await use(page)
  },
})

export { expect }

/** First day of the previous month as YYYY-MM (always fully in the past, so attendance can be marked). */
export function previousMonth(): string {
  const d = new Date()
  d.setDate(1)
  d.setMonth(d.getMonth() - 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

export const uid = () => Math.random().toString(36).slice(2, 8)
