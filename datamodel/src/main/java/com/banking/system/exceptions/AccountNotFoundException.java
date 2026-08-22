package com.banking.system.exceptions;

import com.banking.system.enums.BankingProcessorError;

public class AccountNotFoundException extends BaseRuntimeException {
    public AccountNotFoundException(BankingProcessorError error) {
        super(error);
    }
}
