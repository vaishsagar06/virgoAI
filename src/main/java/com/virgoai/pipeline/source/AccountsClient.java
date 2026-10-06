package com.virgoai.pipeline.source;    

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AccountsClient {
    
    private final RestClient http;

    public AccountsClient(@Value("${sources.usbank.base-url}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public List<AccountsResponse.Account> fetchAccounts() {

        AccountsResponse response = http.get()
        .uri("/accounts")
        .retrieve()
        .body(AccountsResponse.class);

        return response == null ? List.of() : response.accounts();

    }
}