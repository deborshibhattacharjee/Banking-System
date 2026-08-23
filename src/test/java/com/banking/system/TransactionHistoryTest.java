package com.banking.system;

import com.banking.system.database.InMemoryTransactionDatabase;
import com.banking.system.dtos.Transaction;
import com.banking.system.enums.TransactionDirection;
import com.banking.system.enums.TransactionType;
import com.banking.system.exceptions.AccountNotFoundException;
import com.banking.system.exceptions.InsufficientFundsException;
import com.banking.system.exceptions.InvalidAmountException;
import com.banking.system.service.BankingService;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.banking.system.TestAssertions.assertBigDecimalEquals;
import static com.banking.system.TestAssertions.assertEquals;
import static com.banking.system.TestAssertions.assertFalse;
import static com.banking.system.TestAssertions.assertNotNull;
import static com.banking.system.TestAssertions.assertThrows;
import static com.banking.system.TestAssertions.assertTrue;
import static com.banking.system.TestAssertions.first;
import static com.banking.system.TestAssertions.last;
import static java.lang.System.currentTimeMillis;

public class TransactionHistoryTest {

    public static void main(String[] args) {
        TestRunner.exit(run());
    }

    public static SuiteResult run() {
        TransactionHistoryTest tests = new TransactionHistoryTest();
        TestRunner runner = new TestRunner("Transaction history");

        runner.test("positiveInitialBalanceCreatesOpeningTransaction",
                tests::positiveInitialBalanceCreatesOpeningTransaction);
        runner.test("newAccountWithZeroBalanceHasEmptyTransactionHistory",
                tests::newAccountWithZeroBalanceHasEmptyTransactionHistory);
        runner.test("successfulDepositCreatesTransaction", tests::successfulDepositCreatesTransaction);
        runner.test("depositTransactionContainsCorrectInformation",
                tests::depositTransactionContainsCorrectInformation);
        runner.test("successfulWithdrawalCreatesTransaction", tests::successfulWithdrawalCreatesTransaction);
        runner.test("withdrawalTransactionContainsCorrectInformation",
                tests::withdrawalTransactionContainsCorrectInformation);
        runner.test("successfulTransferAddsEntryToEachAccountHistory",
                tests::successfulTransferAddsEntryToEachAccountHistory);
        runner.test("transferUsesCorrectDirections", tests::transferUsesCorrectDirections);
        runner.test("transferReferencesRelatedAccounts", tests::transferReferencesRelatedAccounts);
        runner.test("transferDoesNotCreateDuplicateDepositOrWithdrawalEntries",
                tests::transferDoesNotCreateDuplicateDepositOrWithdrawalEntries);
        runner.test("transactionHistoryMaintainsOperationOrder", tests::transactionHistoryMaintainsOperationOrder);
        runner.test("transactionHistoryIsSpecificToEachAccount", tests::transactionHistoryIsSpecificToEachAccount);
        runner.test("transactionContainsTimestamp", tests::transactionContainsTimestamp);
        runner.test("transactionIdsAreUnique", tests::transactionIdsAreUnique);
        runner.test("failedDepositDoesNotCreateTransaction", tests::failedDepositDoesNotCreateTransaction);
        runner.test("failedWithdrawalDoesNotCreateTransaction", tests::failedWithdrawalDoesNotCreateTransaction);
        runner.test("failedTransferDoesNotCreateTransactions", tests::failedTransferDoesNotCreateTransactions);
        runner.test("unknownAccountHistoryQueryIsRejected", tests::unknownAccountHistoryQueryIsRejected);
        runner.test("balancesAreUpdatedTogetherWithLedger", tests::balancesAreUpdatedTogetherWithLedger);
        runner.test("returnedHistoryCannotBeModifiedByCallers", tests::returnedHistoryCannotBeModifiedByCallers);
        runner.test("historyIsQueryableWithoutASecondLookupService",
                tests::historyIsQueryableWithoutASecondLookupService);
        runner.test("unknownTransactionIdsAreSkippedRatherThanReturnedAsNull",
                tests::unknownTransactionIdsAreSkippedRatherThanReturnedAsNull);

        return runner.result();
    }

