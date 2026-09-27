CREATE TABLE policies (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    policy_number  INT NOT NULL UNIQUE,
    holder_id      BIGINT NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    coverage_start DATE NOT NULL,
    coverage_end   DATE NOT NULL,
    coverage_limit NUMERIC(12, 2) NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_policies_coverage_dates CHECK (coverage_end > coverage_start)
);

CREATE INDEX idx_policies_holder_id ON policies (holder_id);
