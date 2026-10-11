-- Monthly class calendar: the dates each batch actually has class. The attendance grid shows these as "S" (scheduled)
-- until the admin marks each student P or A; fees still count only marked classes.
CREATE TABLE class_session (
    id             BIGSERIAL PRIMARY KEY,
    class_group_id BIGINT       NOT NULL REFERENCES class_group (id),
    class_date     DATE         NOT NULL,
    start_time     TIME         NOT NULL,
    end_time       TIME         NOT NULL,
    location       VARCHAR(150),
    notes          VARCHAR(500),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_class_session_group_date UNIQUE (class_group_id, class_date),
    CONSTRAINT ck_class_session_times CHECK (end_time > start_time)
);
CREATE INDEX ix_class_session_date ON class_session (class_date);
