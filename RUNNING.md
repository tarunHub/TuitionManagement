# Smart Start Student Portal — Running the project

`Readme.md` is the build specification. This file explains how to run what was built.

## Quick start (Docker)

```bash
cp .env.example .env          # then set DB_PASSWORD, JWT_SECRET (>= 32 chars), ADMIN_PASSWORD
docker compose up -d --build
```

| What | URL |
|---|---|
| App | http://localhost:5173 (log in with `ADMIN_USERNAME` / `ADMIN_PASSWORD` from `.env`) |
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| MailHog (sent emails) | http://localhost:8025 |

With `SPRING_PROFILES_ACTIVE=dev` (the default in `.env.example`) the fictional seed data from section 11 is loaded:
grade rates, 4 class groups, 6 active + 2 inactive students, August 2026 attendance/fees/payments/feedback,
the Fall 2026 term with two slots, and four notification templates. The August 2026 dashboard shows
billed $930, collected $960, outstanding −$30, collection rate 103%, classes held (max) 16.

To start again from a clean database: `docker compose down -v && docker compose up -d`.

## Running without Docker (for development)

Requirements: JDK 21+, Node 20+, plus Postgres 16 and MailHog (e.g. `docker compose up -d postgres mailhog`).

```bash
# backend (standalone Gradle build in backend/)
set -a; source .env; set +a
cd backend && ./gradlew bootRun          # SPRING_PROFILES_ACTIVE from .env selects the dev seed

# frontend (Vite dev server; proxies /api to http://localhost:8080)
cd frontend && npm install && npm run dev
```

`npm run build` type-checks and produces a production bundle in `frontend/dist`.

## End-to-end tests (Playwright)

With the stack running (Docker or local):

```bash
cd e2e && npm install && npx playwright install chromium
npx playwright test
```

