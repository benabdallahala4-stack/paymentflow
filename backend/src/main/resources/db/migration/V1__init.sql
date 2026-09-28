-- PaymentFlow initial schema. Mirrors docs/database/schema.md exactly (ADR-011: Flyway only),
-- with one deliberate deviation noted below.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- users
-- ============================================================
-- DEVIATION from schema.md: schema.md specifies `email CITEXT NOT NULL UNIQUE`. We use
-- plain VARCHAR + a unique index on lower(email) instead, because Hibernate 6's schema
-- VALIDATOR (spring.jpa.hibernate.ddl-auto=validate, per ADR-011) does not recognize the
-- citext OID as compatible with any Java String JDBC type mapping, so app startup fails
-- schema validation even though the column is otherwise correct at the DB level. Case-
-- insensitive uniqueness is preserved via the expression index below; the application
-- layer (identity.application.AuthService) normalizes email to lowercase before every
-- read/write, which gives the same externally-observable behavior as CITEXT.
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    password_hash   TEXT NOT NULL,
    role            VARCHAR(16) NOT NULL CHECK (role IN ('CUSTOMER', 'ADMIN')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_users_email_lower ON users (lower(email));

-- ============================================================
-- accounts
-- ============================================================
CREATE TABLE accounts (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id               UUID NOT NULL REFERENCES users(id),
    currency               CHAR(3) NOT NULL,
    balance_minor_units    BIGINT NOT NULL DEFAULT 0,
    status                 VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
                               CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    version                BIGINT NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_accounts_balance_non_negative CHECK (balance_minor_units >= 0)
);

CREATE INDEX idx_accounts_owner_id ON accounts (owner_id);

-- ============================================================
-- transactions
-- ============================================================
CREATE TABLE transactions (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    initiated_by_user_id     UUID NOT NULL REFERENCES users(id),
    source_account_id        UUID NOT NULL REFERENCES accounts(id),
    destination_account_id   UUID NOT NULL REFERENCES accounts(id),
    amount_minor_units       BIGINT NOT NULL,
    currency                 CHAR(3) NOT NULL,
    status                   VARCHAR(16) NOT NULL DEFAULT 'PENDING'
                                 CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REJECTED')),
    failure_reason           TEXT,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at             TIMESTAMPTZ,
    CONSTRAINT chk_txn_amount_positive CHECK (amount_minor_units > 0),
    CONSTRAINT chk_txn_distinct_accounts CHECK (source_account_id <> destination_account_id)
);

CREATE INDEX idx_transactions_source_account ON transactions (source_account_id, created_at DESC);
CREATE INDEX idx_transactions_destination_account ON transactions (destination_account_id, created_at DESC);
CREATE INDEX idx_transactions_initiated_by ON transactions (initiated_by_user_id, created_at DESC, id DESC);

-- ============================================================
-- ledger_entries (append-only, immutable)
-- ============================================================
CREATE TABLE ledger_entries (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id       UUID NOT NULL REFERENCES transactions(id),
    account_id           UUID NOT NULL REFERENCES accounts(id),
    direction            VARCHAR(6) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount_minor_units   BIGINT NOT NULL CHECK (amount_minor_units > 0),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ledger_entries_account_id ON ledger_entries (account_id, created_at DESC);
CREATE INDEX idx_ledger_entries_transaction_id ON ledger_entries (transaction_id);

-- ============================================================
-- idempotency_keys
-- ============================================================
CREATE TABLE idempotency_keys (
    idempotency_key      TEXT NOT NULL,
    user_id              UUID NOT NULL REFERENCES users(id),
    request_fingerprint  TEXT NOT NULL,
    transaction_id       UUID REFERENCES transactions(id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, idempotency_key)
);

-- ============================================================
-- outbox_events
-- ============================================================
CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id   UUID NOT NULL,
    event_type     VARCHAR(64) NOT NULL,
    payload        JSONB NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;

-- Immutability of ledger_entries is enforced at the application repository level
-- (no update/delete methods, see LedgerEntryRepository) since revoking UPDATE/DELETE
-- privileges from the application's own DB role would also break Flyway-managed
-- ownership in a simple single-role local/Testcontainers setup. Documented here as a
-- deliberate simplification vs. ADR-011/schema.md's "DB privilege level" aspiration.
