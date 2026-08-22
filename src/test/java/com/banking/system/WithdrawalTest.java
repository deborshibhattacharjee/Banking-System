package com.banking.system;

import com.banking.system.database.InMemoryAccountDatabase;
import com.banking.system.exceptions.AccountNotFoundException;
import com.banking.system.exceptions.InsufficientFundsException;
import com.banking.system.exceptions.InvalidAmountException;
import com.banking.system.repo.AccountRepository;
import com.banking.system.repo.impl.AccountRepositoryImpl;
import com.banking.system.service.BankingService;
import com.banking.system.service.impl.BankingServiceImpl;
import com.banking.system.validator.BankingServiceValidator;

import java.math.BigDecimal;

public class WithdrawalTest {

    public static void main(String[] args) {
        WithdrawalTest tests = new WithdrawalTest();

        tests.runTest("withdrawsMoneyFromAccount", tests::withdrawsMoneyFromAccount);

        tests.runTest("withdrawalDecreasesExistingBalance", tests::withdrawalDecreasesExistingBalance);

        tests.runTest("allowsMultipleWithdrawals", tests::allowsMultipleWithdrawals);

        tests.runTest("allowsWithdrawalOfExactBalance", tests::allowsWithdrawalOfExactBalance);

        tests.runTest("keepsMultipleAccountBalancesIndependent", tests::keepsMultipleAccountBalancesIndependent);

        tests.runTest("rejectsWithdrawalFromUnknownAccount", tests::rejectsWithdrawalFromUnknownAccount);

        tests.runTest("rejectsNullWithdrawalAmount", tests::rejectsNullWithdrawalAmount);

        tests.runTest("rejectsNegativeWithdrawalAmount", tests::rejectsNegativeWithdrawalAmount);

        tests.runTest("rejectsZeroWithdrawalAmount", tests::rejectsZeroWithdrawalAmount);

        tests.runTest("rejectsWithdrawalThatWouldCauseOverdraft", tests::rejectsWithdrawalThatWouldCauseOverdraft);

        tests.runTest("failedWithdrawalDoesNotChangeBalance", tests::failedWithdrawalDoesNotChangeBalance);

        System.out.println();
        System.out.println("All withdrawal tests passed.");
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
        AccountRepository repository = new AccountRepositoryImpl(database);
        BankingServiceValidator validator = new BankingServiceValidator(repository);
        return new BankingServiceImpl(repository, validator);
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

