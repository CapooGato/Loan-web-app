package psk.pd.lab3.repository;

import org.springframework.stereotype.Repository;
import psk.pd.lab3.model.Transaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TransactionRepository {

    private final Map<String, Transaction> store = new ConcurrentHashMap<>();

    public Transaction save(Transaction transaction) {
        store.put(transaction.getId(), transaction);
        return transaction;
    }

    public List<Transaction> findByUsername(String username) {
        return store.values().stream()
                .filter(t -> t.getUsername().equals(username))
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .toList();
    }

    public List<Transaction> findAll() {
        return new ArrayList<>(store.values());
    }
}
