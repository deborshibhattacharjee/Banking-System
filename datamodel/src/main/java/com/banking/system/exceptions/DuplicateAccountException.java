package com.banking.system.exceptions;

import com.banking.system.enums.BankingProcessorError;

public class DuplicateAccountException extends BaseRuntimeException {
    public DuplicateAccountException(BankingProcessorError error) {
        super(error);
    }
}
