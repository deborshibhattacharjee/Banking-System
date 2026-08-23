package com.banking.system;

import com.banking.system.exceptions.AccountNotFoundException;
import com.banking.system.exceptions.InvalidAmountException;
import com.banking.system.service.BankingService;

import java.math.BigDecimal;

public class DepositTest {

    public static void main(String[] args) {
        TestRunner.exit(run());
    }

    public static SuiteResult run() {
        DepositTest tests = new DepositTest();
        TestRunner runner = new TestRunner("Deposit");

        runner.test("depositsMoneyIntoAccount", tests::depositsMoneyIntoAccount);

        runner.test("depositIncreasesExistingBalance", tests::depositIncreasesExistingBalance);

        runner.test("allowsMultipleDeposits", tests::allowsMultipleDeposits);

        runner.test("allowsDepositsIntoMultipleAccounts", tests::allowsDepositsIntoMultipleAccounts);

        runner.test("rejectsDepositIntoUnknownAccount", tests::rejectsDepositIntoUnknownAccount);

        runner.test("rejectsNullDepositAmount", tests::rejectsNullDepositAmount);

        runner.test("rejectsNegativeDepositAmount", tests::rejectsNegativeDepositAmount);

        runner.test("rejectsZeroDepositAmount", tests::rejectsZeroDepositAmount);

        runner.test("failedDepositDoesNotChangeBalance", tests::failedDepositDoesNotChangeBalance);

        return runner.result();
    }

    private BankingService createService() {
        return TestFixtures.service();
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

