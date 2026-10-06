INSERT INTO out_institutions
    (run_id, page_no, cert, bank_name, city, state_code,
     assets_usd, deposits_usd, offices, established_date)
SELECT run_id, page_no, cert, TRIM(bank_name), city, state_code,
       assets_thousands * 1000, deposits_thousands * 1000, offices,
       SUBSTRING(established, 7, 4) || '-' || SUBSTRING(established, 1, 2) || '-' || SUBSTRING(established, 4, 2)
FROM stg_institutions
WHERE run_id = ? AND page_no = ?
