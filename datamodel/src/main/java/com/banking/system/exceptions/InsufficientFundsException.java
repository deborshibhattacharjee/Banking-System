package com.banking.system.exceptions;

import com.banking.system.enums.BankingSystemError;

public class InsufficientFundsException extends BaseRuntimeException {
    public InsufficientFundsException(BankingSystemError error, String... params) {
        super(error, params);
    }
}
