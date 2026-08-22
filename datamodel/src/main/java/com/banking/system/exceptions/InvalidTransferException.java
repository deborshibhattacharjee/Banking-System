package com.banking.system.exceptions;

import com.banking.system.enums.BankingSystemError;

public class InvalidTransferException extends BaseRuntimeException {
    public InvalidTransferException(BankingSystemError error) {
        super(error);
    }
}
