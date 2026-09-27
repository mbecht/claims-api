CREATE TABLE claims (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    claim_number  INT NOT NULL UNIQUE,
    policy_id     BIGINT NOT NULL REFERENCES policies (id) ON DELETE RESTRICT,
    incident_date TIMESTAMP NOT NULL,
    amount        NUMERIC(12, 2) NOT NULL,
    description   VARCHAR(1000) NOT NULL,
    status        VARCHAR(20) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_claims_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_claims_status CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'DENIED', 'PAID'))
);

CREATE INDEX idx_claims_policy_id ON claims (policy_id);
