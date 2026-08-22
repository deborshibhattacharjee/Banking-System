package com.banking.system.exceptions;

import com.banking.system.enums.BankingSystemError;

public class InvalidAmountException extends BaseRuntimeException {
    public InvalidAmountException(BankingSystemError error) {
        super(error);
    }
}
