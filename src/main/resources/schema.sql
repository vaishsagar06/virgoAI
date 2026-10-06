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
