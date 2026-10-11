import { http } from '@/api/client'

/** Downloads a file from an authenticated endpoint (the browser cannot add the bearer header to a plain link). */
export async function downloadFile(url: string, params: Record<string, unknown>, fallbackName: string) {
  const res = await http.get<Blob>(url, { params, responseType: 'blob' })
  const disposition = res.headers['content-disposition'] as string | undefined
  const match = disposition?.match(/filename="?([^";]+)"?/)
  const name = match?.[1] ?? fallbackName
  const href = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = href
  a.download = name
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(href)
}
