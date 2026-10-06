package com.virgoai.pipeline.target;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class InMemoryTargetStore implements TargetStore {

    private final Map<String, Map<String, Map<String, Object>>> collections = new ConcurrentHashMap<>();

    @Override
    public void upsert(String collection, String id, Map<String, Object> document) {
        collections.computeIfAbsent(collection, c -> new ConcurrentHashMap<>())
                .put(id, new LinkedHashMap<>(document));
    }

    @Override
    public void append(String collection, Map<String, Object> document) {
        upsert(collection, UUID.randomUUID().toString(), document);
    }

    @Override
    public int count(String collection) {
        return collections.getOrDefault(collection, Map.of()).size();
    }

    @Override
    public List<Map<String, Object>> all(String collection) {
        return new ArrayList<>(collections.getOrDefault(collection, Map.of()).values());
    }
}
