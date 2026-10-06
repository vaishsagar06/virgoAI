package com.virgoai.pipeline.job;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import com.virgoai.pipeline.fault.FaultSwitches;
import com.virgoai.pipeline.log.JobLog;
import com.virgoai.pipeline.source.FdicClient;
import com.virgoai.pipeline.source.FdicResponse;
import com.virgoai.pipeline.target.TargetStore;

@Component
public class InstitutionsJob {

    private static final String JOB = "institutions";
    private static final int MAX_ATTEMPTS = 3;

    private final FdicClient client;
    private final JdbcTemplate jdbc;
    private final TargetStore target;
    private final JobLog log;
    private final FaultSwitches faults;
    private final int pageSize;
    private final int timeoutPage;
    private final long pageDelayMs;
    private final String transformSql;

    public InstitutionsJob(FdicClient client, JdbcTemplate jdbc, TargetStore target, JobLog log,
            FaultSwitches faults,
            @Value("${sources.fdic.page-size:100}") int pageSize,
            @Value("${faults.fdic-timeout.page:38}") int timeoutPage,
            @Value("${sources.fdic.page-delay-ms:300}") long pageDelayMs) {
        this.client = client;
        this.jdbc = jdbc;
        this.target = target;
        this.log = log;
        this.faults = faults;
        this.pageSize = pageSize;
        this.timeoutPage = timeoutPage;
        this.pageDelayMs = pageDelayMs;
        this.transformSql = readSql("sql/institutions_transform.sql");
    }

    public Map<String, Object> run(int startPage) {
        String runId = UUID.randomUUID().toString();
        String step = "EXTRACT";
        int read = 0;
        int loaded = 0;
        int page = startPage;
        int lastPageLoaded = startPage - 1;
        int totalPages = 0;

        jdbc.update("INSERT INTO job_run (run_id, job_name, extract_mode, status, started_at) "
                + "VALUES (?, ?, 'FULL', 'RUNNING', CURRENT_TIMESTAMP)", runId, JOB);
        log.info(runId, JOB, step, "run_started",
                "extract_mode=FULL write_mode=APPEND page_size=" + pageSize + " start_page=" + startPage);

        try {
            do {
                step = "EXTRACT";
                FdicResponse response = fetchWithRetry(runId, page);
                List<FdicResponse.Row> rows =
                        response == null || response.data() == null ? List.of() : response.data();
                if (totalPages == 0) {
                    int total = response == null || response.meta() == null ? 0 : response.meta().total();
                    totalPages = (total + pageSize - 1) / pageSize;
                    log.info(runId, JOB, step, "extract_sized",
                            "total_records=" + total + " total_pages=" + totalPages);
                }
                read += rows.size();

                step = "STAGE";
                for (FdicResponse.Row row : rows) {
                    FdicResponse.Institution i = row.data();
                    jdbc.update("INSERT INTO stg_institutions (run_id, page_no, cert, bank_name, city, "
                            + "state_code, assets_thousands, deposits_thousands, offices, established, "
                            + "date_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            runId, page, i.cert(), i.name(), i.city(), i.state(), i.assets(),
                            i.deposits(), i.offices(), i.established(), i.dateUpdated());
                }

                step = "TRANSFORM";
                jdbc.update(transformSql, runId, page);

                step = "LOAD";
                List<Map<String, Object>> documents = jdbc.queryForList(
                        "SELECT cert, bank_name, city, state_code, assets_usd, deposits_usd, offices, "
                                + "established_date FROM out_institutions WHERE run_id = ? AND page_no = ?",
                        runId, page);
                for (Map<String, Object> document : documents) {
                    target.append("institutions", document);
                    loaded++;
                }
                lastPageLoaded = page;
                log.info(runId, JOB, step, "page_done",
                        "page=" + page + " total_pages=" + totalPages + " records_loaded_total=" + loaded);

                page++;
                pause(pageDelayMs);
            } while (page <= totalPages);

            jdbc.update("UPDATE job_run SET status = 'SUCCEEDED', finished_at = CURRENT_TIMESTAMP, "
                    + "records_read = ?, records_loaded = ? WHERE run_id = ?", read, loaded, runId);
            log.info(runId, JOB, "END", "run_succeeded",
                    "records_read=" + read + " records_loaded=" + loaded
                            + " start_page=" + startPage + " last_page_loaded=" + lastPageLoaded);
        } catch (Exception e) {
            String message = Errors.describe(e);
            jdbc.update("UPDATE job_run SET status = 'FAILED', finished_at = CURRENT_TIMESTAMP, "
                    + "records_read = ?, records_loaded = ?, failed_step = ?, error_message = ? "
                    + "WHERE run_id = ?", read, loaded, step, message, runId);
            log.error(runId, JOB, step, "run_failed", message);
            log.info(runId, JOB, "END", "run_state",
                    "failed_page=" + page + " last_page_loaded=" + lastPageLoaded
                            + " total_pages=" + totalPages + " records_loaded=" + loaded
                            + " resume_from_page=" + page);
        }

        return jdbc.queryForMap("SELECT * FROM job_run WHERE run_id = ?", runId);
    }

    private FdicResponse fetchWithRetry(String runId, int page) {
        int offset = (page - 1) * pageSize;
        RuntimeException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                if (faults.isOn(FaultSwitches.FDIC_TIMEOUT) && page == timeoutPage) {
                    throw new ResourceAccessException(
                            "I/O error on GET request for \"/institutions\": Read timed out",
                            new SocketTimeoutException("Read timed out"));
                }
                return client.fetchPage(offset, pageSize);
            } catch (RuntimeException e) {
                last = e;
                log.warn(runId, JOB, "EXTRACT", "page_attempt_failed",
                        "page=" + page + " offset=" + offset + " attempt=" + attempt
                                + " max_attempts=" + MAX_ATTEMPTS
                                + " error=\"" + Errors.describe(e).replace('"', '\'') + "\"");
                if (attempt < MAX_ATTEMPTS) {
                    pause(500L * attempt);
                }
            }
        }
        throw last;
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String readSql(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
