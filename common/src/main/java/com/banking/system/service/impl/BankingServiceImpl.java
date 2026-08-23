package com.banking.system.service.impl;

import com.banking.system.concurrent.AccountLockManager;
import com.banking.system.dtos.Account;
import com.banking.system.dtos.Transaction;
import com.banking.system.enums.TransactionDirection;
import com.banking.system.enums.TransactionType;
import com.banking.system.exceptions.DuplicateAccountException;
import com.banking.system.exceptions.InsufficientFundsException;
import com.banking.system.exceptions.InvalidTransferException;
import com.banking.system.repo.AccountRepository;
import com.banking.system.service.BankingService;
import com.banking.system.service.TransactionService;
import com.banking.system.validator.BankingServiceValidator;

import java.math.BigDecimal;
import java.util.List;

import static com.banking.system.enums.BankingSystemError.DUPLICATE_ACCOUNT;
import static com.banking.system.enums.BankingSystemError.INSUFFICIENT_FUNDS_FOR_WITHDRAWAL;
import static com.banking.system.enums.BankingSystemError.INVALID_OPERATION_TRANSFER_TO_SAME_ACCOUNT;

public class BankingServiceImpl implements BankingService {

    private final AccountRepository accountRepository;
    private final BankingServiceValidator validator;
    private final TransactionService transactionService;
    private final AccountLockManager lockManager;

    public BankingServiceImpl(AccountRepository accountRepository, BankingServiceValidator validator,
                              TransactionService transactionService, AccountLockManager lockManager) {
        this.accountRepository = accountRepository;
        this.validator = validator;
        this.transactionService = transactionService;
        this.lockManager = lockManager;
    }

    @Override
    public void createAccount(String accountId, BigDecimal initialBalance) {
        validator.validateAccountCreation(accountId, initialBalance);
        Account account = new Account(accountId, initialBalance);
        if (!accountRepository.saveIfAbsent(account)) {
            throw new DuplicateAccountException(DUPLICATE_ACCOUNT);
        }
        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            lockManager.inAccountScope(accountId, () ->
                    record(account, TransactionType.DEPOSIT, TransactionDirection.CREDIT, initialBalance, null));
        }
    }

    @Override
    public BigDecimal getBalance(String accountId) {
        return validator.validateAccountIdExists(accountId).getBalance();
    }

    @Override
    public void deposit(String accountId, BigDecimal amount) {
        validator.validateAmount(amount);
        Account account = validator.validateAccountIdExists(accountId);
        lockManager.inAccountScope(accountId, () -> {
            account.add(amount);
            record(account, TransactionType.DEPOSIT, TransactionDirection.CREDIT, amount, null);
        });
    }

    @Override
    public void withdraw(String accountId, BigDecimal amount) {
        validator.validateAmount(amount);
        Account account = validator.validateAccountIdExists(accountId);
        lockManager.inAccountScope(accountId, () -> {
            requireSufficientFunds(account, amount);
            account.subtract(amount);
            record(account, TransactionType.WITHDRAWAL, TransactionDirection.DEBIT, amount, null);
        });
    }

    @Override
    public void transfer(String srcId, String destId, BigDecimal amount) {
        validator.validateAmount(amount);
        validator.validateAccountId(srcId);
        validator.validateAccountId(destId);
        if (srcId.equals(destId)) {
            throw new InvalidTransferException(INVALID_OPERATION_TRANSFER_TO_SAME_ACCOUNT);
        }
        Account source = validator.validateAccountIdExists(srcId);
        Account destination = validator.validateAccountIdExists(destId);
        lockManager.inAccountScope(srcId, destId, () -> {
            requireSufficientFunds(source, amount);
            source.subtract(amount);
            destination.add(amount);
            record(source, TransactionType.TRANSFER, TransactionDirection.DEBIT, amount, destId);
            record(destination, TransactionType.TRANSFER, TransactionDirection.CREDIT, amount, srcId);
        });
    }

    @Override
    public List<Transaction> getTransactionHistory(String accountId) {
        Account account = validator.validateAccountIdExists(accountId);
        return transactionService.findByTransactionIds(account.getTransactions());
    }

    private void requireSufficientFunds(Account account, BigDecimal amount) {
        if (!account.hasAtLeast(amount)) {
            throw new InsufficientFundsException(INSUFFICIENT_FUNDS_FOR_WITHDRAWAL, account.getId());
        }
    }

    private void record(Account account, TransactionType type, TransactionDirection direction,
                        BigDecimal amount, String relatedAccountId) {
        Transaction transaction = transactionService.recordTransaction(account.getId(), relatedAccountId,
                type, direction, amount, account.getBalance());
        account.addToTransactions(transaction.getTransactionId());
    }
}
