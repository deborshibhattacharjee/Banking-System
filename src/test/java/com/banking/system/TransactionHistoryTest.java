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
import static com.banking.system.TestAssertions.first;
import static com.banking.system.TestAssertions.last;

public class TransactionHistoryTest {

    public static void main(String[] args) {
        TestRunner.exit(run());
    }

    public static SuiteResult run() {
        TransactionHistoryTest tests = new TransactionHistoryTest();
        TestRunner runner = new TestRunner("Transaction history");

        runner.test("positiveInitialBalanceCreatesOpeningTransaction", tests::positiveInitialBalanceCreatesOpeningTransaction);

        runner.test("newAccountWithZeroBalanceHasEmptyTransactionHistory", tests::newAccountWithZeroBalanceHasEmptyTransactionHistory);

        runner.test("successfulDepositCreatesTransaction", tests::successfulDepositCreatesTransaction);

        runner.test("depositTransactionContainsCorrectInformation", tests::depositTransactionContainsCorrectInformation);

        runner.test("successfulWithdrawalCreatesTransaction", tests::successfulWithdrawalCreatesTransaction);

        runner.test("withdrawalTransactionContainsCorrectInformation", tests::withdrawalTransactionContainsCorrectInformation);

        runner.test("successfulTransferAddsEntryToEachAccountHistory", tests::successfulTransferAddsEntryToEachAccountHistory);

        runner.test("transferAddsTransferEntryWithoutDuplicateOperationEntries", tests::transferAddsTransferEntryWithoutDuplicateOperationEntries);

        runner.test("transferUsesCorrectDirections", tests::transferUsesCorrectDirections);

        runner.test("transferReferencesRelatedAccounts", tests::transferReferencesRelatedAccounts);

        runner.test("transferDoesNotCreateDuplicateDepositOrWithdrawalEntries", tests::transferDoesNotCreateDuplicateDepositOrWithdrawalEntries);

        runner.test("transactionHistoryMaintainsOperationOrder", tests::transactionHistoryMaintainsOperationOrder);

        runner.test("transactionHistoryIsSpecificToEachAccount", tests::transactionHistoryIsSpecificToEachAccount);

        runner.test("transactionContainsTimestamp", tests::transactionContainsTimestamp);

        runner.test("transactionIdsAreUnique", tests::transactionIdsAreUnique);

        runner.test("failedDepositDoesNotCreateTransaction", tests::failedDepositDoesNotCreateTransaction);

        runner.test("failedWithdrawalDoesNotCreateTransaction", tests::failedWithdrawalDoesNotCreateTransaction);

        runner.test("failedTransferDoesNotCreateTransactions", tests::failedTransferDoesNotCreateTransactions);

        runner.test("unknownAccountHistoryQueryIsRejected", tests::unknownAccountHistoryQueryIsRejected);

        runner.test("balancesAreUpdatedTogetherWithLedger", tests::balancesAreUpdatedTogetherWithLedger);

        return runner.result();
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

    private void positiveInitialBalanceCreatesOpeningTransaction() {
        TestContext context = createTestContext();
        context.service.createAccount("account-1", new BigDecimal("100.00"));
        List<String> ids = context.service.getTransactionHistory("account-1");
        TestAssertions.assertEquals(1, ids.size(),
                "A positive initial balance should create one opening transaction");
        Transaction transaction = first(context.transactionService.findByTransactionIds(ids));
        TestAssertions.assertEquals(TransactionType.DEPOSIT.name(), transaction.getType(),
                "The opening transaction should be a DEPOSIT");
        TestAssertions.assertEquals(TransactionDirection.CREDIT.name(), transaction.getDirection(),
                "The opening transaction should be CREDIT");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("100.00"), transaction.getAmount(),
                "The opening transaction amount should match the initial balance");
        TestAssertions.assertBigDecimalEquals(new BigDecimal("100.00"), transaction.getBalanceAfter(),
                "The opening transaction balance should be correct");
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
        TestAssertions.assertEquals(TransactionType.DEPOSIT.name(), first(transactions).getType(),
                "The transaction type should be DEPOSIT"
        );
    }

    private void depositTransactionContainsCorrectInformation() {
        TestContext context = createContextWithOneAccount();
        context.service.deposit("account-1", new BigDecimal("25.00"));
        String transactionId = last(context.service.getTransactionHistory("account-1"));
        Transaction transaction = first(context.transactionService.findByTransactionIds(List.of(transactionId)));
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
        TestAssertions.assertEquals(TransactionType.WITHDRAWAL.name(), last(transactions).getType(),
                "The transaction type should be WITHDRAWAL");
    }

