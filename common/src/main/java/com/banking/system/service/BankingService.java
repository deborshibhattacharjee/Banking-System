package com.banking.system.service;

import com.banking.system.bean.Account;

import java.math.BigDecimal;

public interface BankingService {

    Account createAccount(String accountId, BigDecimal initialBalance);

    BigDecimal getBalance(String accountId);
}
