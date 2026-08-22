package com.banking.system.dtos;

import java.math.BigDecimal;
import java.util.Objects;

public final class Transaction {

    private final String transactionId;
    private final String accountId;
    private final String type;
    private final String direction;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String relatedAccountId;
    private final Long timestamp;

    public Transaction(String transactionId, String accountId, String type, String direction,
                       BigDecimal amount, BigDecimal balanceAfter, String relatedAccountId, Long timestamp) {

        this.transactionId = requireText(transactionId, "Transaction ID cannot be null or blank");
        this.accountId = requireText(accountId, "Account ID cannot be null or blank");
        this.type = Objects.requireNonNull(type, "Transaction type cannot be null");
        this.direction = Objects.requireNonNull(direction, "Transaction direction cannot be null");
        this.amount = requireNonNegativeAmount(amount);
        this.balanceAfter = requireNonNegativeAmount(balanceAfter);
        this.relatedAccountId = relatedAccountId;
        this.timestamp = Objects.requireNonNull(timestamp, "Transaction timestamp cannot be null");
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getType() {
        return type;
    }

    public String getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getRelatedAccountId() {
        return relatedAccountId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static BigDecimal requireNonNegativeAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        return amount;
    }
}
