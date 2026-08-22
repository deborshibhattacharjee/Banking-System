package com.banking.system;

import com.banking.system.database.InMemoryAccountDatabase;
import com.banking.system.database.InMemoryTransactionDatabase;
import com.banking.system.exceptions.AccountNotFoundException;
import com.banking.system.exceptions.DuplicateAccountException;
import com.banking.system.exceptions.InvalidAccountIdException;
import com.banking.system.exceptions.InvalidAmountException;
import com.banking.system.repo.AccountRepository;
import com.banking.system.repo.TransactionRepository;
import com.banking.system.repo.impl.AccountRepositoryImpl;
import com.banking.system.repo.impl.TransactionRepositoryImpl;
import com.banking.system.service.BankingService;
import com.banking.system.service.TransactionService;
import com.banking.system.service.impl.BankingServiceImpl;
import com.banking.system.service.impl.TransactionServiceImpl;
import com.banking.system.validator.BankingServiceValidator;

import java.math.BigDecimal;


public class AccountCreationTest {

    public static void main(String[] args) {
        AccountCreationTest tests = new AccountCreationTest();

        tests.runTest("createsAccountWithZeroInitialBalance", tests::createsAccountWithZeroInitialBalance);

        tests.runTest("createsAccountWithPositiveInitialBalance", tests::createsAccountWithPositiveInitialBalance);

        tests.runTest("rejectsDuplicateAccountId", tests::rejectsDuplicateAccountId);

        tests.runTest("rejectsNullAccountId", tests::rejectsNullAccountId);

        tests.runTest("rejectsEmptyAccountId", tests::rejectsEmptyAccountId);

        tests.runTest("rejectsBlankAccountId", tests::rejectsBlankAccountId);

        tests.runTest("rejectsNullInitialBalance", tests::rejectsNullInitialBalance);

        tests.runTest("rejectsNegativeInitialBalance", tests::rejectsNegativeInitialBalance);

        tests.runTest("rejectsBalanceQueryForUnknownAccount", tests::rejectsBalanceQueryForUnknownAccount);

        tests.runTest("keepsMultipleAccountBalancesIndependent", tests::keepsMultipleAccountBalancesIndependent);

        tests.runTest("invalidAccountCreationDoesNotCreateAccount", tests::invalidAccountCreationDoesNotCreateAccount);

        tests.runTest("acceptsSmallPositiveInitialBalance", tests::acceptsSmallPositiveInitialBalance);

        System.out.println();
        System.out.println("All account creation tests passed.");
    }

    private void runTest(String testName, Runnable test) {
        try {
            test.run();
            System.out.println("PASS: " + testName);
        } catch (AssertionError error) {
            System.out.println("FAIL: " + testName);
            System.out.println("       " + error.getMessage());
            throw error;
        } catch (RuntimeException error) {
            System.out.println("ERROR: " + testName);
            throw error;
        }
    }

    private BankingService createService(){
        InMemoryAccountDatabase  inMemoryAccountDatabase = new InMemoryAccountDatabase();
        InMemoryTransactionDatabase   inMemoryTransactionDatabase = new InMemoryTransactionDatabase();
        AccountRepository accountRepository = new AccountRepositoryImpl(inMemoryAccountDatabase);
        TransactionRepository transactionRepository = new TransactionRepositoryImpl(inMemoryTransactionDatabase);
        BankingServiceValidator bankingServiceValidator = new BankingServiceValidator(accountRepository);
        TransactionService transactionService = new TransactionServiceImpl(transactionRepository);
        return new BankingServiceImpl(accountRepository, bankingServiceValidator, transactionService);
    }

    void createsAccountWithZeroInitialBalance() {
        BankingService service = createService();
        service.createAccount("account-1", new BigDecimal("0.00"));
        TestAssertions.assertBigDecimalEquals(new BigDecimal("0.00"), service.getBalance("account-1"),
                "A newly created account should have a zero balance");
    }

    void createsAccountWithPositiveInitialBalance() {
        BankingService service = createService();
        service.createAccount("account-1", new BigDecimal("100.00"));
        TestAssertions.assertBigDecimalEquals(new BigDecimal("100.00"), service.getBalance("account-1"),
                "A newly created account should have its initial balance");
    }

    void rejectsDuplicateAccountId() {
        BankingService service = createService();
        service.createAccount("account-1", new BigDecimal("100.00"));
        TestAssertions.assertThrows(DuplicateAccountException.class, () ->
                        service.createAccount("account-1", new BigDecimal("200.00")),
                "Creating an account with an existing ID should be rejected");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("100.00"), service.getBalance("account-1"),
                "The original account balance should remain unchanged");
    }

    void rejectsNullAccountId() {
        BankingService service = createService();
        TestAssertions.assertThrows(InvalidAccountIdException.class, () ->
                service.createAccount(null, new BigDecimal("100.00")),
                "A null account ID should be rejected");
    }

    void rejectsEmptyAccountId() {
        BankingService service = createService();
        TestAssertions.assertThrows(InvalidAccountIdException.class,
                () -> service.createAccount("", new BigDecimal("100.00")),
                "An empty account ID should be rejected");
    }

    void rejectsBlankAccountId() {
        BankingService service = createService();
        TestAssertions.assertThrows(InvalidAccountIdException.class, () ->
                        service.createAccount("   ", new BigDecimal("100.00")),
                "A whitespace-only account ID should be rejected");
    }

    void rejectsNullInitialBalance() {
        BankingService service = createService();
        TestAssertions.assertThrows(InvalidAmountException.class, () ->
                        service.createAccount("account-3", null), "A null initial balance should be rejected");
    }

    void rejectsNegativeInitialBalance() {
        BankingService service = createService();
        TestAssertions.assertThrows(InvalidAmountException.class, () ->
                        service.createAccount("account-3", new BigDecimal("-1.00")),
                "A negative initial balance should be rejected"
        );
    }

    void rejectsBalanceQueryForUnknownAccount() {
        BankingService service = createService();
        TestAssertions.assertThrows(AccountNotFoundException.class, () ->
                        service.getBalance("unknown-account"),
                "Querying an unknown account should be rejected");
    }

    void keepsMultipleAccountBalancesIndependent() {
        BankingService service = createService();
        service.createAccount("account-1", new BigDecimal("100.00"));
        service.createAccount("account-2", new BigDecimal("250.00"));
        TestAssertions.assertBigDecimalEquals(
                new BigDecimal("100.00"), service.getBalance("account-1"),
                "Account 1 balance should remain independent");
        TestAssertions.assertBigDecimalEquals(
                new BigDecimal("250.00"), service.getBalance("account-2"),
                "Account 2 balance should remain independent");
    }

    void invalidAccountCreationDoesNotCreateAccount() {
        BankingService service = createService();
        TestAssertions.assertThrows(
                InvalidAmountException.class, () ->
                        service.createAccount("account-1", new BigDecimal("-10.00")),
                "Invalid account creation should be rejected");
        TestAssertions.assertThrows(AccountNotFoundException.class, () -> service.getBalance("account-1"),
                "An account should not exist after failed creation");
    }

    void acceptsSmallPositiveInitialBalance() {
        BankingService service = createService();
        service.createAccount("account-1", new BigDecimal("0.01"));
        TestAssertions.assertBigDecimalEquals(
                new BigDecimal("0.01"), service.getBalance("account-1"),
                "A small positive initial balance should be accepted"
        );
    }
}
