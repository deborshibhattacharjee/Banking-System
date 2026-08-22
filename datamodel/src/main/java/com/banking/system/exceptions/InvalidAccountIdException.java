package com.banking.system.exceptions;

import com.banking.system.enums.BankingSystemError;

public class InvalidAccountIdException extends BaseRuntimeException {
    public InvalidAccountIdException(BankingSystemError error) {
        super(error);
    }
}
