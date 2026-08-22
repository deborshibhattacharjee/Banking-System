package com.banking.system.repo;

import com.banking.system.dtos.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {

    void save(Transaction transaction);

    Optional<List<Transaction>> findByTransactionId(List<String> transactionIds);
}
