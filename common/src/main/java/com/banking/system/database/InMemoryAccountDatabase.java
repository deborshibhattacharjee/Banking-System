package com.banking.system.database;

import com.banking.system.dtos.Account;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAccountDatabase {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    public Optional<Account> fetchById(String accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    public Optional<BigDecimal> getBalance(String accountId) {
        return Optional.ofNullable(accounts.get(accountId)).map(Account::getBalance);
    }

    public boolean contains(String accountId) {
        return accounts.containsKey(accountId);
    }

    public void save(Account account) {
        accounts.put(account.getId(), account);
    }

    public void clear() {
        accounts.clear();
    }
}
