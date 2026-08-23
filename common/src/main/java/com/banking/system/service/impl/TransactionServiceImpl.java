package com.banking.system.service.impl;

import com.banking.system.dtos.Transaction;
import com.banking.system.enums.TransactionDirection;
import com.banking.system.enums.TransactionType;
import com.banking.system.repo.TransactionRepository;
import com.banking.system.service.TransactionService;

import java.math.BigDecimal;
import java.util.List;

import static java.lang.System.currentTimeMillis;

public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Transaction recordTransaction(String accountId, String relatedAccountId, TransactionType type,
                                         TransactionDirection direction, BigDecimal amount, BigDecimal balanceAfter) {
        Transaction transaction = new Transaction(java.util.UUID.randomUUID().toString(), accountId, type,
                direction, amount, balanceAfter, relatedAccountId, currentTimeMillis());
        transactionRepository.save(transaction);
        return transaction;
    }

    @Override
    public List<Transaction> findByTransactionIds(List<String> transactionIds) {
        return transactionRepository.findByTransactionIds(transactionIds);
    }
}
