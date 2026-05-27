package psk.pd.lab3.model;

import java.time.LocalDateTime;

public class Transaction {

    public enum Type {
        DEPOSIT, WITHDRAWAL, LOAN
    }

    private String id;
    private String username;
    private Type type;
    private double amount;
    private String performedBy;
    private LocalDateTime createdAt;

    public Transaction() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
