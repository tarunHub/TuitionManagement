import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const here = path.dirname(fileURLToPath(import.meta.url))

/** Reads KEY=value pairs from the repo-root .env so tests share the app's admin credentials. */
function dotenv(): Record<string, string> {
  const file = path.resolve(here, '../../.env')
  if (!existsSync(file)) return {}
  const out: Record<string, string> = {}
  for (const line of readFileSync(file, 'utf8').split(/\r?\n/)) {
    const m = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/)
    if (m) out[m[1]] = m[2].replace(/^["']|["']$/g, '')
  }
  return out
}

const fileEnv = dotenv()

export const env = {
  baseUrl: process.env.BASE_URL ?? 'http://localhost:5173',
  username: process.env.TEST_USERNAME ?? fileEnv.ADMIN_USERNAME ?? 'admin',
  password: process.env.TEST_PASSWORD ?? fileEnv.ADMIN_PASSWORD ?? '',
}

/**
 * Failed logins lock a username after a few attempts (LOGIN_MAX_FAILURES), so negative tests
 * must never use the real admin. Each call returns a fresh throwaway username.
 */
export function throwawayUsername(): string {
  return `pw-nobody-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}
