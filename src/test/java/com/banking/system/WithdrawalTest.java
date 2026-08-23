package com.banking.system;

import com.banking.system.exceptions.AccountNotFoundException;
import com.banking.system.exceptions.InsufficientFundsException;
import com.banking.system.exceptions.InvalidAmountException;
import com.banking.system.service.BankingService;

import java.math.BigDecimal;

public class WithdrawalTest {

    public static void main(String[] args) {
        TestRunner.exit(run());
    }

    public static SuiteResult run() {
        WithdrawalTest tests = new WithdrawalTest();
        TestRunner runner = new TestRunner("Withdrawal");

        runner.test("withdrawsMoneyFromAccount", tests::withdrawsMoneyFromAccount);

        runner.test("withdrawalDecreasesExistingBalance", tests::withdrawalDecreasesExistingBalance);

        runner.test("allowsMultipleWithdrawals", tests::allowsMultipleWithdrawals);

        runner.test("allowsWithdrawalOfExactBalance", tests::allowsWithdrawalOfExactBalance);

        runner.test("keepsMultipleAccountBalancesIndependent", tests::keepsMultipleAccountBalancesIndependent);

        runner.test("rejectsWithdrawalFromUnknownAccount", tests::rejectsWithdrawalFromUnknownAccount);

        runner.test("rejectsNullWithdrawalAmount", tests::rejectsNullWithdrawalAmount);

        runner.test("rejectsNegativeWithdrawalAmount", tests::rejectsNegativeWithdrawalAmount);

        runner.test("rejectsZeroWithdrawalAmount", tests::rejectsZeroWithdrawalAmount);

        runner.test("rejectsWithdrawalThatWouldCauseOverdraft", tests::rejectsWithdrawalThatWouldCauseOverdraft);

        runner.test("failedWithdrawalDoesNotChangeBalance", tests::failedWithdrawalDoesNotChangeBalance);

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

    private void withdrawsMoneyFromAccount() {
        BankingService service = createServiceWithAccount("account-1", new BigDecimal("100.00"));
        service.withdraw("account-1", new BigDecimal("25.00"));
        TestAssertions.assertBigDecimalEquals(new BigDecimal("75.00"), service.getBalance("account-1"),
                "A withdrawal should reduce the account balance");
    }

    private void withdrawalDecreasesExistingBalance() {
        BankingService service = createServiceWithAccount("account-1", new BigDecimal("100.00"));
        service.withdraw("account-1", new BigDecimal("40.50"));
        TestAssertions.assertBigDecimalEquals(new BigDecimal("59.50"), service.getBalance("account-1"),
                "The withdrawal amount should be deducted from the balance");
    }

    private void allowsMultipleWithdrawals() {
        BankingService service = createServiceWithAccount("account-1", new BigDecimal("200.00"));
        service.withdraw("account-1", new BigDecimal("50.00"));
        service.withdraw("account-1", new BigDecimal("25.50"));
        service.withdraw("account-1", new BigDecimal("10.25"));
        TestAssertions.assertBigDecimalEquals(new BigDecimal("114.25"), service.getBalance("account-1"),
                "Multiple withdrawals should accumulate correctly");
    }

    private void allowsWithdrawalOfExactBalance() {
        BankingService service = createServiceWithAccount("account-1", new BigDecimal("100.00"));
        service.withdraw("account-1", new BigDecimal("100.00"));
        TestAssertions.assertBigDecimalEquals(BigDecimal.ZERO, service.getBalance("account-1"),
                "A withdrawal equal to the balance should be allowed");
    }

    private void keepsMultipleAccountBalancesIndependent() {
        BankingService service = createService();
        service.createAccount("account-1", new BigDecimal("100.00"));
        service.createAccount("account-2", new BigDecimal("200.00"));
        service.withdraw("account-1", new BigDecimal("25.00"));
        TestAssertions.assertBigDecimalEquals(new BigDecimal("75.00"), service.getBalance("account-1"),
                "The first account balance should be reduced");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("200.00"), service.getBalance("account-2"),
                "The second account should remain unchanged");
    }

    private void rejectsWithdrawalFromUnknownAccount() {
        BankingService service = createService();
        TestAssertions.assertThrows(AccountNotFoundException.class, () ->
                        service.withdraw("unknown-account", new BigDecimal("50.00")),
                "A withdrawal from an unknown account should be rejected");
    }

    private void rejectsNullWithdrawalAmount() {
        BankingService service = createServiceWithAccount("account-1", new BigDecimal("100.00"));
        TestAssertions.assertThrows(InvalidAmountException.class, () -> service.withdraw("account-1", null),
                "A null withdrawal amount should be rejected");
    }

    private void rejectsNegativeWithdrawalAmount() {
        BankingService service = createServiceWithAccount("account-1", new BigDecimal("100.00"));
        TestAssertions.assertThrows(InvalidAmountException.class, () ->
                        service.withdraw("account-1", new BigDecimal("-10.00")),
                "A negative withdrawal amount should be rejected");
    }

    private void rejectsZeroWithdrawalAmount() {
        BankingService service = createServiceWithAccount("account-1", new BigDecimal("100.00"));
        TestAssertions.assertThrows(InvalidAmountException.class, () -> service.withdraw("account-1", BigDecimal.ZERO),
                "A zero withdrawal amount should be rejected");
    }

    private void rejectsWithdrawalThatWouldCauseOverdraft() {
        BankingService service = createServiceWithAccount("account-1", new BigDecimal("100.00"));
        TestAssertions.assertThrows(InsufficientFundsException.class, () ->
                        service.withdraw("account-1", new BigDecimal("100.01")),
                "A withdrawal greater than the balance should be rejected");
    }

    private void failedWithdrawalDoesNotChangeBalance() {
        BankingService service = createServiceWithAccount("account-1", new BigDecimal("100.00"));
        TestAssertions.assertThrows(InsufficientFundsException.class, () ->
                        service.withdraw("account-1", new BigDecimal("150.00")),
                "An overdraft withdrawal should be rejected");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("100.00"), service.getBalance("account-1"),
                "A failed withdrawal should not change the balance");
    }
}

