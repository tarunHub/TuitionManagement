-- Multi-centre: every tuition centre's data is tagged with centre_id. Existing data becomes centre 1 ("Smart Start").

CREATE TABLE centre (
    id                    BIGSERIAL PRIMARY KEY,
    name                  VARCHAR(150) NOT NULL,
    student_code_prefix   VARCHAR(10)  NOT NULL,
    next_student_code     BIGINT       NOT NULL DEFAULT 1,
    contact_email         VARCHAR(150),
    contact_phone         VARCHAR(40),
    address               VARCHAR(300),
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Email (SMTP). Passwords are stored encrypted by the application.
    mail_from             VARCHAR(150),
    mail_from_name        VARCHAR(150),
    smtp_host             VARCHAR(150),
    smtp_port             INTEGER,
    smtp_username         VARCHAR(150),
    smtp_password         VARCHAR(1000),
    smtp_auth             BOOLEAN      NOT NULL DEFAULT FALSE,
    smtp_starttls         BOOLEAN      NOT NULL DEFAULT FALSE,
    -- SMS gateway (Android "SMS Gateway" app).
    sms_enabled           BOOLEAN      NOT NULL DEFAULT FALSE,
    sms_gateway_url       VARCHAR(300),
    sms_username          VARCHAR(150),
    sms_password          VARCHAR(1000),
    sms_country_code      VARCHAR(5)   NOT NULL DEFAULT '1',
    -- Set once the app has copied the old .env email/SMS settings into this centre (centre 1 only).
    messaging_imported    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_centre_name ON centre (lower(name));

INSERT INTO centre (id, name, student_code_prefix, next_student_code)
SELECT 1, 'Smart Start', 'SS', CASE WHEN is_called THEN last_value + 1 ELSE last_value END FROM student_code_seq;
SELECT setval('centre_id_seq', 1);
DROP SEQUENCE student_code_seq;

-- Users: platform admins have no centre; centre staff belong to exactly one.
ALTER TABLE admin_user ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'PLATFORM_ADMIN';
ALTER TABLE admin_user ADD COLUMN centre_id BIGINT REFERENCES centre (id);
ALTER TABLE admin_user ALTER COLUMN role DROP DEFAULT;
ALTER TABLE admin_user ADD CONSTRAINT ck_admin_user_role CHECK (
    (role = 'PLATFORM_ADMIN' AND centre_id IS NULL) OR (role = 'STAFF' AND centre_id IS NOT NULL));
CREATE INDEX ix_admin_user_centre ON admin_user (centre_id);

-- Tag every centre-owned table.
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['grade_rate', 'class_group', 'student', 'attendance', 'monthly_fee', 'fee_payment',
                             'monthly_feedback', 'term', 'schedule_slot', 'notification_template', 'notification',
                             'class_session', 'student_batch_history', 'reminder_settings']
    LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN centre_id BIGINT NOT NULL DEFAULT 1 REFERENCES centre (id)', t);
        EXECUTE format('ALTER TABLE %I ALTER COLUMN centre_id DROP DEFAULT', t);
        EXECUTE format('CREATE INDEX ix_%s_centre ON %I (centre_id)', t, t);
    END LOOP;
END $$;

-- Names and codes are unique per centre, not across the platform.
ALTER TABLE grade_rate DROP CONSTRAINT ux_grade_rate_grade;
ALTER TABLE grade_rate ADD CONSTRAINT ux_grade_rate_grade UNIQUE (centre_id, grade);
ALTER TABLE class_group DROP CONSTRAINT ux_class_group_name;
ALTER TABLE class_group ADD CONSTRAINT ux_class_group_name UNIQUE (centre_id, name);
ALTER TABLE student DROP CONSTRAINT ux_student_code;
ALTER TABLE student ADD CONSTRAINT ux_student_code UNIQUE (centre_id, student_code);
ALTER TABLE notification_template DROP CONSTRAINT ux_template_name;
ALTER TABLE notification_template ADD CONSTRAINT ux_template_name UNIQUE (centre_id, name);
DROP INDEX ux_term_single_active;
CREATE UNIQUE INDEX ux_term_single_active ON term (centre_id) WHERE active;

-- Reminder settings: one row per centre instead of a single row with id 1.
ALTER TABLE reminder_settings DROP CONSTRAINT reminder_settings_pkey;
ALTER TABLE reminder_settings DROP CONSTRAINT IF EXISTS reminder_settings_id_check;
ALTER TABLE reminder_settings DROP COLUMN id;
ALTER TABLE reminder_settings ADD COLUMN id BIGSERIAL PRIMARY KEY;
ALTER TABLE reminder_settings ADD CONSTRAINT ux_reminder_settings_centre UNIQUE (centre_id);
