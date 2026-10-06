INSERT INTO out_accounts
    (run_id, account_id, account_name, account_number_masked,
     routing_number, bank_name, account_type, currency)
SELECT run_id, account_id, UPPER(TRIM(account_name)), '****' || RIGHT(account_number, 4),
       routing_number, bank_name, account_type, currency
FROM stg_accounts
WHERE run_id = ?
