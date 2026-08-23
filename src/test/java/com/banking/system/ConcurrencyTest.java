package com.banking.system;

import com.banking.system.dtos.Transaction;
import com.banking.system.enums.TransactionDirection;
import com.banking.system.exceptions.DuplicateAccountException;
import com.banking.system.service.BankingService;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static com.banking.system.TestAssertions.assertBigDecimalEquals;
import static com.banking.system.TestAssertions.assertEquals;
import static com.banking.system.TestAssertions.assertTrue;
import static com.banking.system.TestAssertions.last;

public class ConcurrencyTest {

    private static final int THREAD_COUNT = 16;
    private static final int COMPLETION_TIMEOUT_SECONDS = 30;

    public static void main(String[] args) {
        TestRunner.exit(run());
    }

    public static SuiteResult run() {
        ConcurrencyTest tests = new ConcurrencyTest();
        TestRunner runner = new TestRunner("Concurrency");

        runner.test("concurrentWithdrawalsNeverOverdrawTheAccount",
                tests::concurrentWithdrawalsNeverOverdrawTheAccount);
        runner.test("concurrentDepositsAreAllApplied", tests::concurrentDepositsAreAllApplied);
        runner.test("concurrentTransfersConserveTotalMoney", tests::concurrentTransfersConserveTotalMoney);
        runner.test("opposingTransfersDoNotDeadlock", tests::opposingTransfersDoNotDeadlock);
        runner.test("concurrentAccountCreationAllowsExactlyOneWinner",
                tests::concurrentAccountCreationAllowsExactlyOneWinner);
        runner.test("ledgerAgreesWithBalanceUnderConcurrency", tests::ledgerAgreesWithBalanceUnderConcurrency);

        return runner.result();
    }

    private void concurrentWithdrawalsNeverOverdrawTheAccount() {
        BankingService service = TestFixtures.service();
        service.createAccount("account-1", new BigDecimal("1000"));

        int successes = runConcurrently(2000, index -> service.withdraw("account-1", BigDecimal.ONE));

        assertEquals(1000, successes,
                "Exactly as many withdrawals as the balance allows should succeed");
        assertBigDecimalEquals(BigDecimal.ZERO, service.getBalance("account-1"),
                "The balance must never be overdrawn");
        assertEquals(1001, service.getTransactionHistory("account-1").size(),
                "The ledger should hold the opening entry plus one entry per successful withdrawal");
    }

    private void concurrentDepositsAreAllApplied() {
        BankingService service = TestFixtures.service();
        service.createAccount("account-1", BigDecimal.ZERO);

        int successes = runConcurrently(500, index -> service.deposit("account-1", BigDecimal.ONE));

        assertEquals(500, successes, "Every deposit should succeed");
        assertBigDecimalEquals(new BigDecimal("500"), service.getBalance("account-1"),
                "No deposit may be lost to a concurrent update");
        assertEquals(500, service.getTransactionHistory("account-1").size(),
                "Every applied deposit should appear exactly once in the ledger");
    }

    private void concurrentTransfersConserveTotalMoney() {
        BankingService service = TestFixtures.service();
        service.createAccount("account-1", new BigDecimal("500"));
        service.createAccount("account-2", new BigDecimal("500"));

        runConcurrently(1000, index -> {
            if (index % 2 == 0) {
                service.transfer("account-1", "account-2", BigDecimal.ONE);
            } else {
                service.transfer("account-2", "account-1", BigDecimal.ONE);
            }
        });

        BigDecimal total = service.getBalance("account-1").add(service.getBalance("account-2"));
        assertBigDecimalEquals(new BigDecimal("1000"), total,
                "Transfers must neither create nor destroy money");
        assertTrue(service.getBalance("account-1").compareTo(BigDecimal.ZERO) >= 0,
                "No account may go negative");
        assertTrue(service.getBalance("account-2").compareTo(BigDecimal.ZERO) >= 0,
                "No account may go negative");
    }

    private void opposingTransfersDoNotDeadlock() {
        BankingService service = TestFixtures.service();
        service.createAccount("account-1", new BigDecimal("1000"));
        service.createAccount("account-2", new BigDecimal("1000"));

        int successes = runConcurrently(1000, index -> {
            if (index % 2 == 0) {
                service.transfer("account-1", "account-2", BigDecimal.ONE);
            } else {
                service.transfer("account-2", "account-1", BigDecimal.ONE);
            }
        });

        assertEquals(1000, successes,
                "Transfers acquired in opposing order should all complete without deadlocking");
    }

    private void concurrentAccountCreationAllowsExactlyOneWinner() {
        BankingService service = TestFixtures.service();

        AtomicInteger duplicates = new AtomicInteger();
        int successes = runConcurrently(200, index -> {
            try {
                service.createAccount("contended-account", new BigDecimal("10"));
            } catch (DuplicateAccountException exception) {
                duplicates.incrementAndGet();
                throw exception;
            }
        });

        assertEquals(1, successes, "Only one thread may create an account for a given id");
        assertEquals(199, duplicates.get(), "Every other thread should be rejected as a duplicate");
        assertBigDecimalEquals(new BigDecimal("10"), service.getBalance("contended-account"),
                "The winning creation should leave exactly one opening balance");
        assertEquals(1, service.getTransactionHistory("contended-account").size(),
                "Only one opening transaction should be recorded");
    }

    private void ledgerAgreesWithBalanceUnderConcurrency() {
        BankingService service = TestFixtures.service();
        service.createAccount("account-1", new BigDecimal("1000"));

        runConcurrently(900, index -> {
            if (index % 3 == 0) {
                service.withdraw("account-1", BigDecimal.ONE);
            } else {
                service.deposit("account-1", BigDecimal.ONE);
            }
        });

        List<Transaction> history = service.getTransactionHistory("account-1");
        BigDecimal replayed = BigDecimal.ZERO;
        for (Transaction transaction : history) {
            replayed = transaction.getDirection() == TransactionDirection.CREDIT
                    ? replayed.add(transaction.getAmount())
                    : replayed.subtract(transaction.getAmount());
        }

        assertBigDecimalEquals(service.getBalance("account-1"), replayed,
                "Replaying the ledger must reproduce the account balance exactly");
        assertBigDecimalEquals(service.getBalance("account-1"), last(history).getBalanceAfter(),
                "The final ledger entry must agree with the account balance");
    }

    private static int runConcurrently(int taskCount, IntConsumer task) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        for (int index = 0; index < taskCount; index++) {
            int taskIndex = index;
            executor.submit(() -> {
                try {
                    startGate.await();
                    task.accept(taskIndex);
                    successes.incrementAndGet();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException expectedRejection) {
                }
            });
        }
        startGate.countDown();
        executor.shutdown();
        try {
            assertTrue(executor.awaitTermination(COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "The concurrent operations did not finish in time, which suggests a deadlock");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for concurrent operations");
        }
        return successes.get();
    }
}
