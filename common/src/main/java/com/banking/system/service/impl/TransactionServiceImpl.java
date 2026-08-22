package com.banking.system.service.impl;

import com.banking.system.dtos.Transaction;
import com.banking.system.repo.TransactionRepository;
import com.banking.system.service.TransactionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.lang.System.currentTimeMillis;

public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction recordTransaction(String accId1, String accId2, String transactionType,
                                         String transactionDirection, BigDecimal amount, BigDecimal balance) {
        Transaction transaction = new Transaction(UUID.randomUUID().toString(), accId1, transactionType,
                transactionDirection, amount, balance, accId2, currentTimeMillis());
        transactionRepository.save(transaction);
        return transaction;
    }

    @Override
    public List<Transaction> findByTransactionIds(List<String> transactionIds) {
        Optional<List<Transaction>> optionalTransactions =
                transactionRepository.findByTransactionId(transactionIds);
        return  optionalTransactions.orElse(List.of());
    }


}
