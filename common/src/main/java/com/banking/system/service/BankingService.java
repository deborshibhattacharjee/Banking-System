package com.banking.system.service;

import java.math.BigDecimal;
import java.util.List;

public interface BankingService {

    void createAccount(String accountId, BigDecimal initialBalance);

    BigDecimal getBalance(String accountId);

    void transfer(String srcId, String destId, BigDecimal amount);

    void deposit(String accountId, BigDecimal amount);

    void withdraw(String accountId, BigDecimal amount);

    List<String> getTransactionHistory(String accountId);
}
