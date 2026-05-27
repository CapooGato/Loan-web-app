package psk.pd.lab3.service;

import org.springframework.stereotype.Service;
import psk.pd.lab3.exception.LoanNotFoundException;
import psk.pd.lab3.model.Loan;
import psk.pd.lab3.repository.LoanRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BankService bankService;

    public LoanService(LoanRepository loanRepository, BankService bankService) {
        this.loanRepository = loanRepository;
        this.bankService = bankService;
    }

    public Loan grantLoan(String borrowerUsername, double amount, double annualInterestRate,
                          int termMonths, String grantedBy) {
        Loan loan = new Loan();
        loan.setId(UUID.randomUUID().toString());
        loan.setBorrowerUsername(borrowerUsername);
        loan.setAmount(amount);
        loan.setAnnualInterestRate(annualInterestRate);
        loan.setTermMonths(termMonths);
        loan.setGrantedBy(grantedBy);
        loan.setStatus("ACTIVE");
        loan.setCreatedAt(LocalDateTime.now());
        recalculate(loan);

        bankService.deposit(borrowerUsername, amount, grantedBy);

        return loanRepository.save(loan);
    }

    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    public List<Loan> getLoansByBorrower(String username) {
        return loanRepository.findByBorrower(username);
    }

    public Loan changeInterestRate(String loanId, double newRate) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));
        loan.setAnnualInterestRate(newRate);
        recalculate(loan);
        return loanRepository.save(loan);
    }

    private void recalculate(Loan loan) {
        double monthly = calculateMonthlyPayment(
                loan.getAmount(), loan.getAnnualInterestRate(), loan.getTermMonths());
        double total = monthly * loan.getTermMonths();
        double interest = total - loan.getAmount();

        loan.setMonthlyPayment(Math.round(monthly * 100.0) / 100.0);
        loan.setTotalAmount(Math.round(total * 100.0) / 100.0);
        loan.setTotalInterest(Math.round(interest * 100.0) / 100.0);
    }

    private double calculateMonthlyPayment(double principal, double annualRate, int termMonths) {
        if (annualRate == 0) {
            return principal / termMonths;
        }
        double r = annualRate / 100.0 / 12.0;
      return principal * (r * Math.pow(1 + r, termMonths)) / (Math.pow(1 + r, termMonths) - 1);
    }
}
