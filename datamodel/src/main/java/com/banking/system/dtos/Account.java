package com.banking.system.dtos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Account {
    private String id;
    private BigDecimal balance;
    private List<String> transactions = new ArrayList<>();

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

    public void add(BigDecimal amount) {
        this.balance = balance.add(amount);
    }

    public void subtract(BigDecimal amount) {
        this.balance = balance.subtract(amount);
    }

    public void addToTransactions(String transactionId) {
        if(transactions == null){
            transactions = new ArrayList<>();
        }
        transactions.add(transactionId);
    }

    public List<String> getTransactions() {
        return transactions;
    }
}
