package com.banking.system;

import java.util.ArrayList;
import java.util.List;

public final class TestRunner {

    private final String suiteName;
    private final List<String> failures = new ArrayList<>();
    private int passed;

    public TestRunner(String suiteName) {
        this.suiteName = suiteName;
        System.out.println();
        System.out.println("[" + suiteName + "]");
    }

    public TestRunner test(String testName, Runnable test) {
        try {
            test.run();
            passed++;
            System.out.println("  PASS  " + testName);
        } catch (AssertionError error) {
            failures.add(testName + " -> " + error.getMessage());
            System.out.println("  FAIL  " + testName);
            System.out.println("        " + error.getMessage());
        } catch (RuntimeException error) {
            failures.add(testName + " -> threw " + error);
            System.out.println("  ERROR " + testName);
            System.out.println("        threw " + error);
        }
        return this;
    }

    public SuiteResult result() {
        return new SuiteResult(suiteName, passed, failures);
    }

    public static void exit(SuiteResult result) {
        System.out.println();
        System.out.println(summarise(result));
        System.exit(result.isSuccessful() ? 0 : 1);
    }

    public static String summarise(SuiteResult result) {
        return String.format("%-22s %2d/%2d passed%s", result.getSuiteName(), result.getPassed(),
                result.getTotal(), result.isSuccessful() ? "" : "  <-- FAILURES");
    }
}
