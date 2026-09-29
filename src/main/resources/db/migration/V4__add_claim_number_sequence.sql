-- Sequence backing the human-readable claim number (e.g. CLM-2026-000001).
-- The raw integer value is formatted by the application; the sequence only
-- guarantees each claim gets a distinct, ever-increasing number.
CREATE SEQUENCE claim_number_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE claims
    ALTER COLUMN claim_number TYPE VARCHAR(20) USING claim_number::VARCHAR(20);
