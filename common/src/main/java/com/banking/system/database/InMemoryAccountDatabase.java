package com.banking.system.database;

import com.banking.system.dtos.Account;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAccountDatabase {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    public Optional<Account> fetchById(String accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    public boolean saveIfAbsent(Account account) {
        return accounts.putIfAbsent(account.getId(), account) == null;
    }
}
