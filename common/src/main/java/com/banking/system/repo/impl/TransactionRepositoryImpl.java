package com.banking.system.repo.impl;

import com.banking.system.database.InMemoryTransactionDatabase;
import com.banking.system.dtos.Transaction;
import com.banking.system.repo.TransactionRepository;

import java.util.List;
import java.util.Optional;

public class TransactionRepositoryImpl implements TransactionRepository {

    private final InMemoryTransactionDatabase inMemoryTransactionDatabase;

    public TransactionRepositoryImpl(InMemoryTransactionDatabase inMemoryTransactionDatabase) {
        this.inMemoryTransactionDatabase = inMemoryTransactionDatabase;
    }

    @Override
    public void save(Transaction transaction) {
        inMemoryTransactionDatabase.save(transaction);
    }

    @Override
    public Optional<List<Transaction>> findByTransactionId(List<String> transactionIds) {
        return inMemoryTransactionDatabase.findByTransactionId(transactionIds);
    }
}
