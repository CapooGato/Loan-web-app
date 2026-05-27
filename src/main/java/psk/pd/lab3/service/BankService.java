package psk.pd.lab3.service;

import org.springframework.stereotype.Service;
import psk.pd.lab3.model.BankAccount;
import psk.pd.lab3.model.Transaction;
import psk.pd.lab3.repository.AccountRepository;
import psk.pd.lab3.repository.TransactionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BankService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public BankService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public BankAccount getAccount(String username) {
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Konto nie znalezione: " + username));
    }

    public List<BankAccount> getAllAccounts() {
        return accountRepository.findAll();
    }

    public void deposit(String username, double amount, String performedBy) {
        if (amount <= 0) {
            throw new RuntimeException("Kwota musi byc wieksza od 0");
        }
        BankAccount account = getAccount(username);
        account.setBalance(account.getBalance() + amount);
        accountRepository.save(account);

        Transaction t = new Transaction();
        t.setId(UUID.randomUUID().toString());
        t.setUsername(username);
        t.setType(Transaction.Type.DEPOSIT);
        t.setAmount(amount);
        t.setPerformedBy(performedBy);
        t.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(t);
    }

    public void withdraw(String username, double amount, String performedBy) {
        if (amount <= 0) {
            throw new RuntimeException("Kwota musi byc wieksza od 0");
        }
        BankAccount account = getAccount(username);
        if (account.getBalance() < amount) {
            throw new RuntimeException("Niewystarczajace srodki na koncie");
        }
        account.setBalance(account.getBalance() - amount);
        accountRepository.save(account);

        Transaction t = new Transaction();
        t.setId(UUID.randomUUID().toString());
        t.setUsername(username);
        t.setType(Transaction.Type.WITHDRAWAL);
        t.setAmount(amount);
        t.setPerformedBy(performedBy);
        t.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(t);
    }

    public List<Transaction> getTransactions(String username) {
        return transactionRepository.findByUsername(username);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
}
