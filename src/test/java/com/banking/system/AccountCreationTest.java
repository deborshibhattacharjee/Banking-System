package com.banking.system;

import com.banking.system.exceptions.AccountNotFoundException;
import com.banking.system.exceptions.DuplicateAccountException;
import com.banking.system.exceptions.InvalidAccountIdException;
import com.banking.system.exceptions.InvalidAmountException;
import com.banking.system.service.BankingService;

import java.math.BigDecimal;


public class AccountCreationTest {

    public static void main(String[] args) {
        TestRunner.exit(run());
    }

    public static SuiteResult run() {
        AccountCreationTest tests = new AccountCreationTest();
        TestRunner runner = new TestRunner("Account creation");

        runner.test("createsAccountWithZeroInitialBalance", tests::createsAccountWithZeroInitialBalance);

        runner.test("createsAccountWithPositiveInitialBalance", tests::createsAccountWithPositiveInitialBalance);

        runner.test("rejectsDuplicateAccountId", tests::rejectsDuplicateAccountId);

        runner.test("rejectsNullAccountId", tests::rejectsNullAccountId);

        runner.test("rejectsEmptyAccountId", tests::rejectsEmptyAccountId);

        runner.test("rejectsBlankAccountId", tests::rejectsBlankAccountId);

        runner.test("rejectsNullInitialBalance", tests::rejectsNullInitialBalance);

        runner.test("rejectsNegativeInitialBalance", tests::rejectsNegativeInitialBalance);

        runner.test("rejectsBalanceQueryForUnknownAccount", tests::rejectsBalanceQueryForUnknownAccount);

        runner.test("keepsMultipleAccountBalancesIndependent", tests::keepsMultipleAccountBalancesIndependent);

        runner.test("invalidAccountCreationDoesNotCreateAccount", tests::invalidAccountCreationDoesNotCreateAccount);

        runner.test("acceptsSmallPositiveInitialBalance", tests::acceptsSmallPositiveInitialBalance);

        runner.test("negativeInitialBalanceReportsABalanceError", tests::negativeInitialBalanceReportsABalanceError);

        return runner.result();
    }

    private BankingService createService() {
        return TestFixtures.service();
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

    private void negativeInitialBalanceReportsABalanceError() {
        BankingService service = createService();
        try {
            service.createAccount("account-1", new BigDecimal("-5.00"));
            throw new AssertionError("A negative initial balance should be rejected");
        } catch (InvalidAmountException exception) {
            TestAssertions.assertTrue(exception.getMessage().contains("BP1008"),
                    "A negative balance should report the negative-balance code, not the null-balance code");
        }
    }
}