    private void positiveInitialBalanceCreatesOpeningTransaction() {
        BankingService service = TestFixtures.service();
        service.createAccount("account-1", new BigDecimal("100.00"));
        List<Transaction> history = service.getTransactionHistory("account-1");
        assertEquals(1, history.size(),
                "A positive initial balance should create one opening transaction");
        Transaction transaction = first(history);
        assertEquals(TransactionType.DEPOSIT, transaction.getType(),
                "The opening transaction should be a DEPOSIT");
        assertEquals(TransactionDirection.CREDIT, transaction.getDirection(),
                "The opening transaction should be CREDIT");
        assertBigDecimalEquals(new BigDecimal("100.00"), transaction.getAmount(),
                "The opening transaction amount should match the initial balance");
        assertBigDecimalEquals(new BigDecimal("100.00"), transaction.getBalanceAfter(),
                "The opening transaction balance should be correct");
    }

    private void newAccountWithZeroBalanceHasEmptyTransactionHistory() {
        BankingService service = TestFixtures.serviceWithZeroBalanceAccount();
        assertEquals(0, service.getTransactionHistory("account-1").size(),
                "A new account should have no transactions");
    }

    private void successfulDepositCreatesTransaction() {
        BankingService service = TestFixtures.serviceWithOneAccount();
        service.deposit("account-1", new BigDecimal("25.00"));
        List<Transaction> history = service.getTransactionHistory("account-1");
        assertEquals(2, history.size(),
                "A successful deposit should add to the opening transaction");
        assertEquals(TransactionType.DEPOSIT, first(history).getType(),
                "The transaction type should be DEPOSIT");
    }

    private void depositTransactionContainsCorrectInformation() {
        BankingService service = TestFixtures.serviceWithOneAccount();
        service.deposit("account-1", new BigDecimal("25.00"));
        Transaction transaction = last(service.getTransactionHistory("account-1"));
        assertNotNull(transaction.getTransactionId(), "The deposit should have a transaction ID");
        assertEquals("account-1", transaction.getAccountId(),
                "The transaction should belong to account-1");
        assertEquals(TransactionType.DEPOSIT, transaction.getType(),
                "The transaction type should be DEPOSIT");
        assertEquals(TransactionDirection.CREDIT, transaction.getDirection(),
                "A deposit should have CREDIT direction");
        assertBigDecimalEquals(new BigDecimal("25.00"), transaction.getAmount(),
                "The transaction amount should match the deposit");
        assertBigDecimalEquals(new BigDecimal("125.00"), transaction.getBalanceAfter(),
                "The balance after the deposit should be recorded");
        assertEquals(null, transaction.getRelatedAccountId(),
                "A deposit should not have a related account");
        assertNotNull(transaction.getTimestamp(), "The deposit should have a timestamp");
    }

    private void successfulWithdrawalCreatesTransaction() {
        BankingService service = TestFixtures.serviceWithOneAccount();
        service.withdraw("account-1", new BigDecimal("25.00"));
        List<Transaction> history = service.getTransactionHistory("account-1");
        assertEquals(2, history.size(),
                "A successful withdrawal should add to the opening transaction");
        assertEquals(TransactionType.WITHDRAWAL, last(history).getType(),
                "The transaction type should be WITHDRAWAL");
    }

    private void withdrawalTransactionContainsCorrectInformation() {
        BankingService service = TestFixtures.serviceWithOneAccount();
        service.withdraw("account-1", new BigDecimal("25.00"));
        Transaction transaction = last(service.getTransactionHistory("account-1"));
        assertNotNull(transaction.getTransactionId(), "The withdrawal should have a transaction ID");
        assertEquals("account-1", transaction.getAccountId(),
                "The transaction should belong to account-1");
        assertEquals(TransactionType.WITHDRAWAL, transaction.getType(),
                "The transaction type should be WITHDRAWAL");
        assertEquals(TransactionDirection.DEBIT, transaction.getDirection(),
                "A withdrawal should have DEBIT direction");
        assertBigDecimalEquals(new BigDecimal("25.00"), transaction.getAmount(),
                "The transaction amount should match the withdrawal");
        assertBigDecimalEquals(new BigDecimal("75.00"), transaction.getBalanceAfter(),
                "The balance after the withdrawal should be recorded");
        assertEquals(null, transaction.getRelatedAccountId(),
                "A withdrawal should not have a related account");
        assertNotNull(transaction.getTimestamp(), "The withdrawal should have a timestamp");
    }

