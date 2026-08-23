package com.banking.system;

import java.util.List;

public final class TestSuite {

    public static void main(String[] args) {
        List<SuiteResult> results = List.of(
                AccountCreationTest.run(),
                DepositTest.run(),
                WithdrawalTest.run(),
                TransferTest.run(),
                TransactionHistoryTest.run(),
                ConcurrencyTest.run());

        int passed = results.stream().mapToInt(SuiteResult::getPassed).sum();
        int total = results.stream().mapToInt(SuiteResult::getTotal).sum();

        System.out.println();
        System.out.println("=========================================");
        results.forEach(result -> System.out.println("  " + TestRunner.summarise(result)));
        System.out.println("=========================================");
        System.out.println("  TOTAL                " + passed + "/" + total + " passed");
        System.out.println();

        results.stream()
                .filter(result -> !result.isSuccessful())
                .forEach(result -> result.getFailures()
                        .forEach(failure -> System.out.println("  " + result.getSuiteName() + ": " + failure)));

        System.exit(passed == total ? 0 : 1);
    }
}
