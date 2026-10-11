-- Development seed data (dev profile only). All names are fictional; all emails use example.com.

INSERT INTO grade_rate (grade, per_class_rate, notes) VALUES
    ('SK', 10.00, 'Senior Kindergarten'),
    ('Grade 1', 15.00, NULL),
    ('Grade 2', 15.00, NULL),
    ('Grade 3', 15.00, NULL),
    ('Grade 4', 15.00, NULL),
    ('Grade 5', 20.00, NULL),
    ('Grade 6', 20.00, NULL),
    ('Grade 7', 20.00, NULL),
    ('Grade 8', 20.00, NULL);

INSERT INTO class_group (name, description) VALUES
    ('Batch1', 'Senior batch'),
    ('Batch2', 'Junior batch'),
    ('Grade 1-4 Core', 'Core Math & English for Grades 1-4'),
    ('Grade 5-8 Core', 'Core Math, English and Science for Grades 5-8');

INSERT INTO student (student_code, full_name, date_of_birth, grade, school, address, guardian_name, primary_contact,
                     alternate_contact, email, emergency_contact_name, emergency_contact_phone, enrollment_date,
                     per_class_rate, status, class_group_id, notes)
VALUES
    ('SS001', 'Arjun Test', '2013-04-12', 'Grade 7', 'Maple Public School', '1 Test Street, Caledon, ON',
     'Priya Test', '905-555-0101', NULL, 'priya.test@example.com', 'Ravi Test', '905-555-0201', '2025-09-02',
     20.00, 'ACTIVE', (SELECT id FROM class_group WHERE name = 'Batch1'), NULL),
    ('SS002', 'Mia Sample', '2020-06-30', 'SK', 'Oak Ridge Elementary', '2 Sample Road, Caledon, ON',
     'Sofia Sample', '905-555-0102', '905-555-0302', 'sofia.sample@example.com', 'Marco Sample', '905-555-0202', '2025-09-02',
     10.00, 'ACTIVE', (SELECT id FROM class_group WHERE name = 'Batch2'), NULL),
    ('SS003', 'Leo Demo', '2018-11-05', 'Grade 2', 'Oak Ridge Elementary', '3 Demo Avenue, Caledon, ON',
     'Hannah Demo', '905-555-0103', NULL, 'hannah.demo@example.com', 'Peter Demo', '905-555-0203', '2025-10-01',
     15.00, 'ACTIVE', (SELECT id FROM class_group WHERE name = 'Batch2'), NULL),
    ('SS004', 'Zara Example', '2013-01-22', 'Grade 7', 'Maple Public School', '4 Example Crescent, Caledon, ON',
     'Aisha Example', '905-555-0104', NULL, 'aisha.example@example.com', 'Omar Example', '905-555-0204', '2026-01-05',
     20.00, 'ACTIVE', (SELECT id FROM class_group WHERE name = 'Batch1'), NULL),
    ('SS005', 'Noah Placeholder', '2017-08-18', 'Grade 3', 'Cedar Valley School', '5 Placeholder Lane, Caledon, ON',
     'Grace Placeholder', '905-555-0105', NULL, 'grace.placeholder@example.com', 'Liam Placeholder', '905-555-0205', '2026-02-02',
     15.00, 'ACTIVE', (SELECT id FROM class_group WHERE name = 'Batch2'), NULL),
    ('SS006', 'Ava Fixture', '2020-03-09', 'SK', 'Cedar Valley School', '6 Fixture Court, Caledon, ON',
     'Emma Fixture', '905-555-0106', NULL, 'emma.fixture@example.com', 'Noel Fixture', '905-555-0206', '2026-03-02',
     10.00, 'ACTIVE', (SELECT id FROM class_group WHERE name = 'Batch2'), NULL),
    ('SS007', 'Ethan Inactive', '2016-05-14', 'Grade 4', 'Maple Public School', '7 Former Way, Caledon, ON',
     'Olivia Inactive', '905-555-0107', NULL, 'olivia.inactive@example.com', 'Sam Inactive', '905-555-0207', '2025-09-02',
     15.00, 'INACTIVE', (SELECT id FROM class_group WHERE name = 'Batch1'), 'Left in July 2026'),
    ('SS008', 'Lily Former', '2019-12-01', 'Grade 1', 'Oak Ridge Elementary', '8 Former Way, Caledon, ON',
     'Chloe Former', '905-555-0108', NULL, 'chloe.former@example.com', 'Ben Former', '905-555-0208', '2026-04-06',
     15.00, 'INACTIVE', NULL, 'Enrolled but never attended');

SELECT setval('student_code_seq', 8);

INSERT INTO student_subject (student_id, subject)
SELECT s.id, x.subject
FROM student s
JOIN (VALUES ('SS001', 'Math'), ('SS001', 'Science'), ('SS002', 'English'), ('SS003', 'Math'), ('SS003', 'English'),
             ('SS004', 'Math'), ('SS004', 'English'), ('SS004', 'Science'), ('SS005', 'Math'), ('SS006', 'English'),
             ('SS007', 'Math'), ('SS008', 'English')) AS x(code, subject) ON x.code = s.student_code;

-- August 2026 attendance: the first N weekdays of the month per student; every 5th class is an absence.
INSERT INTO attendance (student_id, class_date, status)
SELECT s.id, d.day, CASE WHEN d.rn % 5 = 0 THEN 'ABSENT' ELSE 'PRESENT' END
FROM student s
JOIN (VALUES ('SS001', 13), ('SS002', 16), ('SS003', 12), ('SS004', 8), ('SS005', 6), ('SS006', 8)) AS h(code, held)
    ON h.code = s.student_code