    private void successfulTransferAddsEntryToEachAccountHistory() {
        BankingService service = TestFixtures.serviceWithTwoAccounts();
        service.transfer("account-1", "account-2", new BigDecimal("25.00"));
        List<Transaction> sourceHistory = service.getTransactionHistory("account-1");
        List<Transaction> destinationHistory = service.getTransactionHistory("account-2");
        assertEquals(2, sourceHistory.size(),
                "The source account should contain its opening transaction and the transfer");
        assertEquals(2, destinationHistory.size(),
                "The destination account should contain its opening transaction and the transfer");
        assertEquals(TransactionType.TRANSFER, last(sourceHistory).getType(),
                "The last source transaction should be TRANSFER");
        assertEquals(TransactionType.TRANSFER, last(destinationHistory).getType(),
                "The last destination transaction should be TRANSFER");
    }

    private void transferUsesCorrectDirections() {
        BankingService service = TestFixtures.serviceWithTwoAccounts();
        service.transfer("account-1", "account-2", new BigDecimal("25.00"));
        assertEquals(TransactionDirection.DEBIT, last(service.getTransactionHistory("account-1")).getDirection(),
                "The source transfer should be DEBIT");
        assertEquals(TransactionDirection.CREDIT, last(service.getTransactionHistory("account-2")).getDirection(),
                "The destination transfer should be CREDIT");
    }

    private void transferReferencesRelatedAccounts() {
        BankingService service = TestFixtures.serviceWithTwoAccounts();
        service.transfer("account-1", "account-2", new BigDecimal("25.00"));
        assertEquals("account-2", last(service.getTransactionHistory("account-1")).getRelatedAccountId(),
                "The source entry should reference account-2");
        assertEquals("account-1", last(service.getTransactionHistory("account-2")).getRelatedAccountId(),
                "The destination entry should reference account-1");
    }

    private void transferDoesNotCreateDuplicateDepositOrWithdrawalEntries() {
        BankingService service = TestFixtures.serviceWithTwoAccounts();
        service.transfer("account-1", "account-2", new BigDecimal("25.00"));
        TransactionType sourceType = last(service.getTransactionHistory("account-1")).getType();
        TransactionType destinationType = last(service.getTransactionHistory("account-2")).getType();
        assertFalse(sourceType == TransactionType.DEPOSIT, "A transfer must not create a DEPOSIT entry");
        assertFalse(sourceType == TransactionType.WITHDRAWAL, "A transfer must not create a WITHDRAWAL entry");
        assertFalse(destinationType == TransactionType.DEPOSIT, "A transfer must not create a DEPOSIT entry");
        assertFalse(destinationType == TransactionType.WITHDRAWAL, "A transfer must not create a WITHDRAWAL entry");
    }

    private void transactionHistoryMaintainsOperationOrder() {
        BankingService service = TestFixtures.serviceWithOneAccount();
        service.deposit("account-1", new BigDecimal("25.00"));
        service.withdraw("account-1", new BigDecimal("10.00"));
        service.deposit("account-1", new BigDecimal("50.00"));
        List<Transaction> history = service.getTransactionHistory("account-1");
        assertEquals(4, history.size(), "All successful operations should appear in history");
        assertEquals(TransactionType.DEPOSIT, history.get(0).getType(),
                "The first transaction should be the opening deposit");
        assertEquals(TransactionType.DEPOSIT, history.get(1).getType(),
                "The second transaction should be the deposit");
        assertEquals(TransactionType.WITHDRAWAL, history.get(2).getType(),
                "The third transaction should be the withdrawal");
        assertEquals(TransactionType.DEPOSIT, history.get(3).getType(),
                "The fourth transaction should be the second deposit");
        assertBigDecimalEquals(new BigDecimal("100.00"), history.get(0).getBalanceAfter(),
                "The first balance-after value should be correct");
        assertBigDecimalEquals(new BigDecimal("125.00"), history.get(1).getBalanceAfter(),
                "The second balance-after value should be correct");
        assertBigDecimalEquals(new BigDecimal("115.00"), history.get(2).getBalanceAfter(),
                "The third balance-after value should be correct");
        assertBigDecimalEquals(new BigDecimal("165.00"), history.get(3).getBalanceAfter(),
                "The fourth balance-after value should be correct");
    }

