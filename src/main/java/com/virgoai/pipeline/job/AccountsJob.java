package com.virgoai.pipeline.job;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.virgoai.pipeline.log.JobLog;
import com.virgoai.pipeline.source.AccountsClient;
import com.virgoai.pipeline.source.AccountsResponse;
import com.virgoai.pipeline.target.TargetStore;

@Component
public class AccountsJob {

    private static final String JOB = "accounts";

    private final AccountsClient client;
    private final JdbcTemplate jdbc;
    private final TargetStore target;
    private final JobLog log;
    private final String transformSql;

    public AccountsJob(AccountsClient client, JdbcTemplate jdbc, TargetStore target, JobLog log) {
        this.client = client;
        this.jdbc = jdbc;
        this.target = target;
        this.log = log;
        this.transformSql = readSql("sql/accounts_transform.sql");
    }

    public Map<String, Object> run() {
        String runId = UUID.randomUUID().toString();
        String step = "EXTRACT";
        int read = 0;
        int loaded = 0;

        jdbc.update("INSERT INTO job_run (run_id, job_name, extract_mode, status, started_at) "
                + "VALUES (?, ?, 'FULL', 'RUNNING', CURRENT_TIMESTAMP)", runId, JOB);
        log.info(runId, JOB, step, "run_started", "extract_mode=FULL write_mode=OVERWRITE");

        try {
            List<AccountsResponse.Account> accounts = client.fetchAccounts();
            read = accounts.size();
            log.info(runId, JOB, step, "extract_done", "records_read=" + read);

            step = "STAGE";
            for (AccountsResponse.Account a : accounts) {
                jdbc.update("INSERT INTO stg_accounts (run_id, account_id, account_name, account_number, "
                        + "routing_number, bank_name, account_type, currency) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        runId, a.accountID(), a.accountName(), a.accountNumber(),
                        a.routingNumber(), a.bankName(), a.accountType(), a.currency());
            }
            log.info(runId, JOB, step, "stage_done", "records_staged=" + read);

            step = "TRANSFORM";
            int transformed = jdbc.update(transformSql, runId);
            log.info(runId, JOB, step, "transform_done", "records_transformed=" + transformed);

            step = "LOAD";
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT account_id, account_name, account_number_masked, routing_number, "
                            + "bank_name, account_type, currency FROM out_accounts WHERE run_id = ?", runId);
            for (Map<String, Object> row : rows) {
                target.upsert("accounts", String.valueOf(row.get("account_id")), row);
                loaded++;
            }
            log.info(runId, JOB, step, "load_done", "records_loaded=" + loaded);

            jdbc.update("UPDATE job_run SET status = 'SUCCEEDED', finished_at = CURRENT_TIMESTAMP, "
                    + "records_read = ?, records_loaded = ? WHERE run_id = ?", read, loaded, runId);
            log.info(runId, JOB, "END", "run_succeeded",
                    "records_read=" + read + " records_loaded=" + loaded);
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            if (message.length() > 900) {
                message = message.substring(0, 900);
            }
            jdbc.update("UPDATE job_run SET status = 'FAILED', finished_at = CURRENT_TIMESTAMP, "
                    + "records_read = ?, records_loaded = ?, failed_step = ?, error_message = ? "
                    + "WHERE run_id = ?", read, loaded, step, message, runId);
            log.error(runId, JOB, step, "run_failed", message);
        }

        return jdbc.queryForMap("SELECT * FROM job_run WHERE run_id = ?", runId);
    }

    private static String readSql(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
