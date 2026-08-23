package com.banking.system;

import com.banking.system.factory.BankingSystemFactory;
import com.banking.system.service.BankingService;

import java.math.BigDecimal;

public final class TestFixtures {

    public static final BigDecimal ACCOUNT_ONE_OPENING_BALANCE = new BigDecimal("100.00");
    public static final BigDecimal ACCOUNT_TWO_OPENING_BALANCE = new BigDecimal("50.00");

    private TestFixtures() {
    }

    public static BankingService service() {
        return BankingSystemFactory.newBankingService();
    }

    public static BankingService serviceWithZeroBalanceAccount() {
        BankingService service = service();
        service.createAccount("account-1", BigDecimal.ZERO);
        return service;
    }

    public static BankingService serviceWithOneAccount() {
        BankingService service = service();
        service.createAccount("account-1", ACCOUNT_ONE_OPENING_BALANCE);
        return service;
    }

    public static BankingService serviceWithTwoAccounts() {
        BankingService service = serviceWithOneAccount();
        service.createAccount("account-2", ACCOUNT_TWO_OPENING_BALANCE);
        return service;
    }
}
