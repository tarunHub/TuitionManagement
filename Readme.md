# Smart Start — Student Portal Management System

**Build specification for a full-stack CRUD application**

This document is a complete brief for building the Student Portal. It covers the product scope, data model, business rules, backend (Java Spring Boot) and frontend (React + TypeScript) design, API contract, and acceptance criteria. Build it in the phases listed at the end, and treat every "Rule" and "Acceptance criteria" item as a requirement.

---

## 1. Overview

A single-centre tutoring business needs to replace its spreadsheets with a web portal. An admin logs in and manages:

1. Students (full CRUD)
2. Grade-based fee rates
3. Daily attendance (monthly grid)
4. Monthly fees and payments
5. Monthly student feedback
6. A live dashboard
7. Class schedule (with optional Google Calendar sync)
8. Parent notifications (email)

There is one role, **ADMIN**. Parents and students do not log in (out of scope for v1).

---

## 2. Tech Stack

### Backend

| Concern | Choice                                                    |
|---|-----------------------------------------------------------|
| Language / runtime | Java 21                                                   |
| Framework | Spring Boot 3.3+                                          |
| Build | Gradle                                                    |
| Web | Spring Web (REST, JSON)                                   |
| Security | Spring Security 6 + JWT (stateless), BCrypt password hashing |
| Persistence | Spring Data JPA (Hibernate)                               |
| Database | PostgreSQL 16 (H2 allowed only for unit tests)            |
| Migrations | Flyway                                                    |
| Validation | Jakarta Bean Validation                                   |
| Mapping | MapStruct (entity ↔ DTO)                                  |
| Boilerplate | Lombok                                                    |
| API docs | springdoc-openapi (Swagger UI at `/swagger-ui.html`)      |
| Email | Spring Boot Mail (SMTP; MailHog for local dev)            |
| Scheduling | Spring `@Scheduled`                                       |
| Calendar (optional) | Google Calendar API Java client                           |
| Export | Apache POI (Excel export of attendance/fees)              |
| Testing | JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL) |

### Frontend

| Concern | Choice | Why |
|---|---|---|
| Framework | React 18 + TypeScript | Mature ecosystem, strong typing |
| Build tool | Vite | Fast dev server and builds |
| Routing | React Router v6 | Standard, supports protected routes |
| Server state | TanStack Query | Caching, refetching, mutation states |
| Forms | React Hook Form + Zod | Performant forms with schema validation |
| UI components | shadcn/ui + Tailwind CSS | Accessible, customizable, no heavy theme lock-in |
| Tables / grids | TanStack Table | Sorting, filtering, editable cells |
| Charts | Recharts | Simple dashboard charts |
| HTTP | Axios with JWT interceptor | Central auth/error handling |
| Dates | date-fns | Lightweight date math |
| Notifications (UI) | sonner (toasts) | Success/error feedback |
| Testing | Vitest + React Testing Library; Playwright (TypeScript) for E2E | |

### DevOps

- Docker Compose with `postgres`, `mailhog`, `backend`, `frontend`
- `.env.example` for all secrets (DB credentials, JWT secret, SMTP, Google credentials path)
- No secrets committed to the repo

---

## 3. Repository Structure

