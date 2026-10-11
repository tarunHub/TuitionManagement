import { AxiosError } from 'axios'
import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import type { ApiError } from '@/types'

export function apiError(error: unknown): ApiError | undefined {
  if (error instanceof AxiosError) {
    const data = error.response?.data as ApiError | undefined
    if (data && typeof data === 'object' && 'status' in data) return data
  }
  return undefined
}

/** A readable message for toasts. */
export function errorMessage(error: unknown, fallback = 'Something went wrong'): string {
  const api = apiError(error)
  if (api) {
    if (api.fieldErrors?.length && api.error === 'VALIDATION_FAILED') {
      return api.fieldErrors.map((f) => `${f.field}: ${f.message}`).join('; ')
    }
    return api.message
  }
  if (error instanceof AxiosError && !error.response) return 'Cannot reach the server'
  if (error instanceof Error) return error.message
  return fallback
}

/**
 * Maps server-side fieldErrors onto react-hook-form fields. Returns true if at least one was applied.
 */
export function applyServerErrors<T extends FieldValues>(error: unknown, setError: UseFormSetError<T>, fields: string[]): boolean {
  const api = apiError(error)
  if (!api?.fieldErrors?.length) return false
  let applied = false
  for (const fe of api.fieldErrors) {
    if (fields.includes(fe.field)) {
      setError(fe.field as Path<T>, { type: 'server', message: fe.message })
      applied = true
    }
  }
  return applied
}
