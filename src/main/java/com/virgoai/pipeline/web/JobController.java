package com.virgoai.pipeline.web;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.virgoai.pipeline.job.AccountsJob;
import com.virgoai.pipeline.target.TargetStore;

@RestController
public class JobController {

    private final AccountsJob accountsJob;
    private final JdbcTemplate jdbc;
    private final TargetStore target;

    public JobController(AccountsJob accountsJob, JdbcTemplate jdbc, TargetStore target) {
        this.accountsJob = accountsJob;
        this.jdbc = jdbc;
        this.target = target;
    }

    @PostMapping("/jobs/accounts/run")
    public Map<String, Object> runAccounts() {
        return accountsJob.run();
    }

    @GetMapping("/runs")
    public List<Map<String, Object>> runs() {
        return jdbc.queryForList("SELECT * FROM job_run ORDER BY started_at DESC");
    }

    @GetMapping("/target/{collection}")
    public Map<String, Object> target(@PathVariable String collection) {
        return Map.of("count", target.count(collection), "documents", target.all(collection));
    }
}
