package psk.pd.lab3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppUsersConfig {

    private List<UserDefinition> users = new ArrayList<>();

    public List<UserDefinition> getUsers() { return users; }
    public void setUsers(List<UserDefinition> users) { this.users = users; }

    public static class UserDefinition {
        private String username;
        private String password;
        private String role;
        private double initialBalance;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public double getInitialBalance() { return initialBalance; }
        public void setInitialBalance(double initialBalance) { this.initialBalance = initialBalance; }
    }
}
