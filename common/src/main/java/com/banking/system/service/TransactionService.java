package com.banking.system.service;

import com.banking.system.dtos.Transaction;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {

    Transaction recordTransaction(String accId1, String accId2, String transactionType,
                                  String transactionDirection, BigDecimal amount, BigDecimal balance);

    List<Transaction> findByTransactionIds(List<String> transactionIds);
}
