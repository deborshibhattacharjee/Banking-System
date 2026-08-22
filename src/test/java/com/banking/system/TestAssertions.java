package com.banking.system;

import java.math.BigDecimal;

public class TestAssertions {

    private TestAssertions() {
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " | Expected: " + expected + ", Actual: " + actual);
        }
    }

    public static void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual, String message) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || actual == null || expected.compareTo(actual) != 0) {
            throw new AssertionError(
                    message + " | Expected: " + expected + ", Actual: " + actual);
        }
    }

    public static void assertThrows(
            Class<? extends RuntimeException> expectedException, Runnable operation, String message) {
        try {
            operation.run();
        } catch (RuntimeException actualException) {
            if (expectedException.isInstance(actualException)) {
                return;
            }
            throw new AssertionError(
                    message + " | Expected exception: " + expectedException.getSimpleName()
                            + ", Actual exception: " + actualException.getClass().getSimpleName());
        }
        throw new AssertionError(
                message + " | Expected exception: " + expectedException.getSimpleName()
                        + ", but no exception was thrown");
    }
}