package com.banking.system;

import com.banking.system.exceptions.AccountNotFoundException;
import com.banking.system.exceptions.InsufficientFundsException;
import com.banking.system.exceptions.InvalidAmountException;
import com.banking.system.exceptions.InvalidTransferException;
import com.banking.system.service.BankingService;

import java.math.BigDecimal;

public class TransferTest {

    public static void main(String[] args) {
        TestRunner.exit(run());
    }

    public static SuiteResult run() {
        TransferTest tests = new TransferTest();
        TestRunner runner = new TestRunner("Transfer");

        runner.test("transfersMoneyBetweenAccounts", tests::transfersMoneyBetweenAccounts);

        runner.test("updatesBothBalancesCorrectly", tests::updatesBothBalancesCorrectly);

        runner.test("allowsMultipleTransfers", tests::allowsMultipleTransfers);

        runner.test("allowsTransferOfExactSourceBalance", tests::allowsTransferOfExactSourceBalance);

        runner.test("rejectsTransferFromUnknownSourceAccount", tests::rejectsTransferFromUnknownSourceAccount);

        runner.test("rejectsTransferToUnknownDestinationAccount", tests::rejectsTransferToUnknownDestinationAccount);

        runner.test("rejectsNullTransferAmount", tests::rejectsNullTransferAmount);

        runner.test("rejectsNegativeTransferAmount", tests::rejectsNegativeTransferAmount);

        runner.test("rejectsZeroTransferAmount", tests::rejectsZeroTransferAmount);

        runner.test("rejectsTransferThatWouldCauseOverdraft", tests::rejectsTransferThatWouldCauseOverdraft);

        runner.test("failedTransferDoesNotChangeEitherBalance", tests::failedTransferDoesNotChangeEitherBalance);

        runner.test("keepsAccountsIndependentAfterTransfer", tests::keepsAccountsIndependentAfterTransfer);

        runner.test("rejectsTransferToSameAccount", tests::rejectsTransferToSameAccount);

        return runner.result();
    }

    private BankingService createService() {
        return TestFixtures.service();
    }

    private BankingService createServiceWithTwoAccounts() {
        BankingService service = createService();
        service.createAccount("account-1", new BigDecimal("100.00"));
        service.createAccount("account-2", new BigDecimal("50.00"));
        return service;
    }

