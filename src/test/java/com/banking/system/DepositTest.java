package com.banking.system;

import com.banking.system.database.InMemoryAccountDatabase;
import com.banking.system.database.InMemoryTransactionDatabase;
import com.banking.system.exceptions.AccountNotFoundException;
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

public class DepositTest {

    public static void main(String[] args) {
        DepositTest tests = new DepositTest();

        tests.runTest("depositsMoneyIntoAccount", tests::depositsMoneyIntoAccount);

        tests.runTest("depositIncreasesExistingBalance", tests::depositIncreasesExistingBalance);

        tests.runTest("allowsMultipleDeposits", tests::allowsMultipleDeposits);

        tests.runTest("allowsDepositsIntoMultipleAccounts", tests::allowsDepositsIntoMultipleAccounts);

        tests.runTest("rejectsDepositIntoUnknownAccount", tests::rejectsDepositIntoUnknownAccount);

        tests.runTest("rejectsNullDepositAmount", tests::rejectsNullDepositAmount);

        tests.runTest("rejectsNegativeDepositAmount", tests::rejectsNegativeDepositAmount);

        tests.runTest("rejectsZeroDepositAmount", tests::rejectsZeroDepositAmount);

        tests.runTest("failedDepositDoesNotChangeBalance", tests::failedDepositDoesNotChangeBalance);

        System.out.println();
        System.out.println("All deposit tests passed.");
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
            error.printStackTrace();
            throw error;
        }
    }

    private BankingService createService() {
        InMemoryAccountDatabase database = new InMemoryAccountDatabase();
        InMemoryTransactionDatabase transactionDatabase = new InMemoryTransactionDatabase();
        AccountRepository repository = new AccountRepositoryImpl(database);
        TransactionRepository transactionRepository = new TransactionRepositoryImpl(transactionDatabase);
        BankingServiceValidator validator = new BankingServiceValidator(repository);
        TransactionService transactionService = new TransactionServiceImpl(transactionRepository);
        return new BankingServiceImpl(repository, validator, transactionService);
    }

    private BankingService createServiceWithAccount(String accountId, BigDecimal initialBalance) {
        BankingService service = createService();
        service.createAccount(accountId, initialBalance);
        return service;
    }

    private void depositsMoneyIntoAccount() {
        BankingService service = createServiceWithAccount("account-1", BigDecimal.ZERO);
        service.deposit("account-1", new BigDecimal("100.00"));
        TestAssertions.assertBigDecimalEquals(new BigDecimal("100.00"), service.getBalance("account-1"),
                "A deposit should increase the account balance");
    }

    private void depositIncreasesExistingBalance() {
        BankingService service = createServiceWithAccount("account-1", new BigDecimal("50.00"));
        service.deposit("account-1", new BigDecimal("25.50"));
        TestAssertions.assertBigDecimalEquals(new BigDecimal("75.50"), service.getBalance("account-1"),
                "A deposit should be added to the existing balance");
    }

    private void allowsMultipleDeposits() {
        BankingService service = createServiceWithAccount("account-1", BigDecimal.ZERO);
        service.deposit("account-1", new BigDecimal("100.00"));
        service.deposit("account-1", new BigDecimal("50.50"));
        service.deposit("account-1", new BigDecimal("10.25"));
        TestAssertions.assertBigDecimalEquals(new BigDecimal("160.75"), service.getBalance("account-1"),
                "Multiple deposits should accumulate");
    }

    private void allowsDepositsIntoMultipleAccounts() {
        BankingService service = createService();
        service.createAccount("account-1", new BigDecimal("100.00"));
        service.createAccount("account-2", new BigDecimal("200.00"));
        service.deposit("account-1", new BigDecimal("50.00"));
        service.deposit("account-2", new BigDecimal("25.00"));

        TestAssertions.assertBigDecimalEquals(new BigDecimal("150.00"), service.getBalance("account-1"),
                "The first account should receive only its own deposit");

        TestAssertions.assertBigDecimalEquals(new BigDecimal("225.00"), service.getBalance("account-2"),
                "The second account should receive only its own deposit");
    }

    private void rejectsDepositIntoUnknownAccount() {
        BankingService service = createService();
        TestAssertions.assertThrows(AccountNotFoundException.class, () ->
                        service.deposit("unknown-account", new BigDecimal("100.00")),
                "A deposit into an unknown account should be rejected");
    }

    private void rejectsNullDepositAmount() {
        BankingService service = createServiceWithAccount("account-1", new BigDecimal("100.00"));
        TestAssertions.assertThrows(InvalidAmountException.class, () ->
                        service.deposit("account-1", null),
                "A null deposit amount should be rejected");
    }

    private void rejectsNegativeDepositAmount() {
        BankingService service = createServiceWithAccount("account-1", new BigDecimal("100.00"));

        TestAssertions.assertThrows(InvalidAmountException.class, () ->
                        service.deposit("account-1", new BigDecimal("-10.00")),
                "A negative deposit amount should be rejected");
    }

    private void rejectsZeroDepositAmount() {
        BankingService service = createServiceWithAccount("account-1", new BigDecimal("100.00"));

        TestAssertions.assertThrows(InvalidAmountException.class, () ->
                        service.deposit("account-1", BigDecimal.ZERO),
                "A zero deposit amount should be rejected"
        );
    }

    private void failedDepositDoesNotChangeBalance() {
        BankingService service = createServiceWithAccount("account-1", new BigDecimal("100.00"));
        TestAssertions.assertThrows(
                InvalidAmountException.class, () ->
                        service.deposit("account-1", new BigDecimal("-50.00")),
                "An invalid deposit should be rejected");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("100.00"), service.getBalance("account-1"),
                "A failed deposit should not change the balance");
    }
}

