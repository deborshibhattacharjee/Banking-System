package com.banking.system.bean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class Account {
    private String id;
    private BigDecimal balance;
    private List<Transaction> transactions;

    public Account(String id, BigDecimal initialBalance) {
        this.id = Objects.requireNonNull(id);
        this.balance = Objects.requireNonNull(initialBalance);
    }

    public String getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void updateBalance(BigDecimal balance) {
        this.balance = Objects.requireNonNull(balance);
    }
}
