package psk.pd.lab3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import psk.pd.lab3.model.BankAccount;
import psk.pd.lab3.model.Transaction;
import psk.pd.lab3.repository.AccountRepository;
import psk.pd.lab3.repository.TransactionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BankService bankService;

    private BankAccount testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new BankAccount("jan", 5000.0);
    }

    // --- getAccount ---

    @Test
    void getAccount_existingUser_returnsAccount() {
        when(accountRepository.findByUsername("jan")).thenReturn(Optional.of(testAccount));

        BankAccount result = bankService.getAccount("jan");

        assertEquals("jan", result.getUsername());
        assertEquals(5000.0, result.getBalance());
        verify(accountRepository).findByUsername("jan");
    }

    @Test
    void getAccount_nonExistingUser_throwsException() {
        when(accountRepository.findByUsername("nieznany")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bankService.getAccount("nieznany"));

        assertTrue(ex.getMessage().contains("nieznany"));
    }

    // --- getAllAccounts ---

    @Test
    void getAllAccounts_returnsList() {
        BankAccount acc2 = new BankAccount("anna", 10000.0);
        when(accountRepository.findAll()).thenReturn(List.of(testAccount, acc2));

        List<BankAccount> result = bankService.getAllAccounts();

        assertEquals(2, result.size());
        verify(accountRepository).findAll();
    }

    @Test
    void getAllAccounts_emptyList() {
        when(accountRepository.findAll()).thenReturn(List.of());

        List<BankAccount> result = bankService.getAllAccounts();

        assertTrue(result.isEmpty());
    }

    // --- deposit ---

    @Test
    void deposit_validAmount_increasesBalance() {
        when(accountRepository.findByUsername("jan")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bankService.deposit("jan", 1000.0, "jan");

        assertEquals(6000.0, testAccount.getBalance(), 0.01);
        verify(accountRepository).save(testAccount);
    }

    @Test
    void deposit_createsTransaction() {
        when(accountRepository.findByUsername("jan")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bankService.deposit("jan", 500.0, "bankier");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());

        Transaction saved = captor.getValue();
        assertEquals("jan", saved.getUsername());
        assertEquals(Transaction.Type.DEPOSIT, saved.getType());
        assertEquals(500.0, saved.getAmount(), 0.01);
        assertEquals("bankier", saved.getPerformedBy());
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void deposit_zeroAmount_throwsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bankService.deposit("jan", 0, "jan"));

        assertTrue(ex.getMessage().contains("wieksza od 0"));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void deposit_negativeAmount_throwsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bankService.deposit("jan", -100, "jan"));

        assertTrue(ex.getMessage().contains("wieksza od 0"));
    }

    @Test
    void deposit_nonExistingUser_throwsException() {
        when(accountRepository.findByUsername("nieznany")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> bankService.deposit("nieznany", 100, "bankier"));
    }

    // --- withdraw ---

    @Test
    void withdraw_validAmount_decreasesBalance() {
        when(accountRepository.findByUsername("jan")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bankService.withdraw("jan", 2000.0, "jan");

        assertEquals(3000.0, testAccount.getBalance(), 0.01);
        verify(accountRepository).save(testAccount);
    }

    @Test
    void withdraw_createsTransaction() {
        when(accountRepository.findByUsername("jan")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bankService.withdraw("jan", 1000.0, "bankier");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());

        Transaction saved = captor.getValue();
        assertEquals("jan", saved.getUsername());
        assertEquals(Transaction.Type.WITHDRAWAL, saved.getType());
        assertEquals(1000.0, saved.getAmount(), 0.01);
        assertEquals("bankier", saved.getPerformedBy());
    }

    @Test
    void withdraw_insufficientFunds_throwsException() {
        when(accountRepository.findByUsername("jan")).thenReturn(Optional.of(testAccount));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bankService.withdraw("jan", 9999.0, "jan"));

        assertTrue(ex.getMessage().contains("Niewystarczajace"));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_exactBalance_succeeds() {
        when(accountRepository.findByUsername("jan")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bankService.withdraw("jan", 5000.0, "jan");

        assertEquals(0.0, testAccount.getBalance(), 0.01);
    }

    @Test
    void withdraw_zeroAmount_throwsException() {
        assertThrows(RuntimeException.class,
                () -> bankService.withdraw("jan", 0, "jan"));
    }

    @Test
    void withdraw_negativeAmount_throwsException() {
        assertThrows(RuntimeException.class,
                () -> bankService.withdraw("jan", -50, "jan"));
    }

    @Test
    void withdraw_nonExistingUser_throwsException() {
        when(accountRepository.findByUsername("nieznany")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> bankService.withdraw("nieznany", 100, "bankier"));
    }

    // --- getTransactions ---

    @Test
    void getTransactions_returnsListForUser() {
        Transaction t = new Transaction();
        t.setUsername("jan");
        t.setType(Transaction.Type.DEPOSIT);
        t.setCreatedAt(LocalDateTime.now());
        when(transactionRepository.findByUsername("jan")).thenReturn(List.of(t));

        List<Transaction> result = bankService.getTransactions("jan");

        assertEquals(1, result.size());
        assertEquals("jan", result.getFirst().getUsername());
        verify(transactionRepository).findByUsername("jan");
    }

    @Test
    void getTransactions_emptyForNewUser() {
        when(transactionRepository.findByUsername("anna")).thenReturn(List.of());

        List<Transaction> result = bankService.getTransactions("anna");

        assertTrue(result.isEmpty());
    }

    // --- getAllTransactions ---

    @Test
    void getAllTransactions_returnsAll() {
        Transaction t1 = new Transaction();
        t1.setUsername("jan");
        Transaction t2 = new Transaction();
        t2.setUsername("anna");
        when(transactionRepository.findAll()).thenReturn(List.of(t1, t2));

        List<Transaction> result = bankService.getAllTransactions();

        assertEquals(2, result.size());
        verify(transactionRepository).findAll();
    }
}
