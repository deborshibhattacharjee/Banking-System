package com.banking.system;

import com.banking.system.database.InMemoryAccountDatabase;
import com.banking.system.database.InMemoryTransactionDatabase;
import com.banking.system.dtos.Transaction;
import com.banking.system.repo.AccountRepository;
import com.banking.system.repo.TransactionRepository;
import com.banking.system.repo.impl.AccountRepositoryImpl;
import com.banking.system.repo.impl.TransactionRepositoryImpl;
import com.banking.system.service.BankingService;
import com.banking.system.service.TransactionService;
import com.banking.system.service.impl.BankingServiceImpl;
import com.banking.system.service.impl.TransactionServiceImpl;
import com.banking.system.validator.BankingServiceValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class BankingSystemApplication {

    private static BankingService bankingService;
    private static TransactionService transactionService;

    public static void main(String[] args) {
        createBankingService();
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            System.out.println("=================================");
            System.out.println(" Banking Transaction Processor");
            System.out.println("=================================");
            while (running) {
                printMenu();
                String choice = scanner.nextLine().trim();
                try {
                    switch (choice) {
                        case "1" -> createAccount(scanner, bankingService);
                        case "2" -> deposit(scanner, bankingService);
                        case "3" -> withdraw(scanner, bankingService);
                        case "4" -> transfer(scanner, bankingService);
                        case "5" -> showBalance(scanner, bankingService);
                        case "6" -> showTransactionHistory(scanner, bankingService);
                        case "0" -> {
                            running = false;
                            System.out.println("Goodbye.");
                        }
                        default -> System.out.println("Invalid option. Please choose again.");
                    }
                } catch (RuntimeException exception) {
                    System.out.println();
                    System.out.println("Operation failed: " + exception.getMessage());
                    System.out.println();
                }
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("Choose an operation:");
        System.out.println("1. Create account");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Transfer");
        System.out.println("5. Check balance");
        System.out.println("6. View transaction history");
        System.out.println("0. Exit");
        System.out.print("Enter your choice: ");
    }

    private static void createAccount(Scanner scanner, BankingService bankingService) {
        String accountId = readText(scanner, "Enter account ID: ");
        BigDecimal initialBalance = readAmount(scanner, "Enter initial balance: ");
        bankingService.createAccount(accountId, initialBalance);
        System.out.println("Account created successfully.");
        System.out.println("Account ID: " + accountId);
        System.out.println("Balance: " + bankingService.getBalance(accountId));
    }

    private static void deposit(Scanner scanner, BankingService bankingService) {
        String accountId = readText(scanner, "Enter account ID: ");
        BigDecimal amount = readAmount(scanner, "Enter deposit amount: ");
        bankingService.deposit(accountId, amount);
        System.out.println("Deposit completed successfully.");
        System.out.println("New balance: " + bankingService.getBalance(accountId));
    }

    private static void withdraw(Scanner scanner, BankingService bankingService) {
        String accountId = readText(scanner, "Enter account ID: ");
        BigDecimal amount = readAmount(scanner, "Enter withdrawal amount: ");
        bankingService.withdraw(accountId, amount);
        System.out.println("Withdrawal completed successfully.");
        System.out.println("New balance: " + bankingService.getBalance(accountId));
    }

    private static void transfer(Scanner scanner, BankingService bankingService) {
        String sourceAccountId = readText(scanner, "Enter source account ID: ");
        String destinationAccountId = readText(scanner, "Enter destination account ID: ");
        BigDecimal amount = readAmount(scanner, "Enter transfer amount: ");
        bankingService.transfer(sourceAccountId, destinationAccountId, amount);
        System.out.println("Transfer completed successfully.");
        System.out.println("Source balance: " + bankingService.getBalance(sourceAccountId));
        System.out.println("Destination balance: " + bankingService.getBalance(destinationAccountId));
    }

    private static void showBalance(Scanner scanner, BankingService bankingService) {
        String accountId = readText(scanner, "Enter account ID: ");
        BigDecimal balance = bankingService.getBalance(accountId);
        System.out.println("Account balance: " + balance);
    }

    private static void showTransactionHistory(Scanner scanner, BankingService bankingService) {
        String accountId = readText(scanner, "Enter account ID: ");
        List<String> transactionIds = bankingService.getTransactionHistory(accountId);
        List<Transaction> transactions = transactionService.findByTransactionIds(transactionIds);
        if (transactionIds.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }

        System.out.println("Transaction history for " + accountId + ":");
        for (int index = 0; index < transactions.size(); index++) {
            System.out.println((index + 1) + ". " + transactions.get(index));
        }
    }

    private static String readText(Scanner scanner, String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    private static BigDecimal readAmount(Scanner scanner, String message) {
        System.out.print(message);
        String input = scanner.nextLine().trim();
        try {
            return new BigDecimal(input);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Please enter a valid monetary amount.");
        }
    }

    private static void createBankingService() {
        InMemoryAccountDatabase accountDatabase = new InMemoryAccountDatabase();
        InMemoryTransactionDatabase transactionDatabase = new InMemoryTransactionDatabase();
        AccountRepository accountRepository = new AccountRepositoryImpl(accountDatabase);
        TransactionRepository transactionRepository = new TransactionRepositoryImpl(transactionDatabase);
        BankingServiceValidator validator = new BankingServiceValidator(accountRepository);
        transactionService = new TransactionServiceImpl(transactionRepository);
        bankingService = new BankingServiceImpl(accountRepository, validator, transactionService);
    }
}
