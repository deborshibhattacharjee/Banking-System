package com.banking.system.service;

import com.banking.system.dtos.Transaction;
import com.banking.system.enums.TransactionDirection;
import com.banking.system.enums.TransactionType;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {

    Transaction recordTransaction(String accountId, String relatedAccountId, TransactionType type,
                                  TransactionDirection direction, BigDecimal amount, BigDecimal balanceAfter);

    List<Transaction> findByTransactionIds(List<String> transactionIds);
}
