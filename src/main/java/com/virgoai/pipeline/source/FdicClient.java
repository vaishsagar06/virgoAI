package com.virgoai.pipeline.source;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FdicClient {

    private static final String FIELDS = "CERT,NAME,CITY,STALP,ASSET,DEP,OFFICES,ESTYMD,DATEUPDT";

    private final RestClient http;

    public FdicClient(@Value("${sources.fdic.base-url}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public FdicResponse fetchPage(int offset, int limit) {
        return http.get()
                .uri(uri -> uri.path("/institutions")
                        .queryParam("filters", "ACTIVE:1")
                        .queryParam("fields", FIELDS)
                        .queryParam("sort_by", "CERT")
                        .queryParam("sort_order", "ASC")
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .body(FdicResponse.class);
    }
}
