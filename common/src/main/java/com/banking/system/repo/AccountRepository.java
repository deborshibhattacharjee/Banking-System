package com.banking.system.repo;

import com.banking.system.bean.Account;

import java.math.BigDecimal;
import java.util.Optional;

public interface AccountRepository {

    Optional<Account> fetchById(String accountId);

    Optional<BigDecimal> getBalance(String accountId);

    boolean existsById(String accountId);

    Account save(Account account);
}