    private void transactionHistoryIsSpecificToEachAccount() {
        BankingService service = TestFixtures.serviceWithTwoAccounts();
        service.deposit("account-1", new BigDecimal("25.00"));
        service.withdraw("account-2", new BigDecimal("10.00"));
        List<Transaction> accountOneHistory = service.getTransactionHistory("account-1");
        List<Transaction> accountTwoHistory = service.getTransactionHistory("account-2");
        assertEquals(2, accountOneHistory.size(), "Account 1 should contain only its own history");
        assertEquals(2, accountTwoHistory.size(), "Account 2 should contain only its own history");
        assertEquals(TransactionType.DEPOSIT, last(accountOneHistory).getType(),
                "Account 1 should contain its deposit");
        assertEquals(TransactionType.WITHDRAWAL, last(accountTwoHistory).getType(),
                "Account 2 should contain its withdrawal");
    }

    private void transactionContainsTimestamp() {
        BankingService service = TestFixtures.serviceWithOneAccount();
        long beforeOperation = currentTimeMillis();
        service.deposit("account-1", new BigDecimal("25.00"));
        long afterOperation = currentTimeMillis();
        Transaction transaction = last(service.getTransactionHistory("account-1"));
        assertNotNull(transaction.getTimestamp(), "The transaction timestamp must not be null");
        assertTrue(transaction.getTimestamp() >= beforeOperation && transaction.getTimestamp() <= afterOperation,
                "The timestamp should represent the operation time");
    }

    private void transactionIdsAreUnique() {
        BankingService service = TestFixtures.serviceWithTwoAccounts();
        service.deposit("account-1", new BigDecimal("10.00"));
        service.withdraw("account-1", new BigDecimal("5.00"));
        service.transfer("account-1", "account-2", new BigDecimal("20.00"));
        Set<String> transactionIds = new HashSet<>();
        for (Transaction transaction : service.getTransactionHistory("account-1")) {
            transactionIds.add(transaction.getTransactionId());
        }
        for (Transaction transaction : service.getTransactionHistory("account-2")) {
            transactionIds.add(transaction.getTransactionId());
        }
        assertEquals(6, transactionIds.size(),
                "Every ledger entry should have a unique transaction ID");
    }

    private void failedDepositDoesNotCreateTransaction() {
        BankingService service = TestFixtures.serviceWithOneAccount();
        assertThrows(InvalidAmountException.class,
                () -> service.deposit("account-1", new BigDecimal("-10.00")),
                "A negative deposit should be rejected");
        assertEquals(1, service.getTransactionHistory("account-1").size(),
                "A failed deposit must not create a transaction");
        assertBigDecimalEquals(new BigDecimal("100.00"), service.getBalance("account-1"),
                "A failed deposit must not change the balance");
    }

    private void failedWithdrawalDoesNotCreateTransaction() {
        BankingService service = TestFixtures.serviceWithOneAccount();
        assertThrows(InsufficientFundsException.class,
                () -> service.withdraw("account-1", new BigDecimal("150.00")),
                "An overdraft withdrawal should be rejected");
        assertEquals(1, service.getTransactionHistory("account-1").size(),
                "A failed withdrawal must not create a transaction");
        assertBigDecimalEquals(new BigDecimal("100.00"), service.getBalance("account-1"),
                "A failed withdrawal must not change the balance");
    }

