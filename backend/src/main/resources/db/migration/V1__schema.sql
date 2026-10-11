-- Smart Start Student Portal: core schema.
-- Money is NUMERIC(10,2); timestamps are TIMESTAMPTZ; business time zone is America/Toronto.

CREATE TABLE admin_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_admin_user_username ON admin_user (lower(username));

CREATE TABLE grade_rate (
    id             BIGSERIAL PRIMARY KEY,
    grade          VARCHAR(30)    NOT NULL,
    per_class_rate NUMERIC(10, 2) NOT NULL CHECK (per_class_rate > 0),
    notes          TEXT,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ux_grade_rate_grade UNIQUE (grade)
);

CREATE TABLE class_group (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_class_group_name UNIQUE (name)
);

-- Student codes are never reused: the sequence only moves forward.
CREATE SEQUENCE student_code_seq START WITH 1 INCREMENT BY 1 NO CYCLE;

CREATE TABLE student (
    id                      BIGSERIAL PRIMARY KEY,
    student_code            VARCHAR(10)    NOT NULL,
    full_name               VARCHAR(100)   NOT NULL,
    date_of_birth           DATE           NOT NULL,
    grade                   VARCHAR(30)    NOT NULL,
    school                  VARCHAR(150),
    address                 TEXT,
    guardian_name           VARCHAR(100)   NOT NULL,
    primary_contact         VARCHAR(20)    NOT NULL,
    alternate_contact       VARCHAR(20),
    email                   VARCHAR(150)   NOT NULL,
    emergency_contact_name  VARCHAR(100)   NOT NULL,
    emergency_contact_phone VARCHAR(20)    NOT NULL,
    enrollment_date         DATE           NOT NULL,
    per_class_rate          NUMERIC(10, 2) NOT NULL CHECK (per_class_rate >= 0),
    status                  VARCHAR(10)    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    class_group_id          BIGINT REFERENCES class_group (id),
    notes                   TEXT,
    created_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ux_student_code UNIQUE (student_code)
);
CREATE INDEX ix_student_status ON student (status);
CREATE INDEX ix_student_grade ON student (grade);
CREATE INDEX ix_student_group ON student (class_group_id);
CREATE INDEX ix_student_full_name ON student (lower(full_name));

CREATE TABLE student_subject (
    student_id BIGINT      NOT NULL REFERENCES student (id) ON DELETE CASCADE,
    subject    VARCHAR(60) NOT NULL,
    PRIMARY KEY (student_id, subject)
);

CREATE TABLE attendance (
    id         BIGSERIAL PRIMARY KEY,
    student_id BIGINT      NOT NULL REFERENCES student (id),
    class_date DATE        NOT NULL,
    status     VARCHAR(10) NOT NULL CHECK (status IN ('PRESENT', 'ABSENT')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_attendance_student_date UNIQUE (student_id, class_date)
);
CREATE INDEX ix_attendance_date ON attendance (class_date);

CREATE TABLE monthly_fee (
    id                  BIGSERIAL PRIMARY KEY,
    student_id          BIGINT         NOT NULL REFERENCES student (id),
    fee_month           DATE           NOT NULL CHECK (EXTRACT(DAY FROM fee_month) = 1),
    rate                NUMERIC(10, 2) NOT NULL CHECK (rate >= 0),
    classes_held        INT            NOT NULL DEFAULT 0 CHECK (classes_held >= 0),
    classes_scheduled   INT CHECK (classes_scheduled >= 0),
    scheduled_override  BOOLEAN        NOT NULL DEFAULT FALSE,
    amount_due          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    notes               TEXT,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ux_monthly_fee_student_month UNIQUE (student_id, fee_month)
);
CREATE INDEX ix_monthly_fee_month ON monthly_fee (fee_month);

CREATE TABLE fee_payment (
    id             BIGSERIAL PRIMARY KEY,
    monthly_fee_id BIGINT         NOT NULL REFERENCES monthly_fee (id) ON DELETE CASCADE,
    amount         NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    payment_date   DATE           NOT NULL,
    method         VARCHAR(20)    NOT NULL CHECK (method IN ('CASH', 'E_TRANSFER', 'CHEQUE', 'CARD', 'OTHER')),
    reference      VARCHAR(100),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX ix_fee_payment_fee ON fee_payment (monthly_fee_id);

CREATE TABLE monthly_feedback (
    id                BIGSERIAL PRIMARY KEY,
    student_id        BIGINT      NOT NULL REFERENCES student (id),
    feedback_month    DATE        NOT NULL CHECK (EXTRACT(DAY FROM feedback_month) = 1),
    topics_taught     TEXT        NOT NULL,
    improvement_areas TEXT,
    achievements      TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_feedback_student_month UNIQUE (student_id, feedback_month)
);
CREATE INDEX ix_feedback_month ON monthly_feedback (feedback_month);

CREATE TABLE term (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    start_date    DATE         NOT NULL,
    end_date      DATE         NOT NULL,
    calendar_name VARCHAR(150),
    active        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_term_dates CHECK (end_date > start_date)
);
-- Only one active term at a time.
CREATE UNIQUE INDEX ux_term_single_active ON term (active) WHERE active;

CREATE TABLE schedule_slot (
    id              BIGSERIAL PRIMARY KEY,
    term_id         BIGINT       NOT NULL REFERENCES term (id) ON DELETE CASCADE,
    class_group_id  BIGINT       NOT NULL REFERENCES class_group (id),
    day_of_week     VARCHAR(3)   NOT NULL CHECK (day_of_week IN ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN')),
    start_time      TIME         NOT NULL,
    end_time        TIME         NOT NULL,
    subject         VARCHAR(150),
    location        VARCHAR(150),
    teacher         VARCHAR(100),
    extra_emails    TEXT,
    notes           TEXT,
    google_event_id VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_slot_times CHECK (end_time > start_time)
);
CREATE INDEX ix_slot_term ON schedule_slot (term_id);
CREATE INDEX ix_slot_group ON schedule_slot (class_group_id);

CREATE TABLE notification_template (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    type       VARCHAR(20)  NOT NULL CHECK (type IN ('SCHEDULE', 'SCHEDULE_CHANGE', 'CLASS_CANCELLED', 'FEE_REMINDER', 'FEEDBACK', 'GENERAL')),
    subject    VARCHAR(200) NOT NULL,
    body       TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_template_name UNIQUE (name)
);

CREATE TABLE notification (
    id            BIGSERIAL PRIMARY KEY,
    type          VARCHAR(20)  NOT NULL CHECK (type IN ('SCHEDULE', 'SCHEDULE_CHANGE', 'CLASS_CANCELLED', 'FEE_REMINDER', 'FEEDBACK', 'GENERAL')),
    channel       VARCHAR(10)  NOT NULL DEFAULT 'EMAIL' CHECK (channel IN ('EMAIL')),
    subject       VARCHAR(200) NOT NULL,
    body          TEXT         NOT NULL,
    recipients    TEXT         NOT NULL DEFAULT '',
    status        VARCHAR(10)  NOT NULL CHECK (status IN ('DRAFT', 'SCHEDULED', 'SENT', 'FAILED')),
    scheduled_at  TIMESTAMPTZ,
    sent_at       TIMESTAMPTZ,
    error_message TEXT,
    template_id   BIGINT REFERENCES notification_template (id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_notification_status ON notification (status, scheduled_at);
CREATE INDEX ix_notification_created ON notification (created_at DESC);
