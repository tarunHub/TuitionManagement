// Stand-in for the Android SMS Gateway app during E2E runs (what MailHog is for email).
// POST any path: records {path, auth, body} and answers like the app (202 + queued state).
// GET /messages lists what was "sent"; DELETE /messages clears it.
import http from 'node:http'

const messages = []

http.createServer((req, res) => {
  const reply = (status, body) => {
    res.writeHead(status, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify(body))
  }
  if (req.method === 'GET' && req.url === '/messages') return reply(200, messages)
  if (req.method === 'DELETE' && req.url === '/messages') {
    messages.length = 0
    return reply(204, {})
  }
  if (req.method !== 'POST') return reply(404, { message: 'not found' })
  let raw = ''
  req.on('data', (c) => (raw += c))
  req.on('end', () => {
    let body
    try {
      body = JSON.parse(raw)
    } catch {
      return reply(400, { message: 'invalid JSON' })
    }
    if (!body?.textMessage?.text || !Array.isArray(body.phoneNumbers) || body.phoneNumbers.length === 0) {
      return reply(400, { message: 'textMessage.text and phoneNumbers are required' })
    }
    const auth = req.headers.authorization?.startsWith('Basic ')
      ? Buffer.from(req.headers.authorization.slice(6), 'base64').toString()
      : null
    const id = String(messages.length + 1)
    messages.push({ id, path: req.url, auth, text: body.textMessage.text, phoneNumbers: body.phoneNumbers })
    reply(202, { id, state: 'Pending', recipients: body.phoneNumbers.map((phoneNumber) => ({ phoneNumber, state: 'Pending' })) })
  })
}).listen(8080, () => console.log('sms-sink listening on 8080'))
