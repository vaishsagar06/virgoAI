INSERT INTO out_transactions
    (run_id, reference_number, account_id, transaction_day, description,
     transaction_type, bai_code, signed_amount, currency, bank_charges)
SELECT run_id, reference_number, account_id, SUBSTRING(transaction_date, 1, 10), description,
       transaction_type, bai_code,
       CASE WHEN debit_credit = 'DEBIT' THEN -amount ELSE amount END,
       currency,
       CAST(us_bank_charges AS DECIMAL(12,2))
FROM stg_transactions
WHERE run_id = ?