CROSS JOIN LATERAL (
    SELECT g::date AS day, row_number() OVER (ORDER BY g) AS rn
    FROM generate_series('2026-08-01'::date, '2026-08-31'::date, interval '1 day') AS g
    WHERE extract(isodow FROM g) < 6
) d
WHERE d.rn <= h.held;

-- Some July history for the inactive student (blocks hard delete).
INSERT INTO attendance (student_id, class_date, status)
SELECT s.id, d::date, 'PRESENT'
FROM student s, generate_series('2026-07-06'::date, '2026-07-10'::date, interval '1 day') AS d
WHERE s.student_code = 'SS007';

-- August 2026 fees (amount_due = held × rate).
INSERT INTO monthly_fee (student_id, fee_month, rate, classes_held, classes_scheduled, scheduled_override, amount_due, notes)
SELECT s.id, '2026-08-01', s.per_class_rate, h.held, h.held, TRUE, s.per_class_rate * h.held, g.name
FROM student s
JOIN (VALUES ('SS001', 13), ('SS002', 16), ('SS003', 12), ('SS004', 8), ('SS005', 6), ('SS006', 8)) AS h(code, held)
    ON h.code = s.student_code
LEFT JOIN class_group g ON g.id = s.class_group_id;

INSERT INTO fee_payment (monthly_fee_id, amount, payment_date, method, reference)
SELECT f.id, p.amount, p.paid_on::date, p.method, p.reference
FROM monthly_fee f
JOIN student s ON s.id = f.student_id
JOIN (VALUES ('SS001', 260.00, '2026-08-31', 'E_TRANSFER', 'ET-1001'),
             ('SS002', 160.00, '2026-08-30', 'CASH', NULL),
             ('SS003', 180.00, '2026-08-31', 'E_TRANSFER', 'ET-1002'),
             ('SS004', 160.00, '2026-09-02', 'CHEQUE', 'CHQ-204'),
             ('SS005', 100.00, '2026-08-29', 'CASH', NULL),
             ('SS006', 50.00, '2026-08-15', 'CASH', NULL),
             ('SS006', 50.00, '2026-08-31', 'E_TRANSFER', 'ET-1003'))
    AS p(code, amount, paid_on, method, reference) ON p.code = s.student_code
WHERE f.fee_month = '2026-08-01';

INSERT INTO monthly_feedback (student_id, feedback_month, topics_taught, improvement_areas, achievements)
SELECT s.id, '2026-08-01', x.topics, x.improve, x.achieve
FROM student s
JOIN (VALUES
    ('SS001', 'Linear equations, ratios and rates', 'Show all working steps', 'Scored 92% on the ratios quiz'),
    ('SS002', 'Letter sounds, sight words, counting to 50', 'Pencil grip', 'Reads ten sight words independently'),
    ('SS003', 'Addition with regrouping, short reading passages', 'Reading comprehension questions', 'Completed every homework sheet'),
    ('SS004', 'Integers, persuasive writing, cells', 'Paragraph structure', 'Excellent science presentation'),
    ('SS005', 'Multiplication tables 2-5, spelling', 'Times tables speed', 'Mastered the 5 times table'),
    ('SS006', 'Shapes, colours, phonics', 'Listening during group time', 'Wrote first name unaided'))
    AS x(code, topics, improve, achieve) ON x.code = s.student_code;

INSERT INTO term (name, start_date, end_date, calendar_name, active)
VALUES ('Fall 2026', '2026-09-02', '2026-12-19', 'Smart Start Classes', TRUE);

INSERT INTO schedule_slot (term_id, class_group_id, day_of_week, start_time, end_time, subject, location, teacher, notes)
SELECT t.id, g.id, x.dow, x.st::time, x.et::time, x.subject, 'Caledon Centre', 'Tarun', 'Weekly'
FROM term t
JOIN (VALUES ('Grade 1-4 Core', 'THU', '17:00', '18:00', 'Math & English'),
             ('Grade 5-8 Core', 'SAT', '10:00', '11:30', 'Math/Eng/Sci')) AS x(grp, dow, st, et, subject) ON TRUE
JOIN class_group g ON g.name = x.grp
WHERE t.name = 'Fall 2026';

INSERT INTO notification_template (name, type, subject, body) VALUES
('Weekly Schedule', 'SCHEDULE', '{{className}} — weekly class schedule',
'Dear {{guardianName}},

Here is the weekly timetable for {{className}}:

{{scheduleTable}}

Please contact us if you have any questions.

Smart Start Tutoring'),
('Fee Reminder', 'FEE_REMINDER', 'Fee reminder for {{studentName}} — {{month}}',
'Dear {{guardianName}},

This is a friendly reminder that the tuition fee for {{studentName}} for {{month}} is outstanding.

Amount due: {{amountDue}}
Balance remaining: {{balance}}

If you have already paid, please disregard this message.

Smart Start Tutoring'),
('Class Reminder', 'SCHEDULE', 'Reminder: {{className}} class on {{date}}',
'Dear {{guardianName}},

This is a reminder that {{className}} has class on {{date}}:

{{scheduleTable}}

See you there!

Smart Start Tutoring'),
('Monthly Feedback', 'FEEDBACK', '{{studentName}} — monthly feedback for {{month}}',
'Dear {{guardianName}},

Here is {{studentName}}''s progress report for {{month}}.

{{feedback}}

Smart Start Tutoring');
