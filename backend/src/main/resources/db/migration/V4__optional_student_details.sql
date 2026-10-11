-- Real enrolment records often lack these (imported from the centre's spreadsheet), so they become optional.
ALTER TABLE student ALTER COLUMN date_of_birth DROP NOT NULL;
ALTER TABLE student ALTER COLUMN primary_contact DROP NOT NULL;
ALTER TABLE student ALTER COLUMN email DROP NOT NULL;
ALTER TABLE student ALTER COLUMN emergency_contact_name DROP NOT NULL;
ALTER TABLE student ALTER COLUMN emergency_contact_phone DROP NOT NULL;
ALTER TABLE student ALTER COLUMN enrollment_date DROP NOT NULL;
