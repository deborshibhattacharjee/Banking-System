package com.banking.system.database;

import com.banking.system.dtos.Transaction;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTransactionDatabase {

    private final Map<String, Transaction> transactionsById = new ConcurrentHashMap<>();

    public void save(Transaction transaction) {
        transactionsById.put(transaction.getTransactionId(), transaction);
    }

    public List<Transaction> findByTransactionIds(List<String> transactionIds) {
        return transactionIds.stream()
                .map(transactionsById::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
