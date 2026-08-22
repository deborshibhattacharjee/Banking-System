package com.banking.system.service.impl;

import com.banking.system.bean.Account;
import com.banking.system.repo.AccountRepository;
import com.banking.system.service.BankingService;
import com.banking.system.validator.BankingServiceValidator;

import java.math.BigDecimal;
import java.util.Optional;

public class BankingServiceImpl implements BankingService {

    private final AccountRepository accountRepository;
    private final BankingServiceValidator  validator;

    public  BankingServiceImpl(AccountRepository accountRepository, BankingServiceValidator validator) {
        this.accountRepository = accountRepository;
        this.validator = validator;
    }

    @Override
    public Account createAccount(String accountId, BigDecimal initialBalance) {
        validator.validateAccountCreation(accountId,  initialBalance);
        Account account = new Account(accountId, initialBalance);
        return accountRepository.save(account);
    }

    @Override
    public BigDecimal getBalance(String accountId) {
        validator.validateAccountIdExists(accountId);
        Optional<BigDecimal> balance = accountRepository.getBalance(accountId);
        return balance.orElse(BigDecimal.ZERO);
    }
}