```
student-portal/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/smartstart/portal/
│       │   ├── config/          # Security, CORS, OpenAPI, Mail config
│       │   ├── security/        # JWT filter, token service, UserDetailsService
│       │   ├── common/          # Error handling, base entity, pagination DTOs
│       │   ├── auth/
│       │   ├── student/
│       │   ├── graderate/
│       │   ├── classgroup/
│       │   ├── attendance/
│       │   ├── fee/
│       │   ├── feedback/
│       │   ├── schedule/
│       │   ├── notification/
│       │   └── dashboard/
│       │       # each module: controller/, service/, repository/, entity/, dto/, mapper/
│       └── main/resources/
│           ├── application.yml
│           ├── application-dev.yml
│           └── db/migration/    # Flyway V1__init.sql, V2__seed.sql ...
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── api/                 # axios client + typed API functions per module
│       ├── components/          # shared UI (DataTable, ConfirmDialog, PageHeader...)
│       ├── features/            # students/, fees/, attendance/, feedback/, schedule/, notifications/, dashboard/
│       ├── hooks/
│       ├── lib/                 # utils, money formatting, date helpers
│       ├── routes/              # router + ProtectedRoute
│       ├── types/               # shared TS types mirroring backend DTOs
│       └── main.tsx
├── e2e/                         # Playwright TypeScript tests
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## 4. Data Model

All tables have `id` (BIGSERIAL PK), `created_at`, `updated_at`. Money is stored as `NUMERIC(10,2)` and handled as `BigDecimal` in Java. Never use `double` for money.

### 4.1 `admin_user`
| Field | Type | Notes |
|---|---|---|
| username | varchar(50) | unique |
| password_hash | varchar | BCrypt |
| full_name | varchar(100) | |
| enabled | boolean | |

### 4.2 `grade_rate` (Fee Management)
| Field | Type | Notes |
|---|---|---|
| grade | varchar(30) | unique, e.g. `SK`, `Grade 1` … `Grade 12` |
| per_class_rate | numeric(10,2) | > 0 |
| notes | text | |

### 4.3 `class_group`
Represents a batch/class (e.g. `Batch1`, `Grade 1-4 Core`).
| Field | Type | Notes |
|---|---|---|
| name | varchar(100) | unique |
| description | text | |

### 4.4 `student`
| Field | Type | Rule |
|---|---|---|
| student_code | varchar(10) | unique, auto-generated `SS001`, `SS002`…, read-only after creation |
| full_name | varchar(100) | required |
| date_of_birth | date | optional, must be in the past |
| grade | varchar(30) | required, must exist in `grade_rate` |
| school | varchar(150) | |
| subjects_enrolled | varchar(255) | comma-separated or a join table `student_subject` (preferred) |
| address | text | |
| guardian_name | varchar(100) | required |
| primary_contact | varchar(20) | optional, phone format (used for SMS) |
| alternate_contact | varchar(20) | optional |
| email | varchar(150) | optional, valid email (parent email, used for notifications; parents without one are skipped) |
| sms_opt_in | boolean | default `false`; parent consented to SMS reminders at `primary_contact` |
| emergency_contact_name | varchar(100) | optional |
| emergency_contact_phone | varchar(20) | optional, phone format |
| enrollment_date | date | optional (blank = billed from any month) |
| per_class_rate | numeric(10,2) | defaults from `grade_rate` on create; editable override |
| status | enum | `ACTIVE`, `INACTIVE` |
| class_group_id | FK → class_group | optional |
| notes | text | |

### 4.5 `attendance`
| Field | Type | Rule |
|---|---|---|
| student_id | FK | |
| class_date | date | |
| status | enum | `PRESENT`, `ABSENT` |

Unique constraint on `(student_id, class_date)`. **No row = no class that day** (blank cell in the grid).

### 4.6 `monthly_fee`
One row per student per month.
| Field | Type | Rule |
|---|---|---|
| student_id | FK | |
| fee_month | date | first day of month, e.g. `2026-08-01`; unique with student_id |
| rate | numeric | snapshot of student's per_class_rate when generated |
| classes_held | int | computed: count of attendance rows (P + A) in the month |
| classes_scheduled | int | computed from schedule for the student's group, or entered manually |
| amount_due | numeric | `classes_held × rate` |
| notes | text | e.g. batch name |

### 4.7 `fee_payment`
Supports multiple payments per month.
| Field | Type | Rule |
|---|---|---|
| monthly_fee_id | FK | |
| amount | numeric | > 0 |
| payment_date | date | not in the future; must be within a reasonable range (flag dates more than 1 year before fee_month) |
| method | enum | `CASH`, `E_TRANSFER`, `CHEQUE`, `CARD`, `OTHER` |
| reference | varchar | optional |

Derived (not stored, computed in service/DTO):
- `amount_paid = SUM(fee_payment.amount)`
- `balance = amount_due − amount_paid`
- `status`: `UNPAID` (paid = 0), `PARTIAL` (0 < paid < due), `PAID` (paid = due), `CREDIT` (paid > due, negative balance)

### 4.8 `monthly_feedback`
| Field | Type | Rule |
|---|---|---|
| student_id | FK | |
| feedback_month | date | first of month; unique with student_id |
| topics_taught | text | required |
| improvement_areas | text | |
| achievements | text | |

### 4.9 `term`
| Field | Type | Rule |
|---|---|---|
| name | varchar | e.g. `Fall 2026` |
| start_date | date | required |
| end_date | date | must be after start_date |
| calendar_name | varchar | e.g. `Smart Start Classes` |
| active | boolean | only one active term at a time |

### 4.10 `schedule_slot`
| Field | Type | Rule |
|---|---|---|
| term_id | FK | |
| class_group_id | FK | |
| day_of_week | enum | `MON`…`SUN` |
| start_time | time | |
| end_time | time | must be after start_time |
| subject | varchar | |
| location | varchar | |
| teacher | varchar | |
| extra_emails | text | comma-separated, validated; added to the group's parent emails |
| notes | text | e.g. `Weekly` |
| google_event_id | varchar | set after calendar sync |

Recipients for a slot = emails of ACTIVE students in the slot's class group + `extra_emails` (de-duplicated).

### 4.11 `notification`
| Field | Type | Rule |
|---|---|---|
| type | enum | `SCHEDULE`, `SCHEDULE_CHANGE`, `CLASS_CANCELLED`, `FEE_REMINDER`, `FEEDBACK`, `GENERAL` |
| channel | enum | `EMAIL`, `SMS` |
| subject | varchar | |
| body | text | |
| recipients | text | resolved email list, or E.164 phone numbers for SMS |
| status | enum | `DRAFT`, `SCHEDULED`, `SENT`, `FAILED` |
| scheduled_at | timestamp | nullable |
| sent_at | timestamp | nullable |
| error_message | text | nullable |

### 4.12 `notification_template`
Reusable templates with placeholders: `{{guardianName}}`, `{{studentName}}`, `{{month}}`, `{{amountDue}}`, `{{balance}}`, `{{scheduleTable}}`, `{{className}}`, `{{date}}`.

---

## 5. Business Rules

1. **Student code** is generated server-side by sequence: `SS` + zero-padded 3 digits (`SS001`). Never reused, even after delete.
2. **Delete policy:** students with attendance, fee, or feedback history are **soft-deactivated** (status → INACTIVE) rather than hard-deleted. Hard delete is allowed only when there is no related history. The UI must explain this in the confirm dialog.
3. **Rate resolution:** a new student's `per_class_rate` defaults to their grade's rate. Changing a grade's rate does **not** retroactively change existing students or already-generated monthly fees (rates are snapshotted).
4. **Billing:** `amount_due = classes_held × rate`. **Absences are still billed.** A blank day (no attendance row) is not billed.
5. **Fee generation:** "Generate fees for month" creates/updates a `monthly_fee` row for every ACTIVE student, recomputing `classes_held` from attendance. Re-running must be idempotent and must never delete payments.
6. **Overpayment** results in a negative balance and status `CREDIT` (displayed as e.g. `-$10`). It is not carried forward automatically in v1.
7. **Attendance** can only be marked for ACTIVE students and not for dates after today.
8. **Feedback** is one record per student per month.
9. **Term dates** bound schedule generation and calendar sync; no events are created outside `start_date`–`end_date`.
10. All times are in `America/Toronto`.

---

## 6. Features and Screens

### 6.1 Admin Login
- `/login` page: username + password, show/hide password, error message on failure.
- On success, store the JWT (in memory + `sessionStorage`), redirect to `/dashboard`.
- Token expiry: 8 hours. On a 401, clear the token and redirect to login.
- Logout button in the top bar.
- Seed one admin from environment variables (`ADMIN_USERNAME`, `ADMIN_PASSWORD`) on first startup. Never hardcode credentials.
- Rate-limit login attempts (e.g. 5 failures → 15-minute lockout).

### 6.2 Students
- `/students`: paginated, searchable (name, ID, guardian, email), filterable by status/grade/group, sortable table.
- Columns: Student ID, Full Name, Grade, Group, Guardian, Primary Contact, Email, Rate, Status.
- `/students/new` and `/students/:id/edit`: form with all fields from 4.4, grouped into sections: *Student*, *Guardian & Contacts*, *Emergency*, *Enrollment & Billing*, *Notes*.
- `/students/:id`: detail view with tabs: Profile, Attendance history, Fees history, Feedback history.
- Delete / deactivate with a confirmation dialog.
- Export students to CSV.

### 6.3 Fee Rates (Grade Rates)
- `/fees/rates`: inline-editable table of Grade, Per-Class Rate ($), Notes.
- Add / edit / delete. A grade in use by students cannot be deleted (show which students).

### 6.4 Attendance
- `/attendance?month=2026-08`: **monthly grid**, mirroring the spreadsheet.
    - Header row 1: day-of-week letters (S, M, T, W, T, F, S). Header row 2: day numbers 1–31.
    - Rows: Student ID, Name (ACTIVE students; optionally filter by class group).
    - Each cell cycles on click: blank → P → A → blank. Keyboard support: `P`, `A`, `Delete`, and arrow keys.
    - Weekend columns lightly shaded; future dates disabled.
    - Right-hand totals per student: Present, Absent, Held (P+A), Attendance %.
    - Bottom row: number of students present per day.
    - Changes are batched and saved with a "Save" button (or autosave with debounce) using a bulk API.
    - "Mark all present for this day" shortcut on each column header.
- Month picker (previous/next).
- Export the month to Excel.

### 6.5 Monthly Fees
- `/fees?month=2026-08`: table matching the spreadsheet:
  Student ID, Name, Grade, Rate ($/class), Classes Held (billed), Classes Scheduled, Amount Due, Amount Paid, Balance, Status, Last Payment Date, Method, Notes.
- **Generate / Refresh fees** button for the month (rule 5).
- **Record payment** dialog: amount, date, method, reference. Show payment history per row and allow editing or deleting a payment.
- Status badges: Paid (green), Partial (amber), Unpaid (red), Credit (blue).
- Footer **TOTAL** row: sum of Amount Due, Amount Paid, Balance.
- Filters: status, group. Export to Excel.
- "Send fee reminder" action for selected Unpaid/Partial rows (creates a FEE_REMINDER notification).

### 6.6 Monthly Feedback
- `/feedback?month=2026-08`: table of Student ID, Name, Topics Taught, Improvement Areas, Achievements.
- Edit in a side drawer with multiline text areas.
- "Copy topics from previous month" helper.
- Optional: "Email feedback to parent" (creates a FEEDBACK notification).
- Optional: printable/PDF report per student.
- **Write with AI** (when the assistant is on, see 6.11): the drawer has a notes box; the admin types rough notes ("did fractions, mixes up numerator/denominator, faster at times tables") and the local model rewrites them, plus anything already in the three boxes, into parent-ready wording. It fills the boxes only; nothing is saved until **Save feedback**, and **Undo draft** restores the previous text. The model is told to use only the notes (no invented marks or topics), keep every point, and refer to the student by first name rather than guessing pronouns.

### 6.7 Dashboard (landing page after login)
Month selector at the top (defaults to the current month).

**Students card**
- Active students
- Inactive students

**Fees card (selected month)**
- Fees billed (due) = Σ amount_due
- Fees collected = Σ payments recorded against that month's fees
- Outstanding balance = billed − collected
- Collection rate = collected ÷ billed (show "—" when billed is 0)

**Attendance card (selected month)**
- Classes held (max) = max classes_held across students
- Average attendance = total Present ÷ total (Present + Absent)

**Monthly summary (selected year)**
- Table: Month, Billed, Collected, Outstanding for Jan–Dec, plus a YEAR TOTAL row.
- Bar chart of Billed vs Collected by month.

**Extras**
- Upcoming classes this week (from the schedule)
- Students with outstanding balances (top 5)
- Feedback not yet entered for this month (count + link)

### 6.8 Schedule
- `/schedule`: term config card (Term Start, Term End, Calendar Name, Active).
- Slots table: Group/Class, Day, Start, End, Subject, Location, Teacher, Recipients (resolved count, expandable), Notes. Full CRUD.
- Weekly calendar view (Mon–Sun columns) showing slots.
- Validation: end after start; warn on overlapping slots at the same location.
- **Google Calendar sync (optional, feature-flagged):** "Sync to Google Calendar" creates/updates recurring weekly events in the named calendar between term start/end, invites recipient emails, and stores `google_event_id`. Deleting a slot deletes its event. If credentials aren't configured, the button is hidden and the API returns 501 with a clear message.

### 6.9 Notifications
- `/notifications`: list with type, subject, recipients count, status, sent/scheduled time.
- **Compose:** pick a type and template, pick recipients (all active parents, by class group, or individual students), preview the rendered email, then **Send now** or **Schedule**.
- **Schedule notifications:** one click sends each group's parents their weekly timetable (`{{scheduleTable}}`).
- **Automatic jobs** (`@Scheduled`, configurable, can be disabled):
    - Class reminder: the day before each class, at 6 PM, to that group's parents.
    - Fee reminder: on the 5th of each month, for previous-month Unpaid/Partial fees.
- Template management screen (CRUD for `notification_template`).
- Failed sends show the error and a **Retry** button.
- All sending goes through a `NotificationSender` interface: `EmailNotificationSender` and `SmsGatewayNotificationSender` (see 6.10).

### 6.10 SMS (Android phone gateway)
Text messages are sent from the centre's own Android phone and mobile plan using the free, open-source
[SMS Gateway for Android](https://sms-gate.app) app, so there is no per-message cost with an unlimited-text plan.
- **Consent first:** a parent is only ever texted when **SMS reminders** is ticked on the student form (`sms_opt_in`). Texts go to the student's `primary_contact`, normalised to E.164 (numbers without `+` get `SMS_DEFAULT_COUNTRY_CODE`, default `1`).
- **Compose:** choose **Email** or **SMS**. For SMS the subject becomes a *title* shown only in the history list; only the body is texted. The form shows the length and the approximate number of SMS per parent. Extra phone numbers can be added. If none of the selected parents opted in, sending is refused.
- **Automatic reminders:** when SMS is configured, the class reminder and fee reminder jobs (and the Fees page's **Send fee reminder** button) also text opted-in parents a short message, in addition to the email. Override the wording by creating templates named **"Class Reminder SMS"** / **"Fee Reminder SMS"** (placeholders include `{{time}}` and `{{location}}`). Weekly schedules and monthly feedback stay email-only because they are too long for SMS.
- **Configuration** (`.env`): `SMS_ENABLED`, `SMS_GATEWAY_URL` (the phone's local server `http://<phone-ip>:8080/message`, or the app's cloud relay `https://api.sms-gate.app/3rdparty/v1/messages`), `SMS_GATEWAY_USERNAME`, `SMS_GATEWAY_PASSWORD`, `SMS_DEFAULT_COUNTRY_CODE`. When not configured the SMS option shows a hint and nothing is sent. Setup steps are in `RUNNING.md`.
- **Delivery status:** "Sent" means the phone accepted the message into its queue. Failures (phone off, wrong credentials) are recorded with the gateway's error and can be retried.

