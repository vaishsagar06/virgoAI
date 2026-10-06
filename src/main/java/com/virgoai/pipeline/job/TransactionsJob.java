package com.virgoai.pipeline.job;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.virgoai.pipeline.fault.FaultSwitches;
import com.virgoai.pipeline.log.JobLog;
import com.virgoai.pipeline.source.AccountsClient;
import com.virgoai.pipeline.source.TransactionsClient;
import com.virgoai.pipeline.source.TransactionsResponse;
import com.virgoai.pipeline.target.TargetStore;

@Component
public class TransactionsJob {

    private static final String JOB = "transactions";

    private final AccountsClient accountsClient;
    private final TransactionsClient client;
    private final JdbcTemplate jdbc;
    private final TargetStore target;
    private final JobLog log;
    private final FaultSwitches faults;
    private final String strictSql;
    private final String fixedSql;

    public TransactionsJob(AccountsClient accountsClient, TransactionsClient client, JdbcTemplate jdbc,
            TargetStore target, JobLog log, FaultSwitches faults) {
        this.accountsClient = accountsClient;
        this.client = client;
        this.jdbc = jdbc;
        this.target = target;
        this.log = log;
        this.faults = faults;
        this.strictSql = readSql("sql/transactions_transform_strict.sql");
        this.fixedSql = readSql("sql/transactions_transform_fixed.sql");
    }

    public Map<String, Object> run() {
        String runId = UUID.randomUUID().toString();
        String step = "EXTRACT";
        int read = 0;
        int loaded = 0;
        LocalDate fromDate = readWatermark();
        LocalDate toDate = LocalDate.now().minusDays(1);

        jdbc.update("INSERT INTO job_run (run_id, job_name, extract_mode, status, started_at) "
                + "VALUES (?, ?, 'INCREMENTAL', 'RUNNING', CURRENT_TIMESTAMP)", runId, JOB);
        log.info(runId, JOB, step, "run_started",
                "extract_mode=INCREMENTAL write_mode=APPEND from_date=" + fromDate + " to_date=" + toDate);

        try {
            String accountId = accountsClient.fetchAccounts().get(0).accountID();
            TransactionsResponse response = client.fetchPreviousDay(accountId, fromDate, toDate);
            List<TransactionsResponse.Transaction> transactions =
                    response == null || response.transactions() == null ? List.of() : response.transactions();
            read = transactions.size();
            log.info(runId, JOB, step, "extract_done", "records_read=" + read);

            step = "STAGE";
            for (TransactionsResponse.Transaction t : transactions) {
                String charges = t.wiresTransactionDetails() == null
                        ? null : t.wiresTransactionDetails().usBankCharges();
                jdbc.update("INSERT INTO stg_transactions (run_id, account_id, transaction_date, debit_credit, "
                        + "description, transaction_type, bai_code, reference_number, bank_reference_number, "
                        + "amount, currency, us_bank_charges) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        runId, t.accountID(), t.transactionDate(), t.debitCreditIndicator(),
                        t.transactionDescription(), t.transactionType(), t.baiCode(),
                        t.transactionReferenceNumber(), t.bankReferenceNumber(),
                        t.amount(), t.currency(), charges);
            }
            log.info(runId, JOB, step, "stage_done", "records_staged=" + read);

            step = "TRANSFORM";
            boolean strict = faults.isOn(FaultSwitches.TRANSACTIONS_TYPE_MISMATCH);
            int transformed = jdbc.update(strict ? strictSql : fixedSql, runId);
            log.info(runId, JOB, step, "transform_done", "records_transformed=" + transformed);

            step = "LOAD";
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT reference_number, account_id, transaction_day, description, transaction_type, "
                            + "bai_code, signed_amount, currency, bank_charges "
                            + "FROM out_transactions WHERE run_id = ?", runId);
            for (Map<String, Object> row : rows) {
                target.append("transactions", row);
                loaded++;
            }
            log.info(runId, JOB, step, "load_done", "records_loaded=" + loaded);

            step = "WATERMARK";
            jdbc.update("MERGE INTO job_watermark (job_name, last_success_date) KEY (job_name) VALUES (?, ?)",
                    JOB, toDate.toString());
            log.info(runId, JOB, step, "watermark_moved", "new_watermark=" + toDate);

            jdbc.update("UPDATE job_run SET status = 'SUCCEEDED', finished_at = CURRENT_TIMESTAMP, "
                    + "records_read = ?, records_loaded = ? WHERE run_id = ?", read, loaded, runId);
            log.info(runId, JOB, "END", "run_succeeded",
                    "records_read=" + read + " records_loaded=" + loaded + " watermark_moved=true");
        } catch (Exception e) {
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(e);
            String message = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
            message = message.replaceAll("\\s+", " ");
            if (message.length() > 900) {
                message = message.substring(0, 900);
            }
            jdbc.update("UPDATE job_run SET status = 'FAILED', finished_at = CURRENT_TIMESTAMP, "
                    + "records_read = ?, records_loaded = ?, failed_step = ?, error_message = ? "
                    + "WHERE run_id = ?", read, loaded, step, message, runId);
            log.error(runId, JOB, step, "run_failed", message);
            log.info(runId, JOB, "END", "run_state",
                    "records_read=" + read + " records_loaded=" + loaded + " watermark_moved=false");
        }

        return jdbc.queryForMap("SELECT * FROM job_run WHERE run_id = ?", runId);
    }

    private LocalDate readWatermark() {
        List<String> rows = jdbc.queryForList(
                "SELECT last_success_date FROM job_watermark WHERE job_name = ?", String.class, JOB);
        return rows.isEmpty() ? LocalDate.now().minusDays(7) : LocalDate.parse(rows.get(0));
    }

    private static String readSql(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
