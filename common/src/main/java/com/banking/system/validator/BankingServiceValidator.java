package com.banking.system.validator;

import com.banking.system.dtos.Account;
import com.banking.system.exceptions.*;
import com.banking.system.repo.AccountRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static com.banking.system.enums.BankingProcessorError.*;
import static com.banking.system.utils.ValidationUtils.throwIfTrue;

public class BankingServiceValidator {

    private final AccountRepository accountRepository;

    public BankingServiceValidator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void validateAccountCreation(String accountId, BigDecimal initialBalance) {
        throwIfTrue((accountId==null || accountId.isBlank()), new InvalidAccountIdException(INVALID_ACCOUNT_ID));
        throwIfTrue(accountRepository.existsById(accountId), new DuplicateAccountException(DUPLICATE_ACCOUNT));
        throwIfTrue((initialBalance==null), new InvalidAmountException(ACCOUNT_BALANCE_CANNOT_BE_NULL));
        throwIfTrue((initialBalance.compareTo(BigDecimal.ZERO)<0),
                new InvalidAmountException(ACCOUNT_BALANCE_CANNOT_BE_NULL));
    }

    public void validateAccountIdExists(String accountId) {
        Optional<Account> account = accountRepository.fetchById(accountId);
        account.orElseThrow(() -> new AccountNotFoundException(ACCOUNT_NOT_FOUND));
    }
}
