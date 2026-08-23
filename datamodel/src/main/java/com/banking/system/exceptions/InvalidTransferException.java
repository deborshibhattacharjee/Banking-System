package com.banking.system.exceptions;

import com.banking.system.enums.BankingSystemError;

public class InvalidTransferException extends BaseRuntimeException {
    public InvalidTransferException(BankingSystemError error) {
        super(error);
    }

    private static final long serialVersionUID = 1L;
}
