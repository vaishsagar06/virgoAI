package com.virgoai.pipeline.web;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.virgoai.pipeline.fault.FaultSwitches;
import com.virgoai.pipeline.job.AccountsJob;
import com.virgoai.pipeline.job.InstitutionsJob;
import com.virgoai.pipeline.job.TransactionsJob;
import com.virgoai.pipeline.target.TargetStore;

@RestController
public class JobController {

    private final AccountsJob accountsJob;
    private final TransactionsJob transactionsJob;
    private final InstitutionsJob institutionsJob;
    private final FaultSwitches faults;
    private final JdbcTemplate jdbc;
    private final TargetStore target;

    public JobController(AccountsJob accountsJob, TransactionsJob transactionsJob,
            InstitutionsJob institutionsJob, FaultSwitches faults, JdbcTemplate jdbc, TargetStore target) {
        this.accountsJob = accountsJob;
        this.transactionsJob = transactionsJob;
        this.institutionsJob = institutionsJob;
        this.faults = faults;
        this.jdbc = jdbc;
        this.target = target;
    }

    @PostMapping("/jobs/accounts/run")
    public Map<String, Object> runAccounts() {
        return accountsJob.run();
    }

    @PostMapping("/jobs/transactions/run")
    public Map<String, Object> runTransactions() {
        return transactionsJob.run();
    }

    @PostMapping("/jobs/institutions/run")
    public Map<String, Object> runInstitutions(
            @RequestParam(name = "fromPage", defaultValue = "1") int fromPage) {
        return institutionsJob.run(fromPage);
    }

    @GetMapping("/faults")
    public Map<String, Boolean> faults() {
        return faults.all();
    }

    @PostMapping("/faults/{name}/{state}")
    public Map<String, Boolean> setFault(@PathVariable("name") String name,
            @PathVariable("state") String state) {
        faults.set(name, "on".equalsIgnoreCase(state));
        return faults.all();
    }

    @GetMapping("/runs")
    public List<Map<String, Object>> runs() {
        return jdbc.queryForList("SELECT * FROM job_run ORDER BY started_at DESC");
    }

    @GetMapping("/target/{collection}")
    public Map<String, Object> target(@PathVariable("collection") String collection) {
        return Map.of("count", target.count(collection), "documents", target.all(collection));
    }

    @GetMapping("/target/{collection}/count")
    public Map<String, Object> targetCount(@PathVariable("collection") String collection) {
        return Map.of("collection", collection, "count", target.count(collection));
    }
}
