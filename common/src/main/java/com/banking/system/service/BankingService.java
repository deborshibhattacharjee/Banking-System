package com.banking.system.service;

import com.banking.system.dtos.Account;

import java.math.BigDecimal;

public interface BankingService {

    void createAccount(String accountId, BigDecimal initialBalance);

    BigDecimal getBalance(String accountId);

    void deposit(String accountId, BigDecimal amount);

    void withdraw(String accountId, BigDecimal amount);
}
