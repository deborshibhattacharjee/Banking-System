package com.banking.system.exceptions;

import com.banking.system.enums.BankingSystemError;

public class DuplicateAccountException extends BaseRuntimeException {
    public DuplicateAccountException(BankingSystemError error) {
        super(error);
    }
}
