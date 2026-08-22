package com.banking.system.exceptions;

import com.banking.system.enums.BankingSystemError;

public class InsufficientFundsException extends AccountNotFoundException {
    public InsufficientFundsException(BankingSystemError error) {
        super(error);
    }
}
