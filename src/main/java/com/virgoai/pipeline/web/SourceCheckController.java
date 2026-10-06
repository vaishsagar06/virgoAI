package com.virgoai.pipeline.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.virgoai.pipeline.source.AccountsClient;
import com.virgoai.pipeline.source.AccountsResponse;

@RestController
public class SourceCheckController {
    private final AccountsClient accounts;

    public SourceCheckController(AccountsClient accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/check/accounts")
    public List<AccountsResponse.Account> accounts(){
        return accounts.fetchAccounts();
    }
}