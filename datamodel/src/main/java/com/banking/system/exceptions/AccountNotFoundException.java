package com.banking.system.exceptions;

import com.banking.system.enums.BankingSystemError;

public class AccountNotFoundException extends BaseRuntimeException {

    public AccountNotFoundException(BankingSystemError error, String... params) {
        super(error, params);
    }

    private static final long serialVersionUID = 1L;
}
