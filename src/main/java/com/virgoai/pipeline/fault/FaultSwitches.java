package com.virgoai.pipeline.fault;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class FaultSwitches {

    public static final String TRANSACTIONS_TYPE_MISMATCH = "transactions-type-mismatch";

    private final Map<String, Boolean> switches = new ConcurrentHashMap<>();

    public FaultSwitches() {
        switches.put(TRANSACTIONS_TYPE_MISMATCH, false);
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
