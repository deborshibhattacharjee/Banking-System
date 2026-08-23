package com.banking.system.repo.impl;

import com.banking.system.database.InMemoryTransactionDatabase;
import com.banking.system.dtos.Transaction;
import com.banking.system.repo.TransactionRepository;

import java.util.List;
import java.util.Objects;

public class TransactionRepositoryImpl implements TransactionRepository {

    private final InMemoryTransactionDatabase database;

    public TransactionRepositoryImpl(InMemoryTransactionDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    @Override
    public void save(Transaction transaction) {
        database.save(transaction);
    }

    @Override
    public List<Transaction> findByTransactionIds(List<String> transactionIds) {
        return database.findByTransactionIds(transactionIds);
    }
}
