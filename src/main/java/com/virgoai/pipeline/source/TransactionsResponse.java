package com.virgoai.pipeline.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionsResponse(List<Transaction> transactions, PageMeta pageMeta) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transaction(
            String accountID,
            String transactionDate,
            String debitCreditIndicator,
            String transactionDescription,
            String transactionType,
            @JsonProperty("BAICode") String baiCode,
            String transactionReferenceNumber,
            String bankReferenceNumber,
            BigDecimal amount,
            String currency,
            WireDetails wiresTransactionDetails) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WireDetails(String usBankCharges) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PageMeta(int pageNumber, int pageSize, int pageCount, int totalCount) {
    }
}
