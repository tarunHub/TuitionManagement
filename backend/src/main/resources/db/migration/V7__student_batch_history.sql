-- Which batch each student was in, month by month. A row applies from from_month until the student's next row.
-- Moving a student writes a row for the current month only, so earlier months keep the batch they had.
CREATE TABLE student_batch_history (
    student_id     BIGINT NOT NULL REFERENCES student (id) ON DELETE CASCADE,
    from_month     DATE   NOT NULL CHECK (EXTRACT(DAY FROM from_month) = 1),
    class_group_id BIGINT REFERENCES class_group (id),
    PRIMARY KEY (student_id, from_month)
);
CREATE INDEX ix_batch_history_group ON student_batch_history (class_group_id);

-- No history exists before this migration: every student's current batch applies to all earlier months.
INSERT INTO student_batch_history (student_id, from_month, class_group_id)
SELECT id, DATE '1900-01-01', class_group_id FROM student;
