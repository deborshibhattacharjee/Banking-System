package com.banking.system.database;

import com.banking.system.dtos.Transaction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTransactionDatabase {

    private final Map<String, Transaction> transactionsByAccount = new ConcurrentHashMap<>();

    public void save(Transaction transaction) {
        transactionsByAccount.put(transaction.getTransactionId(), transaction);
    }

    public Optional<List<Transaction>> findByTransactionId(List<String> transactionIds) {
        List<Transaction> transactions = transactionIds.stream().map(transactionsByAccount::get).toList();
        return Optional.of(Collections.unmodifiableList(new ArrayList<>(transactions)));
    }
}

