CREATE TABLE loan_products (
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
public_id UUID NOT NULL UNIQUE,
name TEXT NOT NULL UNIQUE,
total_principal DECIMAL (19,4) NOT NULL,
interest_rate DECIMAL (5,4) NOT NULL,
penalty_rate_ea DECIMAL (5,4) NOT NULL,
credit_modality TEXT NOT NULL,
installments INT NOT NULL,
periodicity INT NOT NULL DEFAULT 12,
minimum_user_score INT NOT NULL,
created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
last_update TIMESTAMPTZ DEFAULT now() NOT NULL,
CONSTRAINT chk_periodicity_monthly_only
CHECK (periodicity = 12),
CONSTRAINT chk_positive_penalty_rate
CHECK (penalty_rate_ea > 0),
CONSTRAINT chk_valid_credit_modality
CHECK (credit_modality IN (
'CONSUMER_AND_ORDINARY','LOW_AMOUNT_CONSUMER','PRODUCTIVE_LARGE_AMOUNT',
'PRODUCTIVE_RURAL','PRODUCTIVE_URBAN','POPULAR_PRODUCTIVE_RURAL',
'POPULAR_PRODUCTIVE_URBAN'))
);

CREATE TABLE loans (
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
public_id UUID NOT NULL UNIQUE,
user_id UUID NOT NULL,
loan_product_id BIGINT REFERENCES loan_products(id) NOT NULL,
principal_receivable DECIMAL (19,4),
start_date DATE,
end_date DATE,
payday INT,
status TEXT NOT NULL,
created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
last_update TIMESTAMPTZ DEFAULT now() NOT NULL,
CONSTRAINT chk_valid_payday
CHECK (payday BETWEEN 1 AND 28),
CONSTRAINT chk_valid_status
CHECK (status IN
('REJECTED','PRE_APPROVED','ACTIVE',
'COMPLETED','IN_ARREARS'))
);

CREATE TABLE loan_installments (
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
public_id UUID NOT NULL UNIQUE,
loan_id BIGINT REFERENCES loans (id) NOT NULL,
installment_number INT NOT NULL,
principal_amount DECIMAL (19,4) NOT NULL,
interest_amount DECIMAL (19,4) NOT NULL,
paid_arrears_amount DECIMAL (19,4) NOT NULL,
accrued_arrears_amount DECIMAL (19,4) NOT NULL,
arrears_accrued_until DATE,
total_amount DECIMAL (19,4) NOT NULL,
payment_date DATE NOT NULL,
status TEXT DEFAULT 'UNPAID' NOT NULL,
created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
last_update TIMESTAMPTZ DEFAULT now() NOT NULL,
CONSTRAINT chk_valid_status
CHECK (status IN
('PAID', 'UNPAID',
'CURRENT', 'OVERDUE')),
CONSTRAINT uq_due_date_per_loan
UNIQUE (payment_date, loan_id),
CONSTRAINT uq_installment_number_per_loan
UNIQUE (installment_number, loan_id),
CONSTRAINT chk_principal_zero_only_when_paid
CHECK (principal_amount > 0 OR status = 'PAID')
);

CREATE TABLE payments (
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
public_id UUID NOT NULL UNIQUE,
loan_id BIGINT REFERENCES loans (id) NOT NULL,
transaction_code TEXT NOT NULL,
amount DECIMAL (19, 4) NOT NULL,
financial_method TEXT NOT NULL,
description TEXT NOT NULL,
status TEXT DEFAULT 'PENDING' NOT NULL,
applied BOOlEAN DEFAULT false NOT NULL,
created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
last_update TIMESTAMPTZ DEFAULT now() NOT NULL,
CONSTRAINT uq_transaction_code_per_financial_method
UNIQUE (transaction_code, financial_method),
CONSTRAINT chk_valid_status
CHECK (status IN
('PENDING','APPROVED',
'DECLINED')),
CONSTRAINT chk_positive_amount
CHECK (amount > 0),
CONSTRAINT chk_valid_financial_method
CHECK (financial_method IN (
'BANK','PAYMENT_GATEWAY',
'NEQUI','DAVIPLATA','ADJUSTMENT'))
);

CREATE TABLE payment_allocations (
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
public_id UUID NOT NULL UNIQUE,
payment_id BIGINT REFERENCES payments (id) NOT NULL,
loan_installment_id BIGINT REFERENCES loan_installments (id) NOT NULL,
amount DECIMAL (19,4) NOT NULL,
applied_to TEXT NOT NULL,
created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
last_update TIMESTAMPTZ DEFAULT now() NOT NULL,
CONSTRAINT chk_valid_applied_to
CHECK (applied_to IN
('PRINCIPAL', 'INTEREST',
'ARREAR', 'SURPLUS')),
CONSTRAINT chk_positive_amount
CHECK (amount > 0)
);

