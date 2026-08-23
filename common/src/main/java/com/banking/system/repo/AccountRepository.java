package com.banking.system.repo;

import com.banking.system.dtos.Account;

import java.util.Optional;

public interface AccountRepository {

    Optional<Account> fetchById(String accountId);

    boolean saveIfAbsent(Account account);
}
