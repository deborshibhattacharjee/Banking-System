package com.banking.system.validator;

import com.banking.system.dtos.Account;
import com.banking.system.exceptions.AccountNotFoundException;
import com.banking.system.exceptions.InvalidAccountIdException;
import com.banking.system.exceptions.InvalidAmountException;
import com.banking.system.repo.AccountRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static com.banking.system.enums.BankingSystemError.ACCOUNT_BALANCE_CANNOT_BE_NEGATIVE;
import static com.banking.system.enums.BankingSystemError.ACCOUNT_BALANCE_CANNOT_BE_NULL;
import static com.banking.system.enums.BankingSystemError.ACCOUNT_NOT_FOUND;
import static com.banking.system.enums.BankingSystemError.AMOUNT_MUST_BE_GREATER_THAN_ZERO;
import static com.banking.system.enums.BankingSystemError.INVALID_ACCOUNT_ID;
import static com.banking.system.utils.ValidationUtils.throwIfTrue;

public class BankingServiceValidator {

    private final AccountRepository accountRepository;

    public BankingServiceValidator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void validateAccountCreation(String accountId, BigDecimal initialBalance) {
        validateAccountId(accountId);
        throwIfTrue(initialBalance == null, () -> new InvalidAmountException(ACCOUNT_BALANCE_CANNOT_BE_NULL));
        throwIfTrue(initialBalance.compareTo(BigDecimal.ZERO) < 0,
                () -> new InvalidAmountException(ACCOUNT_BALANCE_CANNOT_BE_NEGATIVE));
    }

    public void validateAccountId(String accountId) {
        throwIfTrue(accountId == null || accountId.isBlank(),
                () -> new InvalidAccountIdException(INVALID_ACCOUNT_ID));
    }

    public Account validateAccountIdExists(String accountId) {
        validateAccountId(accountId);
        Optional<Account> account = accountRepository.fetchById(accountId);
        return account.orElseThrow(() -> new AccountNotFoundException(ACCOUNT_NOT_FOUND, accountId));
    }

    public void validateAmount(BigDecimal amount) {
        throwIfTrue(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0,
                () -> new InvalidAmountException(AMOUNT_MUST_BE_GREATER_THAN_ZERO));
    }
}
