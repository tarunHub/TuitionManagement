-- V3 (not V2): the dev seed in db/seed already uses version 2.
-- SMS notifications via an Android phone gateway, sent only to parents who opted in.
ALTER TABLE notification DROP CONSTRAINT notification_channel_check;
ALTER TABLE notification ADD CONSTRAINT notification_channel_check CHECK (channel IN ('EMAIL', 'SMS'));

ALTER TABLE student ADD COLUMN sms_opt_in BOOLEAN NOT NULL DEFAULT FALSE;
