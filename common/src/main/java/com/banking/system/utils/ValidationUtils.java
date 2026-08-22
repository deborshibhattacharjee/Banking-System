package com.banking.system.utils;

import com.banking.system.enums.BankingSystemError;
import com.banking.system.exceptions.BaseRuntimeException;

import java.util.List;

public class ValidationUtils {
    public static void throwIfTrue(boolean expression, BaseRuntimeException exception) {
        if (expression) throw exception;
    }

    public static void addToErrorsIfTrue(boolean expression, BankingSystemError error,
                                         List<BankingSystemError> errors) {
        if (expression) {
            errors.add(error);
        }
    }
}
