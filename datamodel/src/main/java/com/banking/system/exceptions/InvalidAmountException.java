package com.banking.system.exceptions;

import com.banking.system.enums.BankingProcessorError;

public class InvalidAmountException extends BaseRuntimeException {
    public InvalidAmountException(BankingProcessorError error) {
        super(error);
    }
}
