import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const here = path.dirname(fileURLToPath(import.meta.url))

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
  baseUrl: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
  mailhogUrl: process.env.E2E_MAILHOG_URL ?? 'http://localhost:8025',
  smsSinkUrl: process.env.E2E_SMS_SINK_URL ?? 'http://localhost:8090',
  username: process.env.E2E_USERNAME ?? fileEnv.ADMIN_USERNAME ?? 'admin',
  password: process.env.E2E_PASSWORD ?? fileEnv.ADMIN_PASSWORD ?? '',
}

export const AUTH_FILE = path.resolve(here, '../.auth/token.json')
