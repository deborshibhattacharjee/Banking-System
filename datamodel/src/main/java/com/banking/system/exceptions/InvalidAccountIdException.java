package com.banking.system.exceptions;

import com.banking.system.enums.BankingProcessorError;

public class InvalidAccountIdException extends BaseRuntimeException {
    public InvalidAccountIdException(BankingProcessorError error) {
        super(error);
    }
}
