package com.banking.system.utils;

import com.banking.system.exceptions.BaseRuntimeException;

public class ValidationUtils {
    public static void throwIfTrue(boolean expression, BaseRuntimeException exception) {
        if (expression) throw exception;
    }
}
