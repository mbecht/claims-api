-- Test-only fixture for ClaimPagingIntegrationTest. Loaded with @Sql on each test, inside the
-- test's rolled-back transaction, so it never reaches the dev database or other tests.
-- One policyholder and two policies, each with 25 claims (50 in total).
INSERT INTO users (username, password_hash, role)
VALUES ('test.holder', '$2b$10$.XfuSLR4.IK0UsgOccWOaeNPt5ZQkG5h08ESiOqxOd8IANWpuyxmC', 'POLICYHOLDER');

INSERT INTO policies (policy_number, holder_id, coverage_start, coverage_end, coverage_limit) 
VALUES
    (1004, (SELECT id FROM users WHERE username = 'test.holder'),
        '2026-01-01'::date, '2027-01-01'::date, 50000.00),
    (1005, (SELECT id FROM users WHERE username = 'test.holder'),
        '2026-01-01'::date, '2027-01-01'::date, 50000.00);

-- 50 claims, generated from the numbers 1 to 50
INSERT INTO claims (claim_number, policy_id, incident_date, amount, description, status, created_at, updated_at)
SELECT
    'CLM-2026-' || LPAD(n::text, 6, '0') AS claim_number,
    (SELECT id FROM policies WHERE policy_number = CASE WHEN n % 2 = 0 THEN 1004 ELSE 1005 END) AS policy_id,
    DATE '2026-01-01' + (n * INTERVAL '5 days') AS incident_date,
    (n * 100)::numeric(12, 2) AS amount,
    'Claim description for claim number ' || n AS description,
    CASE
        WHEN n <= 25 THEN 'SUBMITTED'
        WHEN n <= 35 THEN 'UNDER_REVIEW'
        WHEN n <= 40 THEN 'APPROVED'
        WHEN n <= 43 THEN 'DENIED'
        ELSE 'PAID'
    END,
    TIMESTAMP '2026-02-01 09:00:00' + (n * INTERVAL '1 day') AS created_at,
    TIMESTAMP '2026-02-01 09:00:00' + (n * INTERVAL '1 day') AS updated_at
FROM generate_series(1, 50) AS n;