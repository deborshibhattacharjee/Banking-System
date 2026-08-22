package com.banking.system.enums;

public enum BankingProcessorError {
    DUPLICATE_ACCOUNT("BP1001", "AccountId already present in system"),
    INVALID_ACCOUNT_ID("BP1002", "Invalid account id"),
    ACCOUNT_BALANCE_CANNOT_BE_NULL("BP1003", "Account balance cannot be null"),
    ACCOUNT_NOT_FOUND("BP1004", "Account with id not found in system"),
    DEPOSIT_AMOUNT_MUST_BE_GREATER_THAN_ZERO("BP1005",
            "Deposit amount must be greater than zero"),
    ;

    private final String errorCode;
    private final String optionalDebugMessage;

    BankingProcessorError(String errorCode, String optionalDebugMessage) {
        this.errorCode = errorCode;
        this.optionalDebugMessage = optionalDebugMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getOptionalDebugMessage() {
        return optionalDebugMessage;
    }
}
