// Stand-in for Ollama during E2E runs: deterministic and instant, but it still makes the backend run real tool
// lookups against the database, then answers from the numbers those lookups returned.
//   question mentions "how many"      -> calls get_student_counts, answers "There are **N** active students."
//   question mentions "owe" or "fee"  -> calls get_fees for the current month, answers with the total due
//   anything else                     -> answers without a lookup (exercises the backend's "not grounded" path)
//   request with a "format" schema    -> feedback draft: echoes the notes and student name back as the three fields
import http from 'node:http'

const MODEL = 'qwen3.5:latest'
const send = (res, status, body) => {
  res.writeHead(status, { 'Content-Type': 'application/json' })
  res.end(JSON.stringify(body))
}
const reply = (message) => ({ model: MODEL, created_at: new Date().toISOString(), done: true, message: { role: 'assistant', content: '', ...message } })

function feedbackDraft(messages) {
  const prompt = messages[messages.length - 1].content
  const name = prompt.match(/^Student: ([^,\n]+)/m)?.[1] ?? 'The student'
  const notes = prompt.match(/Tutor's notes:\n([^\n]*)/)?.[1] ?? ''
  const draft = { topicsTaught: `Drafted: ${notes}`, improvementAreas: `${name} should keep practising.`, achievements: `${name} worked hard this month.` }
  return reply({ content: JSON.stringify(draft) })
}

function answer(messages) {
  const last = messages[messages.length - 1]
  if (last.role === 'tool') {
    const data = JSON.parse(last.content)
    if (last.tool_name === 'get_student_counts') return reply({ content: `There are **${data.active}** active students.` })
    if (last.tool_name === 'get_fees') return reply({ content: `For ${data.month} the total due is $${Number(data.totals.amountDue).toFixed(2)}.` })
    return reply({ content: `Looked up ${last.tool_name}.` })
  }
  const question = [...messages].reverse().find((m) => m.role === 'user')?.content.toLowerCase() ?? ''
  const thisMonth = messages[0].content.match(/\((\d{4}-\d{2})\)/)?.[1]
  if (question.includes('how many')) {
    return reply({ tool_calls: [{ function: { name: 'get_student_counts', arguments: {} } }] })
  }
  if (question.includes('owe') || question.includes('fee')) {
    return reply({ tool_calls: [{ function: { name: 'get_fees', arguments: { month: thisMonth } } }] })
  }
  return reply({ content: 'It is sunny (made up).' })
}

http.createServer((req, res) => {
  if (req.method === 'GET' && req.url === '/api/tags') return send(res, 200, { models: [{ name: MODEL, model: MODEL }] })
  if (req.method === 'GET' && req.url === '/api/version') return send(res, 200, { version: 'fake' })
  if (req.method !== 'POST' || req.url !== '/api/chat') return send(res, 404, { error: 'not found' })
  let raw = ''
  req.on('data', (c) => (raw += c))
  req.on('end', () => {
    try {
      const body = JSON.parse(raw)
      send(res, 200, body.format ? feedbackDraft(body.messages) : answer(body.messages))
    } catch (e) {
      send(res, 400, { error: String(e) })
    }
  })
}).listen(11434, () => console.log('fake-ollama listening on 11434'))