    private void transfersMoneyBetweenAccounts() {
        BankingService service = createServiceWithTwoAccounts();
        service.transfer("account-1", "account-2", new BigDecimal("25.00"));
        TestAssertions.assertBigDecimalEquals(new BigDecimal("75.00"), service.getBalance("account-1"),
                "The source account should be reduced");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("75.00"), service.getBalance("account-2"),
                "The destination account should be increased");
    }

    private void updatesBothBalancesCorrectly() {
        BankingService service = createService();
        service.createAccount("source", new BigDecimal("250.75"));
        service.createAccount("destination", new BigDecimal("100.25"));
        service.transfer("source", "destination", new BigDecimal("50.50"));
        TestAssertions.assertBigDecimalEquals(new BigDecimal("200.25"), service.getBalance("source"),
                "The source balance should be reduced by the transfer amount");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("150.75"), service.getBalance("destination"),
                "The destination balance should be increased by the transfer amount");
    }

    private void allowsMultipleTransfers() {
        BankingService service = createService();
        service.createAccount("account-1", new BigDecimal("200.00"));
        service.createAccount("account-2", new BigDecimal("50.00"));
        service.transfer("account-1", "account-2", new BigDecimal("25.00"));
        service.transfer("account-1", "account-2", new BigDecimal("50.50"));
        service.transfer("account-2", "account-1", new BigDecimal("10.25"));
        TestAssertions.assertBigDecimalEquals(new BigDecimal("134.75"), service.getBalance("account-1"),
                "Multiple transfers should update the source balance correctly");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("115.25"), service.getBalance("account-2"),
                "Multiple transfers should update the destination balance correctly");
    }

    private void allowsTransferOfExactSourceBalance() {
        BankingService service = createService();
        service.createAccount("source", new BigDecimal("100.00"));
        service.createAccount("destination", BigDecimal.ZERO);
        service.transfer("source", "destination", new BigDecimal("100.00"));
        TestAssertions.assertBigDecimalEquals(BigDecimal.ZERO, service.getBalance("source"),
                "The source account should have a zero balance");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("100.00"), service.getBalance("destination"),
                "The destination should receive the full amount");
    }

    private void rejectsTransferFromUnknownSourceAccount() {
        BankingService service = createService();
        service.createAccount("destination", BigDecimal.ZERO);
        TestAssertions.assertThrows(AccountNotFoundException.class, () ->
                        service.transfer("unknown-source", "destination", new BigDecimal("25.00")),
                "A transfer from an unknown source account should be rejected");
    }

    private void rejectsTransferToUnknownDestinationAccount() {
        BankingService service = createService();
        service.createAccount("source", new BigDecimal("100.00"));
        TestAssertions.assertThrows(AccountNotFoundException.class, () ->
                        service.transfer("source", "unknown-destination", new BigDecimal("25.00")),
                "A transfer to an unknown destination account should be rejected");
    }

    private void rejectsNullTransferAmount() {
        BankingService service = createServiceWithTwoAccounts();
        TestAssertions.assertThrows(InvalidAmountException.class, () ->
                        service.transfer("account-1", "account-2", null),
                "A null transfer amount should be rejected");
    }

    private void rejectsNegativeTransferAmount() {
        BankingService service = createServiceWithTwoAccounts();
        TestAssertions.assertThrows(InvalidAmountException.class, () ->
                        service.transfer("account-1", "account-2", new BigDecimal("-10.00")),
                "A negative transfer amount should be rejected");
    }

    private void rejectsZeroTransferAmount() {
        BankingService service = createServiceWithTwoAccounts();
        TestAssertions.assertThrows(InvalidAmountException.class, () ->
                        service.transfer("account-1", "account-2", BigDecimal.ZERO),
                "A zero transfer amount should be rejected");
    }

    private void rejectsTransferThatWouldCauseOverdraft() {
        BankingService service = createServiceWithTwoAccounts();
        TestAssertions.assertThrows(InsufficientFundsException.class, () ->
                        service.transfer("account-1", "account-2", new BigDecimal("100.01")),
                "A transfer greater than the source balance should be rejected");
    }

    private void failedTransferDoesNotChangeEitherBalance() {
        BankingService service = createServiceWithTwoAccounts();
        TestAssertions.assertThrows(InsufficientFundsException.class, () ->
                        service.transfer("account-1", "account-2", new BigDecimal("150.00")),
                "An overdraft transfer should be rejected");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("100.00"), service.getBalance("account-1"),
                "The source balance should remain unchanged after a failed transfer");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("50.00"), service.getBalance("account-2"),
                "The destination balance should remain unchanged after a failed transfer");
    }

    private void keepsAccountsIndependentAfterTransfer() {
        BankingService service = createService();
        service.createAccount("account-1", new BigDecimal("100.00"));
        service.createAccount("account-2", new BigDecimal("200.00"));
        service.createAccount("account-3", new BigDecimal("300.00"));
        service.transfer("account-1", "account-2", new BigDecimal("25.00"));
        TestAssertions.assertBigDecimalEquals(new BigDecimal("75.00"), service.getBalance("account-1"),
                "Account 1 should reflect the transfer");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("225.00"), service.getBalance("account-2"),
                "Account 2 should receive the transfer");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("300.00"), service.getBalance("account-3"),
                "An unrelated account should remain unchanged");
    }

    private void rejectsTransferToSameAccount() {
        BankingService service = createService();
        service.createAccount("account-1", new BigDecimal("100.00"));
        TestAssertions.assertThrows(InvalidTransferException.class, () ->
                        service.transfer("account-1", "account-1", new BigDecimal("25.00")),
                "A transfer to the same account should be rejected");
    }


}
