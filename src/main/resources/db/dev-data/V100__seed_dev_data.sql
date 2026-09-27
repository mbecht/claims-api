-- Dev-only seed data. Only loaded when the "dev" profile is active (see application-dev.yml),
-- which adds this db/dev-data directory as an extra Flyway location alongside db/migration.
-- Every seeded account shares the password "password123" for local testing convenience.

INSERT INTO users (username, password_hash, role) VALUES
    ('alice.holder',    '$2b$10$.XfuSLR4.IK0UsgOccWOaeNPt5ZQkG5h08ESiOqxOd8IANWpuyxmC', 'POLICYHOLDER'),
    ('bob.holder',      '$2b$10$.XfuSLR4.IK0UsgOccWOaeNPt5ZQkG5h08ESiOqxOd8IANWpuyxmC', 'POLICYHOLDER'),
    ('carol.adjuster',  '$2b$10$.XfuSLR4.IK0UsgOccWOaeNPt5ZQkG5h08ESiOqxOd8IANWpuyxmC', 'ADJUSTER'),
    ('dave.supervisor', '$2b$10$.XfuSLR4.IK0UsgOccWOaeNPt5ZQkG5h08ESiOqxOd8IANWpuyxmC', 'SUPERVISOR');

INSERT INTO policies (policy_number, holder_id, coverage_start, coverage_end, coverage_limit) VALUES
    -- Active policy, well outside the 30-day-from-start fraud window.
    (1001, (SELECT id FROM users WHERE username = 'alice.holder'),
        (CURRENT_DATE - INTERVAL '200 days')::date, (CURRENT_DATE + INTERVAL '165 days')::date, 5000.00),

    -- Expired policy: coverage ended over a month ago.
    (1002, (SELECT id FROM users WHERE username = 'alice.holder'),
        (CURRENT_DATE - INTERVAL '400 days')::date, (CURRENT_DATE - INTERVAL '35 days')::date, 8000.00),

    -- Active policy that started 10 days ago: any claim filed against it today falls inside
    -- the future 30-day-from-policy-start fraud rule (story 13).
    (1003, (SELECT id FROM users WHERE username = 'bob.holder'),
        (CURRENT_DATE - INTERVAL '10 days')::date, (CURRENT_DATE + INTERVAL '355 days')::date, 12000.00);
