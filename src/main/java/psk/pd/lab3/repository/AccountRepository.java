package psk.pd.lab3.repository;

import org.springframework.stereotype.Repository;
import psk.pd.lab3.model.BankAccount;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class AccountRepository {

    private final Map<String, BankAccount> store = new ConcurrentHashMap<>();

    public BankAccount save(BankAccount account) {
        store.put(account.getUsername(), account);
        return account;
    }

    public Optional<BankAccount> findByUsername(String username) {
        return Optional.ofNullable(store.get(username));
    }

    public List<BankAccount> findAll() {
        return new ArrayList<>(store.values());
    }
}
