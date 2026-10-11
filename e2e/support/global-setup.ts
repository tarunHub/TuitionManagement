import { mkdirSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { AUTH_FILE, env } from './env'

/** Logs in once through the API and stores the token for reuse by every test. */
export default async function globalSetup() {
  if (!env.password) throw new Error('Set E2E_PASSWORD or ADMIN_PASSWORD in ../.env')
  const res = await fetch(`${env.baseUrl}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: env.username, password: env.password }),
  })
  if (!res.ok) throw new Error(`E2E login failed: ${res.status} ${await res.text()}`)
  const { token } = (await res.json()) as { token: string }
  mkdirSync(path.dirname(AUTH_FILE), { recursive: true })
  writeFileSync(AUTH_FILE, JSON.stringify({ token }))
}
