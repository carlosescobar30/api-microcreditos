CREATE TABLE loans (
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
public_id UUID NOT NULL UNIQUE,
user_id BIGINT NOT NULL,
total_amount DECIMAL (19,4) NOT NULL,
start_date DATE,
end_date DATE,
payday INT,
total_installments INT NOT NULL,
interest_rate DECIMAL (5,4) NOT NULL,
penalty_rate DECIMAL (5,4) NOT NULL,
status TEXT NOT NULL,
created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
last_update TIMESTAMPTZ DEFAULT now() NOT NULL,
CONSTRAINT chk_valid_payday
CHECK (payday BETWEEN 1 AND 28),
CONSTRAINT chk_valid_status
CHECK (status IN
('REQUIRED','PRE_APPROVED','ACTIVE','CANCELLED',
'REJECTED','COMPLETED','IN_ARREARS','CHARGED_OFF'))
);

CREATE TABLE loan_installments (
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
public_id UUID NOT NULL UNIQUE,
loan_id BIGINT REFERENCES loans (id) NOT NULL,
installment_number INT NOT NULL,
principal_amount DECIMAL (19,4) NOT NULL,
due_date DATE NOT NULL,
status TEXT DEFAULT 'UNPAID' NOT NULL,
amount_paid DECIMAL (19,4) DEFAULT 0 NOT NULL,
created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
last_update TIMESTAMPTZ DEFAULT now() NOT NULL,
CONSTRAINT chk_valid_status
CHECK (status IN
('PAID', 'UNPAID', 'PARTIALLY_PAID')),
CONSTRAINT consistency_partial_payment_and_partial_amount
CHECK ((status = 'PARTIALLY_PAID' AND amount_paid > 0)
OR (status = 'UNPAID' AND amount_paid = 0)
OR (status = 'PAID')),
CONSTRAINT uq_due_date_per_loan
UNIQUE (due_date, loan_id),
CONSTRAINT uq_installment_number_per_loan
UNIQUE (installment_number, loan_id)
);

CREATE TABLE payments (
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
public_id UUID NOT NULL UNIQUE,
loan_id BIGINT REFERENCES loans (id) NOT NULL,
amount DECIMAL (19, 4) NOT NULL,
financial_method TEXT NOT NULL,
description TEXT NOT NULL,
status TEXT DEFAULT 'PENDING' NOT NULL,
is_adjustment BOOLEAN DEFAULT FALSE NOT NULL,
reversal_payment_id BIGINT REFERENCES payments(id) UNIQUE,
created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
last_update TIMESTAMPTZ DEFAULT now() NOT NULL,
CONSTRAINT chk_valid_status
CHECK (status IN
('PENDING','APPROVED',
'DECLINED','CANCELED')),
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
reversal_payment_allocation_id BIGINT REFERENCES payment_allocations(id) UNIQUE,
created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
last_update TIMESTAMPTZ DEFAULT now() NOT NULL,
CONSTRAINT chk_valid_applied_to
CHECK (applied_to IN
('PRINCIPAL', 'INTEREST',
'ARREAR', 'ADMINISTRATIVE_FEE')),
CONSTRAINT chk_positive_amount
CHECK (amount > 0)
);

CREATE INDEX idx_loans_user ON loans(user_id);
CREATE INDEX idx_loan_installments_loan ON loan_installments(loan_id);
CREATE INDEX idx_payments_loans ON payments(loan_id);
CREATE INDEX idx_payment_allocations_payments ON payment_allocations(payment_id);
CREATE INDEX idx_payment_allocations_loan_installments ON payment_allocations(loan_installment_id);
