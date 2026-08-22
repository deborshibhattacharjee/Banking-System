package com.banking.system.utils;

import com.banking.system.enums.BankingProcessorError;
import com.banking.system.exceptions.BaseRuntimeException;

import java.util.List;

public class ValidationUtils {
    public static void throwIfTrue(boolean expression, BaseRuntimeException exception) {
        if (expression) throw exception;
    }

    public static void addToErrorsIfTrue(boolean expression, BankingProcessorError error,
                                         List<BankingProcessorError> errors) {
        if (expression) {
            errors.add(error);
        }
    }
}
