package com.virgoai.pipeline.fault;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class FaultSwitches {

    public static final String ACCOUNTS_EXPIRED_CREDENTIAL = "accounts-expired-credential";
    public static final String TRANSACTIONS_TYPE_MISMATCH = "transactions-type-mismatch";
    public static final String FDIC_TIMEOUT = "fdic-timeout";

    private final Map<String, Boolean> switches = new ConcurrentHashMap<>();

    public FaultSwitches() {
        switches.put(ACCOUNTS_EXPIRED_CREDENTIAL, false);
        switches.put(TRANSACTIONS_TYPE_MISMATCH, false);
        switches.put(FDIC_TIMEOUT, false);
    }

    public boolean isOn(String name) {
        return switches.getOrDefault(name, false);
    }

    public void set(String name, boolean on) {
        switches.put(name, on);
    }

    public Map<String, Boolean> all() {
        return new TreeMap<>(switches);
    }
}
