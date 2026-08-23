package com.banking.system;

import java.util.List;

public final class SuiteResult {

    private final String suiteName;
    private final int passed;
    private final List<String> failures;

    SuiteResult(String suiteName, int passed, List<String> failures) {
        this.suiteName = suiteName;
        this.passed = passed;
        this.failures = List.copyOf(failures);
    }

    public String getSuiteName() {
        return suiteName;
    }

    public int getPassed() {
        return passed;
    }

    public int getFailed() {
        return failures.size();
    }

    public int getTotal() {
        return passed + failures.size();
    }

    public List<String> getFailures() {
        return failures;
    }

    public boolean isSuccessful() {
        return failures.isEmpty();
    }
}
