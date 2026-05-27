package psk.pd.lab3.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import psk.pd.lab3.model.BankAccount;
import psk.pd.lab3.repository.AccountRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final AppUsersConfig config;

    public DataInitializer(AccountRepository accountRepository, AppUsersConfig config) {
        this.accountRepository = accountRepository;
        this.config = config;
    }

    @Override
    public void run(String... args) {
        config.getUsers().stream()
                .filter(u -> "USER".equals(u.getRole()))
                .forEach(u -> {
                    BankAccount account = new BankAccount(u.getUsername(), u.getInitialBalance());
                    accountRepository.save(account);
                });
    }
}