**Always start the backend with the E2E overlay before running the tests.** It sends all email to MailHog and all SMS
to `sms-sink` (a small stand-in for the Android gateway, `e2e/sms-sink/server.mjs`, listing what it received at
http://localhost:8090/messages), whatever `.env` says, so tests never email or text real people. Switch back afterwards:

```bash
docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d backend   # before the tests
docker compose up -d backend                                                   # back to .env's email/SMS settings
```

Credentials are read from `E2E_USERNAME`/`E2E_PASSWORD`, falling back to `ADMIN_USERNAME`/`ADMIN_PASSWORD` in `../.env`.
The suite seeds its own data through the API (unique groups/students per run) and retires it afterwards;
the failed-login test uses a throwaway username so it never locks the real admin.
No unit/integration tests have been written yet.

## SMS via an Android phone (optional)

Texts are sent from your own Android phone and plan through the free, open-source **SMS Gateway for Android** app
(https://sms-gate.app). With an unlimited-text plan this costs nothing extra.

1. **Install the app** on an Android phone with a SIM: download it from the project's site or GitHub releases
   (https://github.com/capcom6/android-sms-gateway) and allow it to send SMS.
2. **Pick a mode** in the app:
   - **Local server** (recommended, nothing leaves your network): turn on *Local server*. The app shows the phone's
     address (e.g. `http://192.168.1.50:8080`) plus a username and password. The phone and the laptop running the
     portal must be on the same Wi-Fi, and the phone should keep a fixed address (reserve it in your router).
   - **Cloud server**: turn on *Cloud server* and note the generated username and password. Works from anywhere,
     but messages pass through the app's public relay.
3. **Configure `.env`:**
   ```
   SMS_ENABLED=true
   SMS_GATEWAY_URL=http://192.168.1.50:8080/message        # local server (note the /message path)
   # SMS_GATEWAY_URL=https://api.sms-gate.app/3rdparty/v1/messages   # cloud server instead
   SMS_GATEWAY_USERNAME=<from the app>
   SMS_GATEWAY_PASSWORD=<from the app>
   SMS_DEFAULT_COUNTRY_CODE=1                               # added to numbers without +, e.g. 905-555-0102 -> +19055550102
   ```
4. **Restart the backend:** `docker compose up -d backend`. The **SMS** option on *Compose notification* no longer
   shows the "SMS is off" hint.
5. **Opt parents in:** edit each student and tick **Send SMS reminders to the primary contact**, only for parents who
   agreed to receive texts. Nobody else is ever texted.
6. **Test it:** compose an SMS to *Individual students* → your own test student (with your number as primary contact),
   or use *Only the extras below* with your number, and **Send now**.

Notes:
- Keep the phone charged, on Wi-Fi, and exclude the app from battery optimisation, or scheduled reminders may fail.
  Failed texts appear in *Notifications* with the gateway's error and a **Retry** button.
- "Sent" means the phone accepted the message into its queue; the app shows the final delivery state.
- Carriers may flag a personal line that sends large volumes; keep it to normal reminder volumes.
- Class and fee reminders also text opted-in parents once SMS is on. Change the wording by creating templates named
  "Class Reminder SMS" / "Fee Reminder SMS".

## "Ask" assistant (local AI, Ollama)

The **Ask** page answers questions about the data using a model running in Ollama on this computer. Nothing is sent
to the internet and it costs nothing per question. It can only read data.

1. Install and start Ollama (https://ollama.com), then pull the model: `ollama pull qwen3.5:latest`.
   On a 16 GB Mac, models of roughly 10B parameters or less run well; bigger ones (e.g. 27B+) are slow or won't fit.
2. In `.env`: `CHAT_ENABLED=true` and `CHAT_MODEL=qwen3.5:latest` (`OLLAMA_URL` is only used when the backend runs
   outside Docker; inside Docker it talks to `host.docker.internal:11434`).
3. `docker compose up -d backend`, then open **Ask** in the sidebar, or the round button at the bottom right of any
   screen. Answers take about 5–25 seconds.

The same model powers **Write with AI** in the Feedback drawer: type rough notes, press **Draft feedback**, review the
three boxes, then **Save feedback**.

To see exactly what is sent to Ollama, set `LOGGING_LEVEL_COM_SMARTSTART_PORTAL_CHAT=DEBUG` in `.env`, run
`docker compose up -d backend`, and watch `docker compose logs -f backend` (full request/response bodies; they contain
student data, so set it back to `INFO` when done).

Check accuracy after changing the model or the tools (uses the real model and data, read-only):

```bash
python3 tools/chat_eval.py        # 20 questions with known answers; update expectations when the data changes
```

The E2E suite does not use Ollama: the overlay starts `e2e/fake-ollama`, a scripted stand-in.

## Monthly class calendar

1. Open **Class Calendar** in the sidebar and pick the month in the top bar.
2. Use **Repeat weekly** (e.g. Batch1, Tue + Thu, 17:00–18:00) to fill the month, then click days to add, move or
   remove single classes (holidays, extra classes).
3. **Send schedule to parents** sends each batch's dates by email and/or SMS (needs email / SMS set up in `.env`).
   Automatic reminders are set under **Reminder settings** on the Class Calendar (default 8 AM on the class day,
   email + SMS). **Send reminder now** (in a class) and **Send today's reminders** send them on demand. After you
   add, move or remove a class, the page asks whether to notify parents.
4. On **Attendance**, those days show **S**; click to change them to **P** or **A** after the class and press **Save**.
   Amber S = class day passed but not marked yet.

## Importing the centre's spreadsheet

`tools/import_excel.py` replaces all portal data (students, groups, grade rates, attendance, fees, payments, feedback,
notification history, term, schedule slots) with `src/main/resources/SmartStart_Student_Management.xlsx`. Admin logins
and notification templates are kept. It prints one SQL transaction plus a report of anything it skipped or adjusted,
so review the report before applying. Back up first:

```bash
mkdir -p backups && docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists' > backups/before-import.sql
pip install openpyxl
python3 tools/import_excel.py src/main/resources/SmartStart_Student_Management.xlsx > /tmp/import.sql
docker compose exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < /tmp/import.sql
```

Restore a backup with the same `psql` command and the backup file. The script's docstring explains how each sheet is
read (monthly sheets are matched to students by first name; October-style "scheduled" billing is converted to the
app's classes-held rule). `backups/` is git-ignored because dumps contain personal data.

**Running the E2E suite writes test students, groups and fees into whatever database the stack uses** (they are
deactivated afterwards, not deleted). With real data loaded, back up first and restore afterwards:

```bash
docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists' > backups/before-e2e.sql
docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d backend && (cd e2e && npx playwright test)
docker compose stop backend
docker compose exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -q -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < backups/before-e2e.sql
docker compose up -d backend
```

## Configuration notes

- **Scheduled jobs** (all `America/Toronto`): scheduled-notification dispatch (every minute), class reminders
  (day before, 18:00) and fee reminders (5th of the month, previous month's Unpaid/Partial). Each has
  `JOB_*_ENABLED` and `JOB_*_CRON` variables.
- **Login throttling:** `LOGIN_MAX_FAILURES` (5) failures lock that username for `LOGIN_LOCKOUT` (15m).
- **Google Calendar (optional):** set `GOOGLE_CALENDAR_ENABLED=true` and place a service-account key at
  `secrets/google-credentials.json` (Docker) or point `GOOGLE_CREDENTIALS_PATH` at it (local). Share the
  target calendar with the service account, or let the sync create a calendar named after the term.
  Inviting attendees from a service account requires domain-wide delegation, so it is behind
  `GOOGLE_SEND_INVITES`. When not configured, the sync button is hidden and the API returns 501.

## Implementation decisions and deviations from the spec

- **Build:** Gradle, as in the stack table (the structure diagram mentioned `pom.xml`). `backend/` is a standalone
  Gradle build with its own wrapper; the IntelliJ template project at the repository root was left untouched.
- **Seed groups:** the student table uses `Batch1`/`Batch2` while the schedule uses `Grade 1-4 Core`/`Grade 5-8 Core`,
  so all four groups are seeded. The Core groups have no students, so their slots have 0 recipients until students or
  extra emails are added.
- **Fee status edge case:** a $0 bill with no payments is `PAID` (paid = due) rather than `UNPAID`.
- **Fee generation** skips active students whose enrollment date is after the month, keeps each existing row's rate
  snapshot (rule 3) and any manually entered "classes scheduled" (tracked by a `scheduled_override` column).
- **Payment dates** more than a year before the fee month are rejected (400) rather than merely flagged.
- **Delete policy:** the student page shows **Delete** (no history) or **Deactivate** (has history) with an explanation;
  `DELETE /students/{id}` still returns 409 when history exists.
- **List endpoints:** students and notifications are paginated (`page`/`size`/`sort`); small reference lists
  (grade rates, class groups, slots, templates, fee and feedback rows for a month) return plain arrays.
- **Additional endpoints** beyond section 7: per-student history (`/attendance/students/{id}`, `/fees/students/{id}`,
  `/feedback/students/{id}`), `POST /feedback/copy-topics`, `GET /schedule/occurrences`, `GET /schedule/google-status`,
  term CRUD under `/terms`, and notification helpers (`/notifications/fee-reminders`, `/feedback-email`,
  `/schedule-broadcast`, `/placeholders`, `PUT /notifications/{id}` for drafts).
- **Emails** are plain text; multi-recipient emails BCC parents so addresses are never shared. Automated flows use the
  templates named "Weekly Schedule", "Class Reminder", "Fee Reminder" and "Monthly Feedback", falling back to
  built-in wording when a template is missing.
- **Class Groups** has its own sidebar entry (the spec lists CRUD for groups but no screen).
- **Printable feedback report** uses the browser's print dialog ("Save as PDF").
