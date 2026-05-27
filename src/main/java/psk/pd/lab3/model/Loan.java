package psk.pd.lab3.model;

import java.time.LocalDateTime;

public class Loan {

    private String id;
    private String borrowerUsername;
    private double amount;
    private double annualInterestRate;
    private int termMonths;
    private double monthlyPayment;
    private double totalAmount;
    private double totalInterest;
    private String status;
    private String grantedBy;
    private LocalDateTime createdAt;

    public Loan() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBorrowerUsername() { return borrowerUsername; }
    public void setBorrowerUsername(String borrowerUsername) { this.borrowerUsername = borrowerUsername; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getAnnualInterestRate() { return annualInterestRate; }
    public void setAnnualInterestRate(double annualInterestRate) { this.annualInterestRate = annualInterestRate; }

    public int getTermMonths() { return termMonths; }
    public void setTermMonths(int termMonths) { this.termMonths = termMonths; }

    public double getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(double monthlyPayment) { this.monthlyPayment = monthlyPayment; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getTotalInterest() { return totalInterest; }
    public void setTotalInterest(double totalInterest) { this.totalInterest = totalInterest; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getGrantedBy() { return grantedBy; }
    public void setGrantedBy(String grantedBy) { this.grantedBy = grantedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
