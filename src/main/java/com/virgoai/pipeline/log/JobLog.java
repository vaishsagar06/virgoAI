package com.virgoai.pipeline.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JobLog {

    private static final Logger LOGGER = LoggerFactory.getLogger("pipeline.job");

    public void info(String runId, String job, String step, String event, String detail) {
        LOGGER.info("run_id={} job={} step={} event={} {}", runId, job, step, event, detail);
    }

    public void warn(String runId, String job, String step, String event, String detail) {
        LOGGER.warn("run_id={} job={} step={} event={} {}", runId, job, step, event, detail);
    }

    public void error(String runId, String job, String step, String event, String message) {
        LOGGER.error("run_id={} job={} step={} event={} error=\"{}\"",
                runId, job, step, event, message.replace('"', '\''));
    }
}