    private void failedTransferDoesNotCreateTransactions() {
        BankingService service = TestFixtures.serviceWithTwoAccounts();
        assertThrows(InsufficientFundsException.class,
                () -> service.transfer("account-1", "account-2", new BigDecimal("150.00")),
                "An overdraft transfer should be rejected");
        assertEquals(1, service.getTransactionHistory("account-1").size(),
                "A failed transfer must not create a source entry");
        assertEquals(1, service.getTransactionHistory("account-2").size(),
                "A failed transfer must not create a destination entry");
        assertBigDecimalEquals(new BigDecimal("100.00"), service.getBalance("account-1"),
                "The source balance must remain unchanged");
        assertBigDecimalEquals(new BigDecimal("50.00"), service.getBalance("account-2"),
                "The destination balance must remain unchanged");
    }

    private void unknownAccountHistoryQueryIsRejected() {
        BankingService service = TestFixtures.service();
        assertThrows(AccountNotFoundException.class,
                () -> service.getTransactionHistory("unknown-account"),
                "History for an unknown account should be rejected");
    }

    private void balancesAreUpdatedTogetherWithLedger() {
        BankingService service = TestFixtures.serviceWithTwoAccounts();
        service.deposit("account-1", new BigDecimal("25.00"));
        service.withdraw("account-1", new BigDecimal("10.00"));
        service.transfer("account-1", "account-2", new BigDecimal("50.00"));
        assertBigDecimalEquals(new BigDecimal("65.00"), service.getBalance("account-1"),
                "The source balance should reflect all operations");
        assertBigDecimalEquals(new BigDecimal("100.00"), service.getBalance("account-2"),
                "The destination balance should reflect the transfer");
        List<Transaction> accountOneHistory = service.getTransactionHistory("account-1");
        List<Transaction> accountTwoHistory = service.getTransactionHistory("account-2");
        assertEquals(4, accountOneHistory.size(),
                "Account 1 should contain opening, deposit, withdrawal, and transfer");
        assertEquals(2, accountTwoHistory.size(),
                "Account 2 should contain opening and the incoming transfer");
        assertEquals(TransactionType.DEPOSIT, accountOneHistory.get(0).getType(),
                "The first entry should be the opening DEPOSIT");
        assertEquals(TransactionType.DEPOSIT, accountOneHistory.get(1).getType(),
                "The second entry should be DEPOSIT");
        assertEquals(TransactionType.WITHDRAWAL, accountOneHistory.get(2).getType(),
                "The third entry should be WITHDRAWAL");
        assertEquals(TransactionType.TRANSFER, accountOneHistory.get(3).getType(),
                "The fourth entry should be TRANSFER");
        assertEquals(TransactionType.TRANSFER, last(accountTwoHistory).getType(),
                "The destination should contain a TRANSFER entry");
        assertBigDecimalEquals(service.getBalance("account-1"), last(accountOneHistory).getBalanceAfter(),
                "The last ledger entry must agree with the account balance");
    }

    private void returnedHistoryCannotBeModifiedByCallers() {
        BankingService service = TestFixtures.serviceWithOneAccount();
        List<Transaction> history = service.getTransactionHistory("account-1");
        assertThrows(UnsupportedOperationException.class, () -> history.add(null),
                "The returned history must not be modifiable by callers");
        assertEquals(1, service.getTransactionHistory("account-1").size(),
                "A caller must not be able to grow the stored ledger");
    }

    private void historyIsQueryableWithoutASecondLookupService() {
        BankingService service = TestFixtures.serviceWithOneAccount();
        service.deposit("account-1", new BigDecimal("25.00"));
        List<Transaction> history = service.getTransactionHistory("account-1");
        assertEquals(2, history.size(), "History should be returned as fully resolved transactions");
        history.forEach(transaction -> {
            assertNotNull(transaction.getType(), "Each entry should carry its type");
            assertNotNull(transaction.getAmount(), "Each entry should carry its amount");
            assertNotNull(transaction.getTimestamp(), "Each entry should carry its timestamp");
        });
    }

    private void unknownTransactionIdsAreSkippedRatherThanReturnedAsNull() {
        InMemoryTransactionDatabase database = new InMemoryTransactionDatabase();
        List<Transaction> found = database.findByTransactionIds(List.of("missing-1", "missing-2"));
        assertEquals(0, found.size(),
                "Unknown transaction ids must be skipped, never returned as null entries");
    }
}
