package com.banking.system.service.impl;

import com.banking.system.dtos.Account;
import com.banking.system.dtos.Transaction;
import com.banking.system.enums.TransactionDirection;
import com.banking.system.enums.TransactionType;
import com.banking.system.exceptions.InsufficientFundsException;
import com.banking.system.exceptions.InvalidTransferException;
import com.banking.system.repo.AccountRepository;
import com.banking.system.service.BankingService;
import com.banking.system.service.TransactionService;
import com.banking.system.validator.BankingServiceValidator;

import java.math.BigDecimal;
import java.util.List;

import static com.banking.system.enums.BankingSystemError.*;

public class BankingServiceImpl implements BankingService {

    private final AccountRepository accountRepository;
    private final BankingServiceValidator validator;
    private final TransactionService transactionService;

    public BankingServiceImpl(AccountRepository accountRepository, BankingServiceValidator validator,
                              TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.validator = validator;
        this.transactionService = transactionService;
    }

    @Override
    public void createAccount(String accountId, BigDecimal initialBalance) {
        validator.validateAccountCreation(accountId, initialBalance);
        Account account = new Account(accountId, initialBalance);
        accountRepository.save(account);
        if(account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            Transaction transaction = transactionService.recordTransaction(accountId, null,
                    TransactionType.DEPOSIT.name(), TransactionDirection.CREDIT.name(), initialBalance,
                    account.getBalance());
            account.addToTransactions(transaction.getTransactionId());
        }
    }

    @Override
    public BigDecimal getBalance(String accountId) {
        Account account = validator.validateAccountIdExists(accountId);
        return account.getBalance();
    }

    @Override
    public void transfer(String srcId, String destId, BigDecimal amount) {
        if (srcId.equals(destId)) {
            throw new InvalidTransferException(INVALID_OPERATION_TRANSFER_TO_SAME_ACCOUNT);
        }
        validator.validateAmount(amount);
        Account srcAcc =  validator.validateAccountIdExists(srcId);
        Account destAcc = validator.validateAccountIdExists(destId);
        if (srcAcc.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(INSUFFICIENT_FUNDS_FOR_WITHDRAWAL, srcId);
        }
        srcAcc.subtract(amount);
        destAcc.add(amount);
        Transaction sourceTransaction = transactionService.recordTransaction(srcId, destId, TransactionType.TRANSFER.name(),
                TransactionDirection.DEBIT.name(), amount, srcAcc.getBalance());
        srcAcc.addToTransactions(sourceTransaction.getTransactionId());
        Transaction destTransaction = transactionService.recordTransaction(destId, srcId, TransactionType.TRANSFER.name(),
                TransactionDirection.CREDIT.name(), amount, destAcc.getBalance());
        destAcc.addToTransactions(destTransaction.getTransactionId());
    }

    @Override
    public void deposit(String accountId, BigDecimal amount) {
        validator.validateAmount(amount);
        Account account = validator.validateAccountIdExists(accountId);
        account.add(amount);
        Transaction transaction = transactionService.recordTransaction(accountId, null,
                TransactionType.DEPOSIT.name(), TransactionDirection.CREDIT.name(), amount, account.getBalance());
        account.addToTransactions(transaction.getTransactionId());
    }

    @Override
    public void withdraw(String accountId, BigDecimal amount) {
        validator.validateAmount(amount);
        Account account = validator.validateAccountIdExists(accountId);
        if (account.getBalance().compareTo(amount) >= 0) {
            account.subtract(amount);
            Transaction transaction = transactionService.recordTransaction(accountId, null,
                    TransactionType.WITHDRAWAL.name(), TransactionDirection.DEBIT.name(), amount, account.getBalance());
            account.addToTransactions(transaction.getTransactionId());
        } else {
            throw new InsufficientFundsException(INSUFFICIENT_FUNDS_FOR_WITHDRAWAL, accountId);
        }
    }

    @Override
    public List<String> getTransactionHistory(String accountId) {
        Account account = validator.validateAccountIdExists(accountId);
        return account.getTransactions();
    }
}
