package com.banking.system.repo.impl;

import com.banking.system.database.InMemoryAccountDatabase;
import com.banking.system.dtos.Account;
import com.banking.system.repo.AccountRepository;

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

    @Override
    public boolean saveIfAbsent(Account account) {
        return database.saveIfAbsent(account);
    }
}
