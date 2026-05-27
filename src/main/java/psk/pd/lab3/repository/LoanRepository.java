package psk.pd.lab3.repository;

import org.springframework.stereotype.Repository;
import psk.pd.lab3.model.Loan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class LoanRepository {

    private final Map<String, Loan> store = new ConcurrentHashMap<>();

    public Loan save(Loan loan) {
        store.put(loan.getId(), loan);
        return loan;
    }

    public Optional<Loan> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Loan> findAll() {
        return new ArrayList<>(store.values());
    }

    public List<Loan> findByBorrower(String username) {
        return store.values().stream()
                .filter(l -> l.getBorrowerUsername().equals(username))
                .toList();
    }
}
