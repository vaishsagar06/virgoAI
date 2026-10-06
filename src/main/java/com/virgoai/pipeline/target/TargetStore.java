package com.virgoai.pipeline.target;

import java.util.List;
import java.util.Map;

public interface TargetStore {

    void upsert(String collection, String id, Map<String, Object> document);

    void append(String collection, Map<String, Object> document);

    int count(String collection);

    List<Map<String, Object>> all(String collection);
}
