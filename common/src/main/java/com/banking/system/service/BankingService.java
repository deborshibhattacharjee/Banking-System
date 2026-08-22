package com.banking.system.service;

import com.banking.system.dtos.Account;

import java.math.BigDecimal;

public interface BankingService {

    Account createAccount(String accountId, BigDecimal initialBalance);

    BigDecimal getBalance(String accountId);

    Account deposit(String accountId, BigDecimal amount);
}
