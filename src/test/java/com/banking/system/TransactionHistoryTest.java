package com.banking.system;

import com.banking.system.database.InMemoryAccountDatabase;
import com.banking.system.database.InMemoryTransactionDatabase;
import com.banking.system.dtos.Transaction;
import com.banking.system.enums.TransactionDirection;
import com.banking.system.enums.TransactionType;
import com.banking.system.exceptions.AccountNotFoundException;
import com.banking.system.exceptions.InsufficientFundsException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.lang.System.currentTimeMillis;

public class TransactionHistoryTest {

    public static void main(String[] args) {
        TransactionHistoryTest tests = new TransactionHistoryTest();

        tests.runTest("newAccountWithZeroBalanceHasEmptyTransactionHistory", tests::newAccountWithZeroBalanceHasEmptyTransactionHistory);

        tests.runTest("successfulDepositCreatesTransaction", tests::successfulDepositCreatesTransaction);

        tests.runTest("depositTransactionContainsCorrectInformation", tests::depositTransactionContainsCorrectInformation);

        tests.runTest("successfulWithdrawalCreatesTransaction", tests::successfulWithdrawalCreatesTransaction);

        tests.runTest("withdrawalTransactionContainsCorrectInformation", tests::withdrawalTransactionContainsCorrectInformation);

        tests.runTest("successfulTransferCreatesTwoEntryForEachAccount", tests::successfulTransferCreatesTwoEntryForEachAccount);

        tests.runTest("transferCreatesOnlyTransferEntries", tests::transferCreatesOnlyTransferEntries);

        tests.runTest("transferUsesCorrectDirections", tests::transferUsesCorrectDirections);

        tests.runTest("transferReferencesRelatedAccounts", tests::transferReferencesRelatedAccounts);

        tests.runTest("transferDoesNotCreateDuplicateDepositOrWithdrawalEntries", tests::transferDoesNotCreateDuplicateDepositOrWithdrawalEntries);

        tests.runTest("transactionHistoryMaintainsOperationOrder", tests::transactionHistoryMaintainsOperationOrder);

        tests.runTest("transactionHistoryIsSpecificToEachAccount", tests::transactionHistoryIsSpecificToEachAccount);

        tests.runTest("transactionContainsTimestamp", tests::transactionContainsTimestamp);

        tests.runTest("transactionIdsAreUnique", tests::transactionIdsAreUnique);

        tests.runTest("failedDepositDoesNotCreateTransaction", tests::failedDepositDoesNotCreateTransaction);

        tests.runTest("failedWithdrawalDoesNotCreateTransaction", tests::failedWithdrawalDoesNotCreateTransaction);

        tests.runTest("failedTransferDoesNotCreateTransactions", tests::failedTransferDoesNotCreateTransactions);

        tests.runTest("unknownAccountHistoryQueryIsRejected", tests::unknownAccountHistoryQueryIsRejected);

        tests.runTest("balancesAreUpdatedTogetherWithLedger", tests::balancesAreUpdatedTogetherWithLedger);

        System.out.println();
        System.out.println("All transaction flow tests passed.");
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

    private TestContext createTestContext() {
        InMemoryAccountDatabase accountDatabase = new InMemoryAccountDatabase();
        InMemoryTransactionDatabase transactionDatabase = new InMemoryTransactionDatabase();
        AccountRepository accountRepository = new AccountRepositoryImpl(accountDatabase);
        TransactionRepository transactionRepository = new TransactionRepositoryImpl(transactionDatabase);
        BankingServiceValidator validator = new BankingServiceValidator(accountRepository);
        TransactionService transactionService = new TransactionServiceImpl(transactionRepository);
        BankingService bankingService = new BankingServiceImpl(accountRepository, validator, transactionService);
        return new TestContext(bankingService, transactionService);
    }

    private TestContext createContextWithZeroBalance() {
        TestContext context = createTestContext();
        context.service.createAccount("account-1", new BigDecimal("0"));
        return context;
    }

    private TestContext createContextWithOneAccount() {
        TestContext context = createTestContext();
        context.service.createAccount("account-1", new BigDecimal("100.00"));
        return context;
    }

    private TestContext createContextWithTwoAccounts() {
        TestContext context = createTestContext();
        context.service.createAccount("account-1", new BigDecimal("100.00"));
        context.service.createAccount("account-2", new BigDecimal("50.00"));
        return context;
    }

    private void newAccountWithZeroBalanceHasEmptyTransactionHistory() {
        TestContext context = createContextWithZeroBalance();
        List<String> history = context.service.getTransactionHistory("account-1");
        TestAssertions.assertEquals(0, history.size(), "A new account should have no transactions");
    }

    private void successfulDepositCreatesTransaction() {
        TestContext context = createContextWithOneAccount();
        context.service.deposit("account-1", new BigDecimal("25.00"));
        List<String> transactionIds = context.service.getTransactionHistory("account-1");
        TestAssertions.assertEquals(2, transactionIds.size(),
                "A successful deposit should create two transaction");
        List<Transaction> transactions = context.transactionService.findByTransactionIds(transactionIds);
        TestAssertions.assertEquals(TransactionType.DEPOSIT.name(), transactions.getFirst().getType(),
                "The transaction type should be DEPOSIT"
        );
    }

    private void depositTransactionContainsCorrectInformation() {
        TestContext context = createContextWithOneAccount();
        context.service.deposit("account-1", new BigDecimal("25.00"));
        String transactionId = context.service.getTransactionHistory("account-1").getLast();
        Transaction transaction = context.transactionService.findByTransactionIds(List.of(transactionId)).getFirst();
        TestAssertions.assertNotNull(transaction.getTransactionId(),
                "The deposit should have a transaction ID");
        TestAssertions.assertEquals("account-1", transaction.getAccountId(),
                "The transaction should belong to account-1");
        TestAssertions.assertEquals(TransactionType.DEPOSIT.name(), transaction.getType(),
                "The transaction type should be DEPOSIT");
        TestAssertions.assertEquals(TransactionDirection.CREDIT.name(), transaction.getDirection(),
                "A deposit should have CREDIT direction");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("25.00"), transaction.getAmount(),
                "The transaction amount should match the deposit");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("125.00"), transaction.getBalanceAfter(),
                "The balance after the deposit should be recorded");
        TestAssertions.assertEquals(null, transaction.getRelatedAccountId(),
                "A deposit should not have a related account");
        TestAssertions.assertNotNull(transaction.getTimestamp(), "The deposit should have a timestamp");
    }

    private void successfulWithdrawalCreatesTransaction() {
        TestContext context = createContextWithOneAccount();
        context.service.withdraw("account-1", new BigDecimal("25.00"));
        List<String> transactionIds = context.service.getTransactionHistory("account-1");
        TestAssertions.assertEquals(2, transactionIds.size(),
                "A successful withdrawal should create two transaction");
        List<Transaction> transactions = context.transactionService.findByTransactionIds(transactionIds);
        TestAssertions.assertEquals(TransactionType.WITHDRAWAL.name(), transactions.getLast().getType(),
                "The transaction type should be WITHDRAWAL");
    }

    private void withdrawalTransactionContainsCorrectInformation() {
        TestContext context = createContextWithOneAccount();
        context.service.withdraw("account-1", new BigDecimal("25.00"));
        String transactionId = context.service.getTransactionHistory("account-1").getLast();
        Transaction transaction = context.transactionService.findByTransactionIds(List.of(transactionId)).getFirst();
        TestAssertions.assertNotNull(transaction.getTransactionId(),
                "The withdrawal should have a transaction ID");
        TestAssertions.assertEquals("account-1", transaction.getAccountId(),
                "The transaction should belong to account-1");
        TestAssertions.assertEquals(TransactionType.WITHDRAWAL.name(), transaction.getType(),
                "The transaction type should be WITHDRAWAL");
        TestAssertions.assertEquals(TransactionDirection.DEBIT.name(), transaction.getDirection(),
                "A withdrawal should have DEBIT direction");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("25.00"), transaction.getAmount(),
                "The transaction amount should match the withdrawal");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("75.00"), transaction.getBalanceAfter(),
                "The balance after the withdrawal should be recorded");
        TestAssertions.assertEquals(null, transaction.getRelatedAccountId(),
                "A withdrawal should not have a related account");
        TestAssertions.assertNotNull(transaction.getTimestamp(), "The withdrawal should have a timestamp");
    }

    private void successfulTransferCreatesTwoEntryForEachAccount() {
        TestContext context = createContextWithTwoAccounts();
        context.service.transfer("account-1", "account-2", new BigDecimal("25.00"));
        List<String> sourceTransactionIds = context.service.getTransactionHistory("account-1");
        List<String> destinationTransactionIds = context.service.getTransactionHistory("account-2");
        List<Transaction> sourceHistory = context.transactionService.findByTransactionIds(sourceTransactionIds);
        List<Transaction> destinationHistory =
                context.transactionService.findByTransactionIds(destinationTransactionIds);
        TestAssertions.assertEquals(2, sourceHistory.size(),
                "The source account should have two transaction");
        TestAssertions.assertEquals(2, destinationHistory.size(),
                "The destination account should have two transaction");
        TestAssertions.assertEquals(TransactionType.TRANSFER.name(), sourceHistory.getLast().getType(),
                "The source transaction should be TRANSFER");
        TestAssertions.assertEquals(TransactionType.TRANSFER.name(), destinationHistory.getLast().getType(),
                "The destination transaction should be TRANSFER");
    }

    private void transferCreatesOnlyTransferEntries() {
        TestContext context = createContextWithTwoAccounts();
        context.service.transfer("account-1", "account-2", new BigDecimal("25.00"));
        List<String> sourceTransactionIds = context.service.getTransactionHistory("account-1");
        List<Transaction> sourceHistory = context.transactionService.findByTransactionIds(sourceTransactionIds);
        List<String> destinationTransactionIds = context.service.getTransactionHistory("account-2");
        List<Transaction> destinationHistory =
                context.transactionService.findByTransactionIds(destinationTransactionIds);
        TestAssertions.assertEquals(2, sourceHistory.size(),
                "The source should contain exactly one entry");
        TestAssertions.assertEquals(2, destinationHistory.size(),
                "The destination should contain exactly one entry");
        TestAssertions.assertEquals(TransactionType.TRANSFER.name(), sourceHistory.getLast().getType(),
                "The source entry should be TRANSFER");
        TestAssertions.assertEquals(TransactionType.TRANSFER.name(), destinationHistory.getLast().getType(),
                "The destination entry should be TRANSFER");
    }

    private void transferUsesCorrectDirections() {
        TestContext context = createContextWithTwoAccounts();
        context.service.transfer("account-1", "account-2", new BigDecimal("25.00"));
        String transactionId = context.service.getTransactionHistory("account-1").getLast();
        Transaction sourceTransaction =
                context.transactionService.findByTransactionIds(List.of(transactionId)).getFirst();
        String destinationTransactionId = context.service.getTransactionHistory("account-2").getFirst();
        Transaction destinationTransaction =
                context.transactionService.findByTransactionIds(List.of(destinationTransactionId)).getFirst();
        TestAssertions.assertEquals(TransactionDirection.DEBIT.name(), sourceTransaction.getDirection(),
                "The source transfer should be DEBIT");
        TestAssertions.assertEquals(TransactionDirection.CREDIT.name(), destinationTransaction.getDirection(),
                "The destination transfer should be CREDIT");
    }

    private void transferReferencesRelatedAccounts() {
        TestContext context = createContextWithTwoAccounts();
        context.service.transfer("account-1", "account-2", new BigDecimal("25.00"));
        String sourceTransactionId = context.service.getTransactionHistory("account-1").getLast();
        Transaction sourceTransaction =
                context.transactionService.findByTransactionIds(List.of(sourceTransactionId)).getFirst();
        String destinationTransactionId = context.service.getTransactionHistory("account-2").getLast();
        Transaction destinationTransaction =
                context.transactionService.findByTransactionIds(List.of(destinationTransactionId)).getFirst();
        TestAssertions.assertEquals("account-2", sourceTransaction.getRelatedAccountId(),
                "The source entry should reference account-2");
        TestAssertions.assertEquals("account-1", destinationTransaction.getRelatedAccountId(),
                "The destination entry should reference account-1");
    }

    private void transferDoesNotCreateDuplicateDepositOrWithdrawalEntries() {
        TestContext context = createContextWithTwoAccounts();
        context.service.transfer("account-1", "account-2", new BigDecimal("25.00"));
        List<String> sourceTransactionIds = context.service.getTransactionHistory("account-1");
        List<Transaction> sourceHistory = context.transactionService.findByTransactionIds(sourceTransactionIds);
        List<String> destinationTransactionIds = context.service.getTransactionHistory("account-2");
        List<Transaction> destinationHistory =
                context.transactionService.findByTransactionIds(destinationTransactionIds);
        TestAssertions.assertFalse(Objects.equals(sourceHistory.getLast().getType(), TransactionType.DEPOSIT.name()),
                "A transfer must not create a DEPOSIT entry");
        TestAssertions.assertFalse(Objects.equals(sourceHistory.getLast().getType(), TransactionType.WITHDRAWAL.name()),
                "A transfer must not create a WITHDRAWAL entry");
        TestAssertions.assertFalse(Objects.equals(destinationHistory.getLast().getType(), TransactionType.DEPOSIT.name()),
                "A transfer must not create a DEPOSIT entry");
        TestAssertions.assertFalse(Objects.equals(destinationHistory.getLast().getType(), TransactionType.WITHDRAWAL.name()),
                "A transfer must not create a WITHDRAWAL entry");
    }

    private void transactionHistoryMaintainsOperationOrder() {
        TestContext context = createContextWithOneAccount();
        context.service.deposit("account-1", new BigDecimal("25.00"));
        context.service.withdraw("account-1", new BigDecimal("10.00"));
        context.service.deposit("account-1", new BigDecimal("50.00"));
        List<String> sourceTransactionIds = context.service.getTransactionHistory("account-1");
        List<Transaction> history = context.transactionService.findByTransactionIds(sourceTransactionIds);
        TestAssertions.assertEquals(4, history.size(),
                "All successful operations should appear in history");
        TestAssertions.assertEquals(TransactionType.DEPOSIT.name(), history.get(0).getType(),
                "The first transaction should be the first deposit");
        TestAssertions.assertEquals(TransactionType.DEPOSIT.name(), history.get(1).getType(),
                "The second transaction should be the second deposit");
        TestAssertions.assertEquals(TransactionType.WITHDRAWAL.name(), history.get(2).getType(),
                "The third transaction should be the withdrawal");
        TestAssertions.assertEquals(TransactionType.DEPOSIT.name(), history.get(3).getType(),
                "The third transaction should be the third deposit");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("100.00"), history.get(0).getBalanceAfter(),
                "The first balance-after value should be correct");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("125.00"), history.get(1).getBalanceAfter(),
                "The second balance-after value should be correct");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("115.00"), history.get(2).getBalanceAfter(),
                "The third balance-after value should be correct");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("165.00"), history.get(3).getBalanceAfter(),
                "The fourth balance-after value should be correct");
    }

    private void transactionHistoryIsSpecificToEachAccount() {
        TestContext context = createContextWithTwoAccounts();
        context.service.deposit("account-1", new BigDecimal("25.00"));
        context.service.withdraw("account-2", new BigDecimal("10.00"));
        List<String> sourceTransactionIds = context.service.getTransactionHistory("account-1");
        List<Transaction> accountOneHistory = context.transactionService.findByTransactionIds(sourceTransactionIds);
        List<String> destinationTransactionIds = context.service.getTransactionHistory("account-2");
        List<Transaction> accountTwoHistory =
                context.transactionService.findByTransactionIds(destinationTransactionIds);
        TestAssertions.assertEquals(2, accountOneHistory.size(),
                "Account 1 should contain only its own history");
        TestAssertions.assertEquals(2, accountTwoHistory.size(),
                "Account 2 should contain only its own history");
        TestAssertions.assertEquals(TransactionType.DEPOSIT.name(), accountOneHistory.getLast().getType(),
                "Account 1 should contain its deposit");
        TestAssertions.assertEquals(TransactionType.WITHDRAWAL.name(), accountTwoHistory.getLast().getType(),
                "Account 2 should contain its withdrawal"
        );
    }

    private void transactionContainsTimestamp() {
        TestContext context = createContextWithOneAccount();
        long beforeOperation = currentTimeMillis();
        context.service.deposit("account-1", new BigDecimal("25.00"));
        long afterOperation = currentTimeMillis();
        String transactionId = context.service.getTransactionHistory("account-1").getFirst();
        Transaction transaction = context.transactionService.findByTransactionIds(List.of(transactionId)).getFirst();
        TestAssertions.assertNotNull(transaction.getTimestamp(), "The transaction timestamp must not be null");
        if (transaction.getTimestamp() < beforeOperation || transaction.getTimestamp() > afterOperation) {
            throw new AssertionError("The timestamp should represent the operation time");
        }
    }

    private void transactionIdsAreUnique() {
        TestContext context = createContextWithTwoAccounts();
        context.service.deposit("account-1", new BigDecimal("10.00"));
        context.service.withdraw("account-1", new BigDecimal("5.00"));
        context.service.transfer("account-1", "account-2", new BigDecimal("20.00"));
        List<String> accountOneTransactionIds = context.service.getTransactionHistory("account-1");
        List<Transaction> accountOneHistory = context.transactionService.findByTransactionIds(accountOneTransactionIds);
        List<String> accountTwoTransactionIds = context.service.getTransactionHistory("account-2");
        List<Transaction> accountTwoHistory = context.transactionService.findByTransactionIds(accountTwoTransactionIds);
        Set<String> transactionIds = new HashSet<>();
        for (Transaction transaction : accountOneHistory) {
            transactionIds.add(transaction.getTransactionId());
        }
        for (Transaction transaction : accountTwoHistory) {
            transactionIds.add(transaction.getTransactionId());
        }
        TestAssertions.assertEquals(6, transactionIds.size(),
                "Every ledger entry should have a unique transaction ID");
    }

    private void failedDepositDoesNotCreateTransaction() {
        TestContext context = createContextWithOneAccount();
        TestAssertions.assertThrows(InvalidAmountException.class, () ->
                        context.service.deposit("account-1", new BigDecimal("-10.00")),
                "A negative deposit should be rejected");
        List<String> history = context.service.getTransactionHistory("account-1");
        TestAssertions.assertEquals(1, history.size(),
                "A failed deposit must not create a transaction");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("100.00"),
                context.service.getBalance("account-1"),
                "A failed deposit must not change the balance");
    }

    private void failedWithdrawalDoesNotCreateTransaction() {
        TestContext context = createContextWithOneAccount();
        TestAssertions.assertThrows(InsufficientFundsException.class, () ->
                        context.service.withdraw("account-1", new BigDecimal("150.00")),
                "An overdraft withdrawal should be rejected");
        List<String> history = context.service.getTransactionHistory("account-1");
        TestAssertions.assertEquals(1, history.size(),
                "A failed withdrawal must not create a transaction");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("100.00"), context.service.getBalance("account-1"),
                "A failed withdrawal must not change the balance");
    }

    private void failedTransferDoesNotCreateTransactions() {
        TestContext context = createContextWithTwoAccounts();
        TestAssertions.assertThrows(InsufficientFundsException.class, () ->
                        context.service.transfer("account-1", "account-2", new BigDecimal("150.00")),
                "An overdraft transfer should be rejected");
        List<String> sourceHistory = context.service.getTransactionHistory("account-1");
        List<String> destinationHistory = context.service.getTransactionHistory("account-2");
        TestAssertions.assertEquals(1, sourceHistory.size(),
                "A failed transfer must not create a source entry");
        TestAssertions.assertEquals(1, destinationHistory.size(),
                "A failed transfer must not create a destination entry");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("100.00"),
                context.service.getBalance("account-1"), "The source balance must remain unchanged");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("50.00"),
                context.service.getBalance("account-2"),
                "The destination balance must remain unchanged");
    }

    private void unknownAccountHistoryQueryIsRejected() {
        TestContext context = createTestContext();
        TestAssertions.assertThrows(AccountNotFoundException.class, () ->
                        context.service.getTransactionHistory("unknown-account"),
                "History for an unknown account should be rejected");
    }

    private void balancesAreUpdatedTogetherWithLedger() {
        TestContext context = createContextWithTwoAccounts();
        context.service.deposit("account-1", new BigDecimal("25.00"));
        context.service.withdraw("account-1", new BigDecimal("10.00"));
        context.service.transfer("account-1", "account-2", new BigDecimal("50.00"));
        TestAssertions.assertBigDecimalEquals(new BigDecimal("65.00"),
                context.service.getBalance("account-1"),
                "The source balance should reflect all operations");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("100.00"),
                context.service.getBalance("account-2"),
                "The destination balance should reflect the transfer");
        List<String> accountOneTransactionIds = context.service.getTransactionHistory("account-1");
        List<Transaction> accountOneHistory = context.transactionService.findByTransactionIds(accountOneTransactionIds);
        List<String> accountTwoTransactionIds = context.service.getTransactionHistory("account-2");
        List<Transaction> accountTwoHistory = context.transactionService.findByTransactionIds(accountTwoTransactionIds);
        TestAssertions.assertEquals(4, accountOneHistory.size(),
                "Account 1 should contain deposit, withdrawal, and transfer");
        TestAssertions.assertEquals(2, accountTwoHistory.size(),
                "Account 2 should contain the incoming transfer");
        TestAssertions.assertEquals(TransactionType.DEPOSIT.name(), accountOneHistory.get(0).getType(),
                "The first entry should be DEPOSIT");
        TestAssertions.assertEquals(TransactionType.DEPOSIT.name(), accountOneHistory.get(1).getType(),
                "The second entry should be DEPOSIT");
        TestAssertions.assertEquals(TransactionType.WITHDRAWAL.name(), accountOneHistory.get(2).getType(),
                "The third entry should be WITHDRAWAL");
        TestAssertions.assertEquals(TransactionType.TRANSFER.name(), accountOneHistory.get(3).getType(),
                "The fourth entry should be TRANSFER");
        TestAssertions.assertEquals(TransactionType.TRANSFER.name(), accountTwoHistory.getLast().getType(),
                "The destination should contain a TRANSFER entry");
    }

    private static final class TestContext {
        private final BankingService service;
        private final TransactionService transactionService;

        private TestContext(BankingService service, TransactionService transactionService) {
            this.service = service;
            this.transactionService = transactionService;
        }
    }
}