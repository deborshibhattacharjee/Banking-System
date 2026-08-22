package com.banking.system.service.impl;

import com.banking.system.dtos.Account;
import com.banking.system.exceptions.InvalidAmountException;
import com.banking.system.repo.AccountRepository;
import com.banking.system.service.BankingService;
import com.banking.system.validator.BankingServiceValidator;

import java.math.BigDecimal;
import java.util.Optional;

import static com.banking.system.enums.BankingProcessorError.DEPOSIT_AMOUNT_MUST_BE_GREATER_THAN_ZERO;

public class BankingServiceImpl implements BankingService {

    private final AccountRepository accountRepository;
    private final BankingServiceValidator validator;

    public BankingServiceImpl(AccountRepository accountRepository, BankingServiceValidator validator) {
        this.accountRepository = accountRepository;
        this.validator = validator;
    }

    @Override
    public Account createAccount(String accountId, BigDecimal initialBalance) {
        validator.validateAccountCreation(accountId, initialBalance);
        Account account = new Account(accountId, initialBalance);
        return accountRepository.save(account);
    }

    @Override
    public BigDecimal getBalance(String accountId) {
        validator.validateAccountIdExists(accountId);
        Optional<BigDecimal> balance = accountRepository.getBalance(accountId);
        return balance.orElse(BigDecimal.ZERO);
    }

    @Override
    public Account deposit(String accountId, BigDecimal amount) {
        validator.validateAccountIdExists(accountId);
        if (amount!=null && amount.compareTo(BigDecimal.ZERO) > 0) {
            Optional<Account> optionalAccount = accountRepository.fetchById(accountId);
            Account account = optionalAccount.get();
            account.add(amount);
            return account;
        }
        throw new InvalidAmountException(DEPOSIT_AMOUNT_MUST_BE_GREATER_THAN_ZERO);
    }
}
