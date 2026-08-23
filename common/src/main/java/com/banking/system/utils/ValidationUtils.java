package com.banking.system.utils;

import com.banking.system.exceptions.BaseRuntimeException;

import java.util.function.Supplier;

public class ValidationUtils {

    public static void throwIfTrue(boolean expression, Supplier<? extends BaseRuntimeException> exceptionSupplier) {
        if (expression) {
            throw exceptionSupplier.get();
        }
    }
}
