package com.banking.system.repo;

import com.banking.system.dtos.Transaction;

import java.util.List;

public interface TransactionRepository {

    void save(Transaction transaction);

    List<Transaction> findByTransactionIds(List<String> transactionIds);
}
