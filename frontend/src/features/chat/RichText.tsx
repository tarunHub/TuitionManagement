import type { ReactNode } from 'react'

/**
 * Renders the small subset of Markdown the assistant uses (paragraphs, bullet/numbered lists, **bold**, headings and
 * pipe tables) as React elements. No HTML is ever injected, so model output can't run scripts.
 */
export function RichText({ text }: { text: string }) {
  const lines = text.replace(/\r/g, '').split('\n')
  const blocks: ReactNode[] = []
  let i = 0
  while (i < lines.length) {
    const line = lines[i]
    if (!line.trim()) {
      i++
      continue
    }
    if (/^\s*\|/.test(line)) {
      const rows: string[][] = []
      while (i < lines.length && /^\s*\|/.test(lines[i])) {
        const cells = lines[i].trim().replace(/^\||\|$/g, '').split('|').map((c) => c.trim())
        if (!cells.every((c) => /^:?-{2,}:?$/.test(c))) rows.push(cells)
        i++
      }
      const [head, ...body] = rows
      blocks.push(
        <div key={blocks.length} className="overflow-x-auto">
          <table className="my-1 w-full text-left text-sm">
            <thead><tr>{head?.map((c, k) => <th key={k} className="border-b px-2 py-1 font-semibold">{inline(c)}</th>)}</tr></thead>
            <tbody>{body.map((r, k) => <tr key={k}>{r.map((c, j) => <td key={j} className="border-b px-2 py-1">{inline(c)}</td>)}</tr>)}</tbody>
          </table>
        </div>,
      )
      continue
    }
    const bullet = /^\s*[-*•]\s+/
    const numbered = /^\s*\d+[.)]\s+/
    if (bullet.test(line) || numbered.test(line)) {
      const ordered = numbered.test(line)
      const pattern = ordered ? numbered : bullet
      const items: string[] = []
      while (i < lines.length && pattern.test(lines[i])) {
        items.push(lines[i].replace(pattern, ''))
        i++
      }
      const List = ordered ? 'ol' : 'ul'
      blocks.push(
        <List key={blocks.length} className={`my-1 space-y-0.5 pl-5 ${ordered ? 'list-decimal' : 'list-disc'}`}>
          {items.map((it, k) => <li key={k}>{inline(it)}</li>)}
        </List>,
      )
      continue
    }
    const heading = line.match(/^\s*#{1,6}\s+(.*)$/)
    blocks.push(
      <p key={blocks.length} className={heading ? 'mt-1 font-semibold' : 'my-1'}>{inline(heading ? heading[1] : line)}</p>,
    )
    i++
  }
  return <div className="text-sm leading-relaxed">{blocks}</div>
}

function inline(text: string): ReactNode[] {
  return text.split(/(\*\*[^*]+\*\*)/g).filter(Boolean).map((part, k) =>
    part.startsWith('**') && part.endsWith('**') ? <strong key={k}>{part.slice(2, -2)}</strong> : <span key={k}>{part}</span>,
  )
}
