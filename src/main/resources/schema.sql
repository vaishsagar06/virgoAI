CREATE TABLE IF NOT EXISTS job_run (
    run_id VARCHAR(36) PRIMARY KEY,
    job_name VARCHAR(50) NOT NULL,
    extract_mode VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    records_read INT DEFAULT 0,
    records_loaded INT DEFAULT 0,
    failed_step VARCHAR(20),
    error_message VARCHAR(1000)
);
CREATE TABLE IF NOT EXISTS stg_accounts (
    run_id VARCHAR(36) NOT NULL,
    account_id VARCHAR(100),
    account_name VARCHAR(200),
    account_number VARCHAR(50),
    routing_number VARCHAR(20),
    bank_name VARCHAR(200),
    account_type VARCHAR(50),
    currency VARCHAR(10)
);
CREATE TABLE IF NOT EXISTS out_accounts (
    run_id VARCHAR(36) NOT NULL,
    account_id VARCHAR(100),
    account_name VARCHAR(200),
    account_number_masked VARCHAR(50),
    routing_number VARCHAR(20),
    bank_name VARCHAR(200),
    account_type VARCHAR(50),
    currency VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS job_watermark (
    job_name VARCHAR(50) PRIMARY KEY,
    last_success_date VARCHAR(10) NOT NULL
);

CREATE TABLE IF NOT EXISTS stg_transactions (
    run_id VARCHAR(36) NOT NULL,
    account_id VARCHAR(100),
    transaction_date VARCHAR(40),
    debit_credit VARCHAR(10),
    description VARCHAR(300),
    transaction_type VARCHAR(30),
    bai_code VARCHAR(10),
    reference_number VARCHAR(50),
    bank_reference_number VARCHAR(50),
    amount DECIMAL(18,2),
    currency VARCHAR(10),
    us_bank_charges VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS out_transactions (
    run_id VARCHAR(36) NOT NULL,
    reference_number VARCHAR(50),
    account_id VARCHAR(100),
    transaction_day VARCHAR(10),
    description VARCHAR(300),
    transaction_type VARCHAR(30),
    bai_code VARCHAR(10),
    signed_amount DECIMAL(18,2),
    currency VARCHAR(10),
    bank_charges DECIMAL(12,2)
);