CREATE TABLE usury_rates (
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
public_id UUID NOT NULL UNIQUE,
credit_modality TEXT NOT NULL,
valid_from DATE NOT NULL,
rate_ea DECIMAL (5,4) NOT NULL,
resolution TEXT,
created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
last_update TIMESTAMPTZ DEFAULT now() NOT NULL,
CONSTRAINT uq_usury_rate_per_modality_and_month
UNIQUE (credit_modality, valid_from),
CONSTRAINT chk_usury_rate_starts_on_first_day
CHECK (EXTRACT(DAY FROM valid_from) = 1),
CONSTRAINT chk_usury_rate_positive
CHECK (rate_ea > 0),
CONSTRAINT chk_usury_rate_valid_credit_modality
CHECK (credit_modality IN (
'CONSUMER_AND_ORDINARY','LOW_AMOUNT_CONSUMER','PRODUCTIVE_LARGE_AMOUNT',
'PRODUCTIVE_RURAL','PRODUCTIVE_URBAN','POPULAR_PRODUCTIVE_RURAL',
'POPULAR_PRODUCTIVE_URBAN'))
);

CREATE INDEX idx_loans_user ON loans(user_id);
CREATE INDEX idx_loan_installments_loan ON loan_installments(loan_id);
CREATE INDEX idx_payments_loan ON payments(loan_id);
CREATE UNIQUE INDEX idx_uq_status_current_per_loan ON loan_installments (loan_id) WHERE status = 'CURRENT';
CREATE INDEX idx_payment_allocations_payments ON payment_allocations(payment_id);
CREATE INDEX idx_payment_allocations_loan_installments ON payment_allocations(loan_installment_id);

INSERT INTO usury_rates (public_id, credit_modality, valid_from, rate_ea, resolution) VALUES
(gen_random_uuid(), 'CONSUMER_AND_ORDINARY', '2026-01-01', 0.2436, NULL),
(gen_random_uuid(), 'CONSUMER_AND_ORDINARY', '2026-02-01', 0.2523, NULL),
(gen_random_uuid(), 'CONSUMER_AND_ORDINARY', '2026-03-01', 0.2552, NULL),
(gen_random_uuid(), 'CONSUMER_AND_ORDINARY', '2026-04-01', 0.2676, NULL),
(gen_random_uuid(), 'CONSUMER_AND_ORDINARY', '2026-05-01', 0.2817, NULL),
(gen_random_uuid(), 'CONSUMER_AND_ORDINARY', '2026-06-01', 0.2879, NULL),
(gen_random_uuid(), 'CONSUMER_AND_ORDINARY', '2026-07-01', 0.2879, NULL),
(gen_random_uuid(), 'CONSUMER_AND_ORDINARY', '2026-08-01', 0.2966, NULL),
(gen_random_uuid(), 'CONSUMER_AND_ORDINARY', '2026-09-01', 0.2924, 'Resolucion 1260 de 2026'),
(gen_random_uuid(), 'CONSUMER_AND_ORDINARY', '2026-10-01', 0.2859, NULL),
(gen_random_uuid(), 'LOW_AMOUNT_CONSUMER', '2026-09-01', 0.6687, 'Resolucion 1260 de 2026'),
(gen_random_uuid(), 'LOW_AMOUNT_CONSUMER', '2026-10-01', 0.6666, NULL),
(gen_random_uuid(), 'PRODUCTIVE_LARGE_AMOUNT', '2026-09-01', 0.4188, 'Resolucion 1260 de 2026'),
(gen_random_uuid(), 'PRODUCTIVE_LARGE_AMOUNT', '2026-10-01', 0.4125, NULL),
(gen_random_uuid(), 'PRODUCTIVE_RURAL', '2026-09-01', 0.3264, 'Resolucion 1260 de 2026'),
(gen_random_uuid(), 'PRODUCTIVE_RURAL', '2026-10-01', 0.3095, NULL),
(gen_random_uuid(), 'PRODUCTIVE_URBAN', '2026-09-01', 0.5792, 'Resolucion 1260 de 2026'),
(gen_random_uuid(), 'PRODUCTIVE_URBAN', '2026-10-01', 0.5739, NULL),
(gen_random_uuid(), 'POPULAR_PRODUCTIVE_RURAL', '2026-09-01', 0.6720, 'Resolucion 1260 de 2026'),
(gen_random_uuid(), 'POPULAR_PRODUCTIVE_RURAL', '2026-10-01', 0.6545, NULL),
(gen_random_uuid(), 'POPULAR_PRODUCTIVE_URBAN', '2026-09-01', 0.8813, 'Resolucion 1260 de 2026'),
(gen_random_uuid(), 'POPULAR_PRODUCTIVE_URBAN', '2026-10-01', 0.8760, NULL);
