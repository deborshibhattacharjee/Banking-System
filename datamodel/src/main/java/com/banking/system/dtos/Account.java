package com.banking.system.dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class Account {

    private final String id;
    private volatile BigDecimal balance;
    private final List<String> transactions = new CopyOnWriteArrayList<>();

    public Account(String id, BigDecimal initialBalance) {
        this.id = Objects.requireNonNull(id, "Account id cannot be null");
        this.balance = Objects.requireNonNull(initialBalance, "Account balance cannot be null");
    }

    public String getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public boolean hasAtLeast(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }

    public void add(BigDecimal amount) {
        this.balance = balance.add(amount);
    }

    public void subtract(BigDecimal amount) {
        this.balance = balance.subtract(amount);
    }

    public void addToTransactions(String transactionId) {
        transactions.add(transactionId);
    }

    public List<String> getTransactions() {
        return List.copyOf(transactions);
    }
}
