#!/usr/bin/env bash
# Produces the three failure scenarios. Run against a freshly started app.
BASE=${BASE:-localhost:8080}

for fault in accounts-expired-credential transactions-type-mismatch fdic-timeout; do
  curl -s -X POST "$BASE/faults/$fault/on" > /dev/null
done

echo "== 1. Accounts: expired credential =="
curl -s -X POST "$BASE/jobs/accounts/run"; echo
echo "== 2. Transactions: type mismatch in the SQL transform =="
curl -s -X POST "$BASE/jobs/transactions/run"; echo
echo "== 3. Institutions: timeout on page 38 (takes about 20 seconds) =="
curl -s -X POST "$BASE/jobs/institutions/run"; echo

echo "== Records in the target after the failures =="
for collection in accounts transactions institutions; do
  curl -s "$BASE/target/$collection/count"; echo
done
