package com.banking.system.enums;

public enum BankingSystemError {
    DUPLICATE_ACCOUNT("BP1001", "AccountId already present in system"),
    INVALID_ACCOUNT_ID("BP1002", "Invalid account id"),
    ACCOUNT_BALANCE_CANNOT_BE_NULL("BP1003", "Account balance cannot be null"),
    ACCOUNT_NOT_FOUND("BP1004", "Account with id %s not found in system"),
    AMOUNT_MUST_BE_GREATER_THAN_ZERO("BP1005",
            "Amount entered must be greater than zero"),
    INSUFFICIENT_FUNDS_FOR_WITHDRAWAL("BP1006",
            "Account %s has insufficient funds for withdrawal"),
    INVALID_OPERATION_TRANSFER_TO_SAME_ACCOUNT("BP1007",
            "Invalid operation, cannot transfer amount to same account");

    private final String errorCode;
    private final String optionalDebugMessage;

    BankingSystemError(String errorCode, String optionalDebugMessage) {
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