### 6.11 Ask (data assistant)
`/ask` lets the admin ask plain-English questions ("Who still owes fees this month?", "How is Kaya doing?") and get
short answers, with follow-up questions in the same conversation.
- **Popup:** every screen also has a round **Ask** button at the bottom right that opens the same assistant in a small panel (Esc or × closes it; ⤢ opens the full page). The popup and the Ask page share one conversation for the browser tab. The button is hidden when `CHAT_ENABLED=false`.
- **Local model:** answered by a model running in [Ollama](https://ollama.com) on the same computer (default `qwen3.5:latest`), so no data leaves the machine and there is no per-question cost.
- **Read-only tools, not SQL:** the model never writes SQL. It can only call fixed lookups backed by the existing services (`get_fees`, `get_monthly_fee_summary`, `get_student_counts`, `list_students`, `get_student`, `get_attendance`, `get_feedback`, `get_schedule`), so balances, statuses and totals are exactly what the screens show. The tools return ready-made counts and lists because small models count poorly.
- **Grounding:** if the model answers without looking anything up, the backend reminds it once; an answer that still isn't based on a lookup is marked "Not looked up in your data". Every answer lists the screens its numbers came from as links.
- **Configuration:** `CHAT_ENABLED`, `CHAT_MODEL`, `OLLAMA_URL` (Docker uses `host.docker.internal:11434`), `CHAT_TIMEOUT`, `CHAT_MAX_ROUNDS`. When Ollama or the model is missing, the page says how to fix it.
- **Accuracy check:** `tools/chat_eval.py` asks 20 typical questions with known answers against the real model.

---

## 7. REST API

Base path: `/api/v1`. JSON only. All endpoints except login require `Authorization: Bearer <jwt>`.

List endpoints support `page`, `size`, `sort` and return:
```json
{ "content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }
```

Errors use a consistent shape (RFC 7807 style):
```json
{ "status": 400, "error": "VALIDATION_FAILED", "message": "Validation failed",
  "fieldErrors": [{ "field": "email", "message": "must be a valid email" }],
  "timestamp": "2026-08-31T10:00:00Z", "path": "/api/v1/students" }
```

| Module | Method & Path | Purpose |
|---|---|---|
| Auth | `POST /auth/login` | Returns `{ token, expiresAt, user }` |
| | `GET /auth/me` | Current admin |
| | `POST /auth/change-password` | |
| Students | `GET /students?search=&status=&grade=&groupId=` | List |
| | `GET /students/{id}` | Detail |
| | `POST /students` | Create (code auto-generated) |
| | `PUT /students/{id}` | Update |
| | `PATCH /students/{id}/status` | Activate/deactivate |
| | `DELETE /students/{id}` | Hard delete if no history, else 409 with a message |
| | `GET /students/export` | CSV |
| Grade rates | `GET/POST /grade-rates`, `PUT/DELETE /grade-rates/{id}` | CRUD |
| Class groups | `GET/POST /class-groups`, `PUT/DELETE /class-groups/{id}` | CRUD |
| Attendance | `GET /attendance?month=YYYY-MM&groupId=` | Grid data: students × days + totals |
| | `PUT /attendance/bulk` | Body: `[{ studentId, date, status \| null }]` (null removes) |
| | `GET /attendance/export?month=` | Excel |
| Fees | `GET /fees?month=YYYY-MM&status=&groupId=` | Rows + totals |
| | `POST /fees/generate?month=YYYY-MM` | Idempotent generate/refresh |
| | `PUT /fees/{id}` | Update classes_scheduled, notes, rate |
| | `POST /fees/{id}/payments` | Record payment |
| | `PUT /fees/{id}/payments/{paymentId}` | Edit payment |
| | `DELETE /fees/{id}/payments/{paymentId}` | Delete payment |
| | `GET /fees/export?month=` | Excel |
| Feedback | `GET /feedback?month=YYYY-MM` | List (all active students, empty fields if not entered) |
| | `PUT /feedback/{studentId}?month=YYYY-MM` | Upsert |
| | `DELETE /feedback/{id}` | |
| Dashboard | `GET /dashboard?month=YYYY-MM` | Cards |
| | `GET /dashboard/monthly-summary?year=YYYY` | 12 months + totals |
| Schedule | `GET/PUT /terms/active` | Term config |
| | `GET/POST /schedule-slots`, `PUT/DELETE /schedule-slots/{id}` | CRUD |
| | `GET /schedule-slots/{id}/recipients` | Resolved email list |
| | `POST /schedule/sync-google` | Calendar sync (501 if disabled) |
| Notifications | `GET /notifications`, `GET /notifications/{id}` | |
| | `POST /notifications` | Create (draft / send now / schedule) |
| | `POST /notifications/preview` | Render template with sample data |
| | `POST /notifications/{id}/send`, `POST /notifications/{id}/retry` | |
| | `DELETE /notifications/{id}` | Only DRAFT/SCHEDULED |
| | `GET /notifications/channels` | `{ email, sms, smsMessage }`: whether SMS is configured |
| Assistant | `POST /chat` | Body `{ messages: [{ role: USER\|ASSISTANT, content }] }` (last = new question) → `{ answer, sources, grounded, toolsUsed, model, durationMs }` |
| | `GET /chat/status` | `{ enabled, available, model, message }` |
| | `POST /chat/feedback-draft` | Body `{ studentId, month, notes, topicsTaught, improvementAreas, achievements }` → suggested `{ topicsTaught, improvementAreas, achievements, model, durationMs }` (nothing saved) |
| | `GET/POST/PUT/DELETE /notification-templates[/{id}]` | Templates |

HTTP status codes: 200/201/204 on success; 400 validation; 401 unauthenticated; 403 forbidden; 404 not found; 409 conflicts (duplicates, delete with history); 501 feature not configured.

---

## 8. Backend Development Guidelines

- **Layering:** Controller (DTOs only, no entities) → Service (business rules, `@Transactional`) → Repository. Entities never leave the service layer.
- **DTOs:** separate `CreateXRequest`, `UpdateXRequest`, `XResponse` records with Bean Validation annotations.
- **Global exception handler** (`@RestControllerAdvice`) producing the error shape in section 7.
- **Security:**
    - Stateless JWT; `SecurityFilterChain` permits only `/api/v1/auth/login`, Swagger, and health.
    - CORS allows the frontend origin from config (`http://localhost:5173` in dev).
    - BCrypt strength 12. JWT secret from env, at least 256 bits.
- **Calculations** live in one place (`FeeCalculator`, `DashboardService`) with unit tests for each rule, including the zero-billed case for collection rate.
- **Queries:** avoid N+1; use projections or aggregate queries for the attendance grid, fees list, and dashboard.
- **Flyway:** `V1__schema.sql` (all tables, constraints, indexes), `V2__seed_dev.sql` (dev profile only).
- **Auditing:** `@CreatedDate` / `@LastModifiedDate` via JPA auditing.
- **Logging:** SLF4J. Never log tokens, passwords, or full parent contact details.
- **Actuator:** `/actuator/health` only.
- **Tests:**
    - Unit: services and calculators (fee status, balance, collection rate, average attendance, student code generation, delete policy).
    - Integration: controllers with Testcontainers PostgreSQL and MockMvc, including auth (401 without token).
    - Target 80%+ line coverage on service packages.

---

## 9. Frontend Development Guidelines

- **App shell:** left sidebar (Dashboard, Students, Attendance, Fees, Fee Rates, Feedback, Schedule, Notifications), top bar with month picker (where relevant), admin name, logout. Responsive; the sidebar collapses on mobile.
- **Routing:** all routes except `/login` wrapped in `<ProtectedRoute>`. Unknown routes → 404 page.
- **API layer:** a single Axios instance that adds the bearer token and handles 401 globally. One typed module per backend module (`studentsApi.ts`, etc.).
- **Data fetching:** TanStack Query with query keys like `['students', filters]`, `['attendance', month, groupId]`. Invalidate related queries after mutations (e.g. recording a payment invalidates `fees` and `dashboard`).
- **Forms:** React Hook Form + Zod schemas that mirror backend validation. Show server-side `fieldErrors` against the right fields.
- **Tables:** a reusable `DataTable` built on TanStack Table (search, sort, pagination, column visibility, empty and loading states).
- **Attendance grid:** dedicated component; keep local edits in state, show a "N unsaved changes" indicator, warn before navigating away with unsaved changes, sticky header row and first two columns, horizontal scroll.
- **Money and dates:** a shared `formatMoney()` (shows `-$10` for credits) and `formatDate()`; month params always `YYYY-MM`.
- **UX:** toasts for success/error, skeleton loaders, confirm dialogs for destructive actions, disabled buttons while submitting.
- **Accessibility:** labelled inputs, keyboard-navigable grid, sufficient color contrast; status is never conveyed by color alone (badges include text).
- **Testability:** add `data-testid` attributes on key controls (forms, grid cells, table rows, buttons) to support Playwright tests.
- **Tests:** Vitest + RTL for calculators, formatters, forms, and the attendance grid's cell cycling.

---

## 10. End-to-End Tests (Playwright, TypeScript)

Located in `/e2e`, run against Docker Compose:
- Login success and failure; redirect to login when the token is missing.
- Student CRUD: create, edit, deactivate, and delete-blocked-with-history.
- Attendance: mark P/A for a month, save, reload, verify totals.
- Fees: generate the month, record full/partial/over payments, verify status badges and the TOTAL row.
- Dashboard: values match the fees and attendance data entered.
- Notification: compose and send; verify delivery through the MailHog API.
- Ask: against a scripted fake Ollama (`e2e/fake-ollama`) that requests real lookups, verify the answer carries the database's numbers, links to its source screen, and flags answers given without a lookup.
- SMS: opt a parent in on the student form, compose and send an SMS, and verify the request through the `sms-sink` stand-in for the phone gateway; a parent who has not opted in is never texted.

Use the API to seed and clean test data, and store authenticated state for reuse.

---

## 11. Seed Data (dev profile)

Use **fictional** names and `example.com` emails only, never real student or parent data.

**Grade rates**

| Grade | Rate |
|---|---|
| SK | $10 |
| Grade 1–4 | $15 |
| Grade 5–8 | $20 |

Use individual grade rows: SK, Grade 1 … Grade 8.

**Class groups:** `Batch1`, `Batch2`

**Students (6 active, plus 2 inactive):**

| ID | Name | Grade | Group | Rate |
|---|---|---|---|---|
| SS001 | Arjun Test | Grade 7 | Batch1 | $20 |
| SS002 | Mia Sample | SK | Batch2 | $10 |
| SS003 | Leo Demo | Grade 2 | Batch2 | $15 |
| SS004 | Zara Example | Grade 7 | Batch1 | $20 |
| SS005 | Noah Placeholder | Grade 3 | Batch2 | $15 |
| SS006 | Ava Fixture | SK | Batch2 | $10 |

**August 2026 fees**, to reproduce the expected dashboard numbers:

| ID | Held | Due | Paid | Balance | Status |
|---|---|---|---|---|---|
| SS001 | 13 | $260 | $260 | $0 | Paid |
| SS002 | 16 | $160 | $160 | $0 | Paid |
| SS003 | 12 | $180 | $180 | $0 | Paid |
| SS004 | 8 | $160 | $160 | $0 | Paid |
| SS005 | 6 | $90 | $100 | −$10 | Credit |
| SS006 | 8 | $80 | $100 | −$20 | Credit |
| **Total** | | **$930** | **$960** | **−$30** | |

Generate matching August attendance rows so `classes_held` equals these values (mix in a few absences). Expected dashboard for August 2026: billed $930, collected $960, outstanding −$30, collection rate 103%, classes held (max) 16.

**Schedule (Fall 2026 term, 2026-09-02 → 2026-12-19, calendar "Smart Start Classes")**

| Group | Day | Start | End | Subject | Location | Teacher |
|---|---|---|---|---|---|---|
| Grade 1-4 Core | Thu | 17:00 | 18:00 | Math & English | Caledon Centre | Tarun |
| Grade 5-8 Core | Sat | 10:00 | 11:30 | Math/Eng/Sci | Caledon Centre | Tarun |

Add sample monthly feedback for August 2026 for each seeded student, and two notification templates (Weekly Schedule, Fee Reminder).

---

## 12. Acceptance Criteria (Definition of Done)

- [ ] `docker compose up` starts the DB, MailHog, backend, and frontend; the app is usable at `http://localhost:5173` with seeded data.
- [ ] Admin can log in and log out; all API routes reject requests without a valid token.
- [ ] Full CRUD works for students, grade rates, class groups, payments, feedback, schedule slots, terms, notifications, and templates.
- [ ] Attendance grid matches the spreadsheet layout, saves in bulk, and totals are correct.
- [ ] Fee generation is idempotent; due, paid, balance, and status follow section 5; the TOTAL row is correct.
- [ ] Dashboard numbers for August 2026 match section 11 exactly.
- [ ] Notifications send to MailHog; failures are recorded and retryable; scheduled jobs can be toggled in config.
- [ ] Google Calendar sync works when configured and degrades gracefully when it isn't.
- [ ] Swagger UI documents every endpoint.
- [ ] Backend unit and integration tests pass; frontend unit tests pass; Playwright E2E suite passes.
- [ ] No secrets in the repo; `.env.example` documents every variable.

---

## 13. Build Order

Build and verify each phase before moving on:

1. **Foundation:** repo structure, Docker Compose, Spring Boot skeleton, Flyway schema, security and JWT login, React app shell, login page, protected routes.
2. **Students, grade rates, class groups:** backend CRUD + tests, frontend list/form/detail pages.
3. **Attendance:** grid API (read + bulk write), grid UI, export.
4. **Fees:** generation, payments, statuses, totals, UI, export.
5. **Feedback:** API + UI.
6. **Dashboard:** aggregate queries, cards, monthly summary, chart.
7. **Schedule:** term + slots CRUD, weekly view, optional Google Calendar sync.
8. **Notifications:** templates, compose/preview/send, MailHog, scheduled jobs, retry.
9. **Hardening:** seed data, Playwright E2E, README run instructions, final acceptance checklist.

At the end of each phase, list what was built, how to run it, and any assumptions made.

---

## 14. Out of Scope (v1)

Parent/student logins, online payments, multi-centre support, multiple admin roles, and automatic credit carry-forward. Design the code so these can be added later; the `NotificationSender` interface and role-based security are already in place.