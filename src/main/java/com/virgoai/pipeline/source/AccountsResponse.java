package com.virgoai.pipeline.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountsResponse (List<Account> accounts) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Account (String accountID,
            String accountName,
            String accountNumber,
            String routingNumber,
            String bankName,
            String accountType,
            String currency) {
                
            }
}
