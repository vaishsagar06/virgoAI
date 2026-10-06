package com.virgoai.pipeline.source;

import java.time.Duration;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TransactionsClient {

    private final RestClient http;

    public TransactionsClient(@Value("${sources.usbank.base-url}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public TransactionsResponse fetchPreviousDay(String accountId, LocalDate fromDate, LocalDate toDate) {
        return http.get()
                .uri("/accounts/{accountId}/transactions/previous-day?fromDate={from}&toDate={to}",
                        accountId, fromDate, toDate)
                .retrieve()
                .body(TransactionsResponse.class);
    }
}
