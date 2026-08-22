package com.banking.system.service.impl;

import com.banking.system.dtos.Account;
import com.banking.system.exceptions.InsufficientFundsException;
import com.banking.system.exceptions.InvalidAmountException;
import com.banking.system.exceptions.InvalidTransferException;
import com.banking.system.repo.AccountRepository;
import com.banking.system.service.BankingService;
import com.banking.system.validator.BankingServiceValidator;

import java.math.BigDecimal;
import java.util.Optional;

import static com.banking.system.enums.BankingSystemError.*;

public class BankingServiceImpl implements BankingService {

    private final AccountRepository accountRepository;
    private final BankingServiceValidator validator;

    public BankingServiceImpl(AccountRepository accountRepository, BankingServiceValidator validator) {
        this.accountRepository = accountRepository;
        this.validator = validator;
    }

    @Override
    public void createAccount(String accountId, BigDecimal initialBalance) {
        validator.validateAccountCreation(accountId, initialBalance);
        Account account = new Account(accountId, initialBalance);
        accountRepository.save(account);
    }

    @Override
    public BigDecimal getBalance(String accountId) {
        validator.validateAccountIdExists(accountId);
        Optional<BigDecimal> balance = accountRepository.getBalance(accountId);
        return balance.orElse(BigDecimal.ZERO);
    }

    @Override
    public void transfer(String srcId, String destId, BigDecimal amount){
        if(srcId.equals(destId)){
            throw new InvalidTransferException(INVALID_OPERATION_TRANSFER_TO_SAME_ACCOUNT);
        }
        if(amount!=null && amount.compareTo(BigDecimal.ZERO) > 0){
            withdraw(srcId, amount);
            deposit(destId, amount);
        } else {
            throw new InvalidAmountException(AMOUNT_MUST_BE_GREATER_THAN_ZERO);
        }
    }

    @Override
    public void deposit(String accountId, BigDecimal amount) {
        validator.validateAccountIdExists(accountId);
        if (amount!=null && amount.compareTo(BigDecimal.ZERO) > 0) {
            Optional<Account> optionalAccount = accountRepository.fetchById(accountId);
            Account account = optionalAccount.get();
            account.add(amount);
        } else {
            throw new InvalidAmountException(AMOUNT_MUST_BE_GREATER_THAN_ZERO);
        }
    }

    @Override
    public void withdraw(String accountId, BigDecimal amount){
        validator.validateAccountIdExists(accountId);
        if (amount!=null && amount.compareTo(BigDecimal.ZERO) > 0) {
            Optional<Account> optionalAccount = accountRepository.fetchById(accountId);
            Account account = optionalAccount.get();
            if(account.getBalance().compareTo(amount)>=0){
                account.subtract(amount);
            } else{
                throw new InsufficientFundsException(INSUFFICIENT_FUNDS_FOR_WITHDRAWAL, accountId);
            }
        } else {
            throw new InvalidAmountException(AMOUNT_MUST_BE_GREATER_THAN_ZERO);
        }
    }
}