    private void withdrawalTransactionContainsCorrectInformation() {
        TestContext context = createContextWithOneAccount();
        context.service.withdraw("account-1", new BigDecimal("25.00"));
        String transactionId = last(context.service.getTransactionHistory("account-1"));
        Transaction transaction = first(context.transactionService.findByTransactionIds(List.of(transactionId)));
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

    private void successfulTransferAddsEntryToEachAccountHistory() {
        TestContext context = createContextWithTwoAccounts();
        context.service.transfer("account-1", "account-2", new BigDecimal("25.00"));
        List<String> sourceTransactionIds = context.service.getTransactionHistory("account-1");
        List<String> destinationTransactionIds = context.service.getTransactionHistory("account-2");
        List<Transaction> sourceHistory = context.transactionService.findByTransactionIds(sourceTransactionIds);
        List<Transaction> destinationHistory =
                context.transactionService.findByTransactionIds(destinationTransactionIds);
        TestAssertions.assertEquals(2, sourceHistory.size(),
                "The source account should contain its opening transaction and transfer");
        TestAssertions.assertEquals(2, destinationHistory.size(),
                "TThe destination account should contain its opening transaction and transfer");
        TestAssertions.assertEquals(TransactionType.TRANSFER.name(), last(sourceHistory).getType(),
                "The last source transaction should be TRANSFER");
        TestAssertions.assertEquals(TransactionType.TRANSFER.name(), last(destinationHistory).getType(),
                "The last destination transaction should be TRANSFER");
    }

    private void transferAddsTransferEntryWithoutDuplicateOperationEntries() {
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
        TestAssertions.assertEquals(TransactionType.TRANSFER.name(), last(sourceHistory).getType(),
                "The source's new entry should be TRANSFER");
        TestAssertions.assertEquals(TransactionType.TRANSFER.name(), last(destinationHistory).getType(),
                "The destination's new entry should be TRANSFER");
    }

    private void transferUsesCorrectDirections() {
        TestContext context = createContextWithTwoAccounts();
        context.service.transfer("account-1", "account-2", new BigDecimal("25.00"));
        String transactionId = last(context.service.getTransactionHistory("account-1"));
        Transaction sourceTransaction =
                first(context.transactionService.findByTransactionIds(List.of(transactionId)));
        String destinationTransactionId = last(context.service.getTransactionHistory("account-2"));
        Transaction destinationTransaction =
                first(context.transactionService.findByTransactionIds(List.of(destinationTransactionId)));
        TestAssertions.assertEquals(TransactionDirection.DEBIT.name(), sourceTransaction.getDirection(),
                "The source transfer should be DEBIT");
        TestAssertions.assertEquals(TransactionDirection.CREDIT.name(), destinationTransaction.getDirection(),
                "The destination transfer should be CREDIT");
    }

    private void transferReferencesRelatedAccounts() {
        TestContext context = createContextWithTwoAccounts();
        context.service.transfer("account-1", "account-2", new BigDecimal("25.00"));
        String sourceTransactionId = last(context.service.getTransactionHistory("account-1"));
        Transaction sourceTransaction =
                first(context.transactionService.findByTransactionIds(List.of(sourceTransactionId)));
        String destinationTransactionId = last(context.service.getTransactionHistory("account-2"));
        Transaction destinationTransaction =
                first(context.transactionService.findByTransactionIds(List.of(destinationTransactionId)));
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
        TestAssertions.assertFalse(Objects.equals(last(sourceHistory).getType(), TransactionType.DEPOSIT.name()),
                "A transfer must not create a DEPOSIT entry");
        TestAssertions.assertFalse(Objects.equals(last(sourceHistory).getType(), TransactionType.WITHDRAWAL.name()),
                "A transfer must not create a WITHDRAWAL entry");
        TestAssertions.assertFalse(Objects.equals(last(destinationHistory).getType(), TransactionType.DEPOSIT.name()),
                "A transfer must not create a DEPOSIT entry");
        TestAssertions.assertFalse(Objects.equals(last(destinationHistory).getType(), TransactionType.WITHDRAWAL.name()),
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
        TestAssertions.assertEquals(TransactionType.DEPOSIT.name(), last(accountOneHistory).getType(),
                "Account 1 should contain its deposit");
        TestAssertions.assertEquals(TransactionType.WITHDRAWAL.name(), last(accountTwoHistory).getType(),
                "Account 2 should contain its withdrawal"
        );
    }

    private void transactionContainsTimestamp() {
        TestContext context = createContextWithOneAccount();
        long beforeOperation = currentTimeMillis();
        context.service.deposit("account-1", new BigDecimal("25.00"));
        long afterOperation = currentTimeMillis();
        String transactionId = last(context.service.getTransactionHistory("account-1"));
        Transaction transaction = first(context.transactionService.findByTransactionIds(List.of(transactionId)));
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
        TestAssertions.assertEquals(TransactionType.TRANSFER.name(), last(accountTwoHistory).getType(),
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