package com.virgoai.pipeline.web;

import java.util.List;
import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.virgoai.pipeline.source.AccountsClient;
import com.virgoai.pipeline.source.AccountsResponse;
import com.virgoai.pipeline.source.TransactionsClient;
import com.virgoai.pipeline.source.TransactionsResponse;

@RestController
public class SourceCheckController {
    private final AccountsClient accounts;
    private final TransactionsClient transactions;

    public SourceCheckController(AccountsClient accounts, TransactionsClient transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    @GetMapping("/check/accounts")
    public List<AccountsResponse.Account> accounts(){
        return accounts.fetchAccounts();
    }

    @GetMapping("/check/transactions")
    public TransactionsResponse transactions() {
        String accountId = accounts.fetchAccounts().get(0).accountID();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return transactions.fetchPreviousDay(accountId, yesterday, yesterday);
    }
}