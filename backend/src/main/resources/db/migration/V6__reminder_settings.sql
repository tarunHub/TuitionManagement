-- Automatic class reminder settings, edited on the Class Calendar page (single row, id = 1).
-- last_run_date stops the once-a-minute check from sending the same day's reminders twice.
CREATE TABLE reminder_settings (
    id            SMALLINT    PRIMARY KEY CHECK (id = 1),
    enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
    days_before   SMALLINT    NOT NULL DEFAULT 0 CHECK (days_before BETWEEN 0 AND 2),
    send_time     TIME        NOT NULL DEFAULT '08:00',
    send_email    BOOLEAN     NOT NULL DEFAULT TRUE,
    send_sms      BOOLEAN     NOT NULL DEFAULT TRUE,
    last_run_date DATE,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO reminder_settings (id) VALUES (1);
