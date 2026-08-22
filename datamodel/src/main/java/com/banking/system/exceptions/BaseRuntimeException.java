package com.banking.system.exceptions;

import com.banking.system.enums.BankingSystemError;

import static com.banking.system.Constants.COLON;

public class BaseRuntimeException extends RuntimeException {

    public BaseRuntimeException(BankingSystemError error, String... params) {
        super(getMessage(error, params));
    }

    private static String getMessage(BankingSystemError error, String... params) {
        String message = error.getOptionalDebugMessage();
        if(params.length > 0) {
            message = String.format(message, params);
        }
        return String.join(COLON, error.getErrorCode(), message);
    }
}
