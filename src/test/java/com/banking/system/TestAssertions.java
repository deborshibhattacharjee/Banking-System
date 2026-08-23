package com.banking.system;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class TestAssertions {

    private TestAssertions() {
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
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

    public static void assertNotNull(Object value, String message) {
        if (value == null) {
            throw new AssertionError(message);
        }
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    public static <T> T first(List<T> values) {
        if (values.isEmpty()) {
            throw new AssertionError("Expected at least one element, but the list was empty");
        }
        return values.get(0);
    }

    public static <T> T last(List<T> values) {
        if (values.isEmpty()) {
            throw new AssertionError("Expected at least one element, but the list was empty");
        }
        return values.get(values.size() - 1);
    }
}
