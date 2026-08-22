package com.banking.system.repo.impl;

import com.banking.system.dtos.Account;
import com.banking.system.database.InMemoryAccountDatabase;
import com.banking.system.repo.AccountRepository;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public class AccountRepositoryImpl implements AccountRepository {

    private final InMemoryAccountDatabase database;

    public AccountRepositoryImpl(InMemoryAccountDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    @Override
    public Optional<Account> fetchById(String accountId) {
        return database.fetchById(accountId);
    }

    public Optional<BigDecimal> getBalance(String accountId) {
        return database.getBalance(accountId);
    }

    @Override
    public boolean existsById(String accountId) {
        return database.contains(accountId);
    }

    @Override
    public Account save(Account account) {
        database.save(account);
        Optional<Account> accountOptional = database.fetchById(account.getId());
        return accountOptional.orElse(null);
    }
}
