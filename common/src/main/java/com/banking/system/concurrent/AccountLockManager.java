package com.banking.system.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public final class AccountLockManager {

    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void inAccountScope(String accountId, Runnable operation) {
        inAccountScope(accountId, () -> {
            operation.run();
            return null;
        });
    }

    public <T> T inAccountScope(String accountId, Supplier<T> operation) {
        ReentrantLock lock = lockFor(accountId);
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }

    public void inAccountScope(String firstAccountId, String secondAccountId, Runnable operation) {
        boolean firstIsLower = firstAccountId.compareTo(secondAccountId) <= 0;
        ReentrantLock lowerLock = lockFor(firstIsLower ? firstAccountId : secondAccountId);
        ReentrantLock higherLock = lockFor(firstIsLower ? secondAccountId : firstAccountId);
        lowerLock.lock();
        try {
            higherLock.lock();
            try {
                operation.run();
            } finally {
                higherLock.unlock();
            }
        } finally {
            lowerLock.unlock();
        }
    }

    private ReentrantLock lockFor(String accountId) {
        return locks.computeIfAbsent(accountId, id -> new ReentrantLock());
    }
}
