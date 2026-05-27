package psk.pd.lab3.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import psk.pd.lab3.exception.LoanNotFoundException;
import psk.pd.lab3.model.Loan;
import psk.pd.lab3.repository.LoanRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BankService bankService;

    @InjectMocks
    private LoanService loanService;

    // --- grantLoan ---

    @Test
    void grantLoan_createsLoanWithCorrectFields() {
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Loan result = loanService.grantLoan("jan", 100000, 5.0, 120, "bankier");

        assertNotNull(result.getId());
        assertEquals("jan", result.getBorrowerUsername());
        assertEquals(100000, result.getAmount(), 0.01);
        assertEquals(5.0, result.getAnnualInterestRate(), 0.01);
        assertEquals(120, result.getTermMonths());
        assertEquals("bankier", result.getGrantedBy());
        assertEquals("ACTIVE", result.getStatus());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void grantLoan_calculatesMonthlyPayment() {
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Loan result = loanService.grantLoan("jan", 100000, 5.0, 120, "bankier");

        assertEquals(1060.66, result.getMonthlyPayment(), 0.01);
    }

    @Test
    void grantLoan_calculatesTotalAmount() {
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Loan result = loanService.grantLoan("jan", 100000, 5.0, 120, "bankier");

        assertEquals(127279.20, result.getTotalAmount(), 1.0);
    }

    @Test
    void grantLoan_calculatesTotalInterest() {
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Loan result = loanService.grantLoan("jan", 100000, 5.0, 120, "bankier");

        assertEquals(27279.20, result.getTotalInterest(), 1.0);
    }

    @Test
    void grantLoan_depositsAmountToBorrowerAccount() {
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loanService.grantLoan("jan", 50000, 7.5, 60, "bankier");

        verify(bankService).deposit("jan", 50000, "bankier");
    }

    @Test
    void grantLoan_savesToRepository() {
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loanService.grantLoan("anna", 20000, 3.0, 24, "bankier");

        ArgumentCaptor<Loan> captor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(captor.capture());
        assertEquals("anna", captor.getValue().getBorrowerUsername());
    }

    @Test
    void grantLoan_zeroInterestRate_calculatesEqualPayments() {
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Loan result = loanService.grantLoan("jan", 12000, 0, 12, "bankier");

        assertEquals(1000.0, result.getMonthlyPayment(), 0.01);
        assertEquals(12000.0, result.getTotalAmount(), 0.01);
        assertEquals(0.0, result.getTotalInterest(), 0.01);
    }

    // --- PMT calculation (via grantLoan) with @ParameterizedTest ---

    @ParameterizedTest
    @CsvSource({
            "100000, 5.0,  120, 1060.66",
            "12000,  0.0,  12,  1000.00",
            "50000,  7.5,  60,  1001.90",
            "200000, 3.5,  360, 898.09",
            "10000,  10.0, 24,  461.45"
    })
    void grantLoan_variousInputs_correctMonthlyPayment(double amount, double rate,
                                                        int months, double expectedPayment) {
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Loan result = loanService.grantLoan("jan", amount, rate, months, "bankier");

        assertEquals(expectedPayment, result.getMonthlyPayment(), 0.01);
    }

    // --- getAllLoans ---

    @Test
    void getAllLoans_returnsList() {
        Loan l1 = new Loan();
        l1.setId("1");
        Loan l2 = new Loan();
        l2.setId("2");
        when(loanRepository.findAll()).thenReturn(List.of(l1, l2));

        List<Loan> result = loanService.getAllLoans();

        assertEquals(2, result.size());
        verify(loanRepository).findAll();
    }

    @Test
    void getAllLoans_emptyList() {
        when(loanRepository.findAll()).thenReturn(List.of());

        List<Loan> result = loanService.getAllLoans();

        assertTrue(result.isEmpty());
    }

    // --- getLoansByBorrower ---

    @Test
    void getLoansByBorrower_returnsFilteredList() {
        Loan l = new Loan();
        l.setBorrowerUsername("jan");
        when(loanRepository.findByBorrower("jan")).thenReturn(List.of(l));

        List<Loan> result = loanService.getLoansByBorrower("jan");

        assertEquals(1, result.size());
        assertEquals("jan", result.getFirst().getBorrowerUsername());
    }

    @Test
    void getLoansByBorrower_emptyForUserWithNoLoans() {
        when(loanRepository.findByBorrower("anna")).thenReturn(List.of());

        List<Loan> result = loanService.getLoansByBorrower("anna");

        assertTrue(result.isEmpty());
    }

    // --- changeInterestRate ---

    @Test
    void changeInterestRate_existingLoan_updatesRate() {
        Loan loan = new Loan();
        loan.setId("abc-123");
        loan.setAmount(100000);
        loan.setAnnualInterestRate(5.0);
        loan.setTermMonths(120);
        when(loanRepository.findById("abc-123")).thenReturn(Optional.of(loan));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Loan result = loanService.changeInterestRate("abc-123", 7.0);

        assertEquals(7.0, result.getAnnualInterestRate(), 0.01);
    }

    @Test
    void changeInterestRate_recalculatesPayments() {
        Loan loan = new Loan();
        loan.setId("abc-123");
        loan.setAmount(100000);
        loan.setAnnualInterestRate(5.0);
        loan.setTermMonths(120);
        when(loanRepository.findById("abc-123")).thenReturn(Optional.of(loan));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Loan result = loanService.changeInterestRate("abc-123", 7.0);

        assertEquals(1161.08, result.getMonthlyPayment(), 0.01);
        assertTrue(result.getTotalAmount() > 100000);
        assertTrue(result.getTotalInterest() > 0);
    }

    @Test
    void changeInterestRate_savesToRepository() {
        Loan loan = new Loan();
        loan.setId("abc-123");
        loan.setAmount(50000);
        loan.setAnnualInterestRate(5.0);
        loan.setTermMonths(60);
        when(loanRepository.findById("abc-123")).thenReturn(Optional.of(loan));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loanService.changeInterestRate("abc-123", 3.0);

        verify(loanRepository).save(loan);
    }

    @Test
    void changeInterestRate_nonExistingLoan_throwsLoanNotFoundException() {
        when(loanRepository.findById("nie-istnieje")).thenReturn(Optional.empty());

        assertThrows(LoanNotFoundException.class,
                () -> loanService.changeInterestRate("nie-istnieje", 5.0));
    }

    @Test
    void changeInterestRate_toZero_calculatesEqualPayments() {
        Loan loan = new Loan();
        loan.setId("abc-123");
        loan.setAmount(12000);
        loan.setAnnualInterestRate(5.0);
        loan.setTermMonths(12);
        when(loanRepository.findById("abc-123")).thenReturn(Optional.of(loan));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Loan result = loanService.changeInterestRate("abc-123", 0);

        assertEquals(1000.0, result.getMonthlyPayment(), 0.01);
        assertEquals(0.0, result.getTotalInterest(), 0.01);
    }
}
