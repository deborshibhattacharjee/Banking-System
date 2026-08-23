package com.banking.system.factory;

import com.banking.system.concurrent.AccountLockManager;
import com.banking.system.database.InMemoryAccountDatabase;
import com.banking.system.database.InMemoryTransactionDatabase;
import com.banking.system.repo.AccountRepository;
import com.banking.system.repo.TransactionRepository;
import com.banking.system.repo.impl.AccountRepositoryImpl;
import com.banking.system.repo.impl.TransactionRepositoryImpl;
import com.banking.system.service.BankingService;
import com.banking.system.service.TransactionService;
import com.banking.system.service.impl.BankingServiceImpl;
import com.banking.system.service.impl.TransactionServiceImpl;
import com.banking.system.validator.BankingServiceValidator;

public final class BankingSystemFactory {

    private BankingSystemFactory() {
    }

    public static BankingService newBankingService() {
        AccountRepository accountRepository = new AccountRepositoryImpl(new InMemoryAccountDatabase());
        TransactionRepository transactionRepository = new TransactionRepositoryImpl(new InMemoryTransactionDatabase());
        BankingServiceValidator validator = new BankingServiceValidator(accountRepository);
        TransactionService transactionService = new TransactionServiceImpl(transactionRepository);
        return new BankingServiceImpl(accountRepository, validator, transactionService, new AccountLockManager());
    }
}
