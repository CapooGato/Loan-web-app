# HELP - Dokumentacja endpointow aplikacji bankowej

## Spis tresci

1. [Architektura i bezpieczenstwo](#architektura-i-bezpieczenstwo)
2. [Konta i role](#konta-i-role)
3. [Endpointy publiczne (HomeController)](#endpointy-publiczne---homecontroller)
4. [Endpointy uzytkownika (UserController)](#endpointy-uzytkownika---usercontroller)
5. [Endpointy bankiera (BankierController)](#endpointy-bankiera---bankiercontroller)
6. [Endpointy menadzera (MenadzerController)](#endpointy-menadzera---menadzercontroller)
7. [Obsluga bledow (GlobalExceptionHandler)](#obsluga-bledow---globalexceptionhandler)
8. [Testy](#testy)

---

## Architektura i bezpieczenstwo

Aplikacja uzywa **Spring Security** z dwoma warstwami kontroli dostepu:

1. **SecurityFilterChain** (`SecurityConfig.java`) - filtr na poziomie HTTP:
   - `/login` i `/css/**` - dostep publiczny (`permitAll`)
   - Kazdy inny request wymaga uwierzytelnienia (`anyRequest().authenticated()`)
   - CSRF wlaczone - kazdy POST musi zawierac token CSRF
   - Formularz logowania pod `/login`, po wylogowaniu redirect na `/login?logout`

2. **@PreAuthorize** na kontrolerach - autoryzacja na poziomie metod:
   - Wlaczona przez `@EnableMethodSecurity` w `SecurityConfig`
   - Kazdy kontroler ma adnotacje `@PreAuthorize("hasRole('...')")` na poziomie klasy
   - Proba dostepu z nieprawidlowa rola zwraca **HTTP 403 Forbidden**

Uzytkownicy i hasla sa zdefiniowani w `application.yml` i ladowani do `InMemoryUserDetailsManager` przez `AppUsersConfig` + `SecurityConfig.userDetailsService()`. Hasla przechowywane z prefixem `{noop}` (bez szyfrowania - srodowisko laboratoryjne).

---

## Konta i role

Zdefiniowane w `application.yml`:

| Login      | Haslo        | Rola      | Saldo poczatkowe |
|------------|--------------|-----------|------------------|
| `jan`      | `jan123`     | USER      | 5000.00 PLN      |
| `anna`     | `anna123`    | USER      | 10000.00 PLN     |
| `bankier`  | `bank123`    | BANKIER   | -                |
| `menadzer` | `menadzer123`| MENADZER  | -                |

Konta bankowe (z saldem) tworzone sa automatycznie przy starcie aplikacji przez `DataInitializer` tylko dla uzytkownikow z rola USER.

---

## Endpointy publiczne - HomeController

Plik: `controller/HomeController.java`
Brak adnotacji `@PreAuthorize` - dostep regulowany przez `SecurityFilterChain`.

### GET /

| Pole          | Wartosc                                                        |
|---------------|----------------------------------------------------------------|
| **Cel**       | Przekierowanie na odpowiedni dashboard na podstawie roli       |
| **Autoryzacja** | Kazdy uwierzytelniony uzytkownik                             |
| **Parametry** | Brak                                                           |
| **Zwraca**    | HTTP 302 redirect na `/user/dashboard`, `/bankier/dashboard` lub `/menadzer/dashboard` w zaleznosci od roli. Niezalogowany -> redirect na `/login` |

**Logika:** Kontroler odczytuje `Authentication.getAuthorities()`, sprawdza obecnosc `ROLE_MENADZER`, `ROLE_BANKIER` lub domyslnie kieruje na `/user/dashboard`.

**Testy** (`HomeControllerTest`):
- `root_unauthenticated_redirectsToLogin` - brak sesji -> redirect `/login`
- `root_userRole_redirectsToUserDashboard` - `@WithMockUser(roles="USER")` -> redirect `/user/dashboard`
- `root_bankierRole_redirectsToBankierDashboard` - `@WithMockUser(roles="BANKIER")` -> redirect `/bankier/dashboard`
- `root_menadzerRole_redirectsToMenadzerDashboard` - `@WithMockUser(roles="MENADZER")` -> redirect `/menadzer/dashboard`

### GET /login

| Pole          | Wartosc                                                        |
|---------------|----------------------------------------------------------------|
| **Cel**       | Wyswietlenie formularza logowania                              |
| **Autoryzacja** | Publiczny (`permitAll` w SecurityFilterChain)                |
| **Parametry** | Opcjonalne query params: `?error`, `?logout`, `?denied`        |
| **Zwraca**    | HTTP 200, widok `login.html`                                   |

**Testy** (`HomeControllerTest`):
- `login_unauthenticated_returns200` - strona logowania dostepna bez sesji

---

## Endpointy uzytkownika - UserController

Plik: `controller/UserController.java`
Adnotacja klasy: **`@PreAuthorize("hasRole('USER')")`**
Prefix: `/user`

### GET /user/dashboard

| Pole          | Wartosc                                                        |
|---------------|----------------------------------------------------------------|
| **Cel**       | Panel glowny uzytkownika - saldo, pozyczki, historia transakcji |
| **Autoryzacja** | Tylko rola **USER**. BANKIER/MENADZER -> 403                |
| **Parametry** | Brak (username pobierany z `Principal.getName()`)              |
| **Model**     | `account` (BankAccount), `transactions` (List\<Transaction\>), `loans` (List\<Loan\>) |
| **Zwraca**    | HTTP 200, widok `user/dashboard.html`                          |

**Logika:** Pobiera dane zalogowanego uzytkownika z `BankService.getAccount()`, `BankService.getTransactions()` i `LoanService.getLoansByBorrower()`.

**Testy** (`UserControllerTest`):
- `dashboard_unauthenticated_redirectsToLogin` - brak sesji -> redirect `/login`
- `dashboard_userRole_returns200` - `@WithMockUser(roles="USER")` -> 200, sprawdza `view()` i `model().attributeExists()`
- `dashboard_bankierRole_accessDenied` - `@WithMockUser(roles="BANKIER")` -> 403
- `dashboard_menadzerRole_accessDenied` - `@WithMockUser(roles="MENADZER")` -> 403

### POST /user/deposit

| Pole          | Wartosc                                                        |
|---------------|----------------------------------------------------------------|
| **Cel**       | Wplata srodkow na wlasne konto                                |
| **Autoryzacja** | Tylko rola **USER** + token CSRF                             |
| **Parametry** | `amount` (double, form param) - kwota do wplaty (> 0)         |
| **Zwraca**    | HTTP 302 redirect na `/user/dashboard` z flash attribute `success` lub `error` |

**Logika:** Wywoluje `BankService.deposit(username, amount, username)`. Pole `performedBy` ustawione na username zalogowanego uzytkownika (sam sobie wplaca). Tworzy obiekt `Transaction` typu DEPOSIT.

**Testy** (`UserControllerTest`):
- `deposit_userRole_redirectsToDashboard` - poprawna wplata z `.with(csrf())` -> redirect + flash `success`
- `deposit_bankierRole_accessDenied` - BANKIER -> 403
- `deposit_unauthenticated_redirectsToLogin` - brak sesji -> redirect `/login`
- `deposit_withoutCsrf_forbidden` - brak tokenu CSRF -> 403

**Testy logiki biznesowej** (`BankServiceTest`, Mockito):
- `deposit_validAmount_increasesBalance` - saldo rosnie o wplacona kwote
- `deposit_createsTransaction` - `ArgumentCaptor` weryfikuje typ DEPOSIT, kwote, performedBy
- `deposit_zeroAmount_throwsException` - kwota 0 -> RuntimeException
- `deposit_negativeAmount_throwsException` - kwota ujemna -> RuntimeException
- `deposit_nonExistingUser_throwsException` - nieistniejacy user -> RuntimeException

### POST /user/withdraw

| Pole          | Wartosc                                                        |
|---------------|----------------------------------------------------------------|
| **Cel**       | Wyplata srodkow z wlasnego konta                              |
| **Autoryzacja** | Tylko rola **USER** + token CSRF                             |
| **Parametry** | `amount` (double, form param) - kwota do wyplaty (> 0, <= saldo) |
| **Zwraca**    | HTTP 302 redirect na `/user/dashboard` z flash attribute `success` lub `error` |

**Logika:** Wywoluje `BankService.withdraw(username, amount, username)`. Sprawdza czy saldo >= kwota, jesli nie -> RuntimeException. Tworzy obiekt `Transaction` typu WITHDRAWAL.

**Testy** (`UserControllerTest`):
- `withdraw_userRole_redirectsToDashboard` - poprawna wyplata -> redirect
- `withdraw_bankierRole_accessDenied` - BANKIER -> 403
- `withdraw_insufficientFunds_redirectsWithError` - kwota 9999999 -> redirect + flash `error`

**Testy logiki biznesowej** (`BankServiceTest`, Mockito):
- `withdraw_validAmount_decreasesBalance` - saldo maleje
- `withdraw_createsTransaction` - `ArgumentCaptor` weryfikuje typ WITHDRAWAL
- `withdraw_insufficientFunds_throwsException` - brak srodkow -> RuntimeException, `verify(accountRepository, never()).save()`
- `withdraw_exactBalance_succeeds` - wyplata calego salda -> saldo = 0
- `withdraw_zeroAmount_throwsException` - kwota 0 -> RuntimeException
- `withdraw_negativeAmount_throwsException` - kwota ujemna -> RuntimeException
- `withdraw_nonExistingUser_throwsException` - nieistniejacy user -> RuntimeException

---

## Endpointy bankiera - BankierController

Plik: `controller/BankierController.java`
Adnotacja klasy: **`@PreAuthorize("hasRole('BANKIER')")`**
Prefix: `/bankier`

### GET /bankier/dashboard

| Pole          | Wartosc                                                        |
|---------------|----------------------------------------------------------------|
| **Cel**       | Panel bankiera - lista kont klientow i wszystkich pozyczek     |
| **Autoryzacja** | Tylko rola **BANKIER**. USER/MENADZER -> 403                 |
| **Parametry** | Brak                                                           |
| **Model**     | `accounts` (List\<BankAccount\>), `loans` (List\<Loan\>)      |
| **Zwraca**    | HTTP 200, widok `bankier/dashboard.html`                       |

**Logika:** Pobiera wszystkie konta przez `BankService.getAllAccounts()` i wszystkie pozyczki przez `LoanService.getAllLoans()`. Konta uzywane sa tez do wypelnienia dropdownow w formularzach.

**Testy** (`BankierControllerTest`):
- `dashboard_unauthenticated_redirectsToLogin` - brak sesji -> redirect `/login`
- `dashboard_bankierRole_returns200` - `@WithMockUser(roles="BANKIER")` -> 200, `model().attributeExists("accounts", "loans")`
- `dashboard_userRole_accessDenied` - USER -> 403
- `dashboard_menadzerRole_accessDenied` - MENADZER -> 403

### POST /bankier/deposit

| Pole          | Wartosc                                                        |
|---------------|----------------------------------------------------------------|
| **Cel**       | Wplata srodkow na konto wybranego klienta                     |
| **Autoryzacja** | Tylko rola **BANKIER** + token CSRF                          |
| **Parametry** | `username` (String) - login klienta, `amount` (double) - kwota |
| **Zwraca**    | HTTP 302 redirect na `/bankier/dashboard` z flash `success`/`error` |

**Logika:** Wywoluje `BankService.deposit(username, amount, principal.getName())`. Pole `performedBy` ustawione na login bankiera (widoczne w historii transakcji klienta).

**Testy** (`BankierControllerTest`):
- `deposit_bankierRole_redirectsToDashboard` - poprawna wplata -> redirect + flash `success`
- `deposit_userRole_accessDenied` - USER -> 403

### POST /bankier/withdraw

| Pole          | Wartosc                                                        |
|---------------|----------------------------------------------------------------|
| **Cel**       | Wyplata srodkow z konta wybranego klienta                     |
| **Autoryzacja** | Tylko rola **BANKIER** + token CSRF                          |
| **Parametry** | `username` (String) - login klienta, `amount` (double) - kwota |
| **Zwraca**    | HTTP 302 redirect na `/bankier/dashboard` z flash `success`/`error` |

**Logika:** Wywoluje `BankService.withdraw(username, amount, principal.getName())`. Walidacja salda identyczna jak w wyplacie uzytkownika.

**Testy** (`BankierControllerTest`):
- `withdraw_bankierRole_redirectsToDashboard` - poprawna wyplata -> redirect
- `withdraw_userRole_accessDenied` - USER -> 403

### POST /bankier/grant-loan

| Pole          | Wartosc                                                        |
|---------------|----------------------------------------------------------------|
| **Cel**       | Udzielenie pozyczki wybranemu klientowi                        |
| **Autoryzacja** | Tylko rola **BANKIER** + token CSRF                          |
| **Parametry** | `username` (String) - login klienta, `amount` (double) - kwota pozyczki, `interestRate` (double) - oprocentowanie roczne w %, `termMonths` (int) - okres w miesiacach |
| **Zwraca**    | HTTP 302 redirect na `/bankier/dashboard` z flash `success`/`error` |

**Logika:** Wywoluje `LoanService.grantLoan(username, amount, interestRate, termMonths, principal.getName())`. Serwis:
1. Tworzy obiekt `Loan` z UUID, statusem ACTIVE i data utworzenia
2. Oblicza rate miesieczna wzorem PMT: `M = P * (r * (1+r)^n) / ((1+r)^n - 1)` (dla oprocentowania 0% rata = kwota / liczba miesiecy)
3. Oblicza laczna kwote do splaty i odsetki
4. Wplaca kwote pozyczki na konto klienta (`BankService.deposit`)
5. Zapisuje pozyczke w `LoanRepository`

**Testy** (`BankierControllerTest`):
- `grantLoan_bankierRole_redirectsToDashboard` - poprawne udzielenie -> redirect + flash `success`
- `grantLoan_userRole_accessDenied` - USER -> 403
- `grantLoan_menadzerRole_accessDenied` - MENADZER -> 403
- `grantLoan_withoutCsrf_forbidden` - brak CSRF -> 403

**Testy logiki biznesowej** (`LoanServiceTest`, Mockito):
- `grantLoan_createsLoanWithCorrectFields` - weryfikacja pol: borrowerUsername, amount, rate, status, grantedBy, id, createdAt
- `grantLoan_calculatesMonthlyPayment` - 100000 PLN, 5%, 120 mies. -> rata 1060.66 PLN
- `grantLoan_calculatesTotalAmount` - laczna kwota ~127279.20 PLN
- `grantLoan_calculatesTotalInterest` - odsetki ~27279.20 PLN
- `grantLoan_depositsAmountToBorrowerAccount` - `verify(bankService).deposit(...)` potwierdza wplate na konto
- `grantLoan_savesToRepository` - `ArgumentCaptor<Loan>` weryfikuje zapis
- `grantLoan_zeroInterestRate_calculatesEqualPayments` - 12000/12 = 1000 PLN raty, 0 odsetek
- `grantLoan_variousInputs_correctMonthlyPayment` - **@ParameterizedTest @CsvSource** z 5 zestawami danych

---

## Endpointy menadzera - MenadzerController

Plik: `controller/MenadzerController.java`
Adnotacja klasy: **`@PreAuthorize("hasRole('MENADZER')")`**
Prefix: `/menadzer`

### GET /menadzer/dashboard

| Pole          | Wartosc                                                        |
|---------------|----------------------------------------------------------------|
| **Cel**       | Panel menadzera - historia wszystkich pozyczek w systemie      |
| **Autoryzacja** | Tylko rola **MENADZER**. USER/BANKIER -> 403                 |
| **Parametry** | Brak                                                           |
| **Model**     | `loans` (List\<Loan\>)                                        |
| **Zwraca**    | HTTP 200, widok `menadzer/dashboard.html`                      |

**Logika:** Pobiera wszystkie pozyczki przez `LoanService.getAllLoans()`. Widok wyswietla tabele z pelna historia: ID, klient, kwota, oprocentowanie, rata, lacznie, odsetki, status, kto udzielil, data. Kazdy wiersz zawiera formularz do zmiany oprocentowania.

**Testy** (`MenadzerControllerTest`):
- `dashboard_unauthenticated_redirectsToLogin` - brak sesji -> redirect `/login`
- `dashboard_menadzerRole_returns200` - `@WithMockUser(roles="MENADZER")` -> 200, `model().attributeExists("loans")`
- `dashboard_userRole_accessDenied` - USER -> 403
- `dashboard_bankierRole_accessDenied` - BANKIER -> 403

### POST /menadzer/change-rate

| Pole          | Wartosc                                                        |
|---------------|----------------------------------------------------------------|
| **Cel**       | Zmiana oprocentowania istniejace pozyczki                      |
| **Autoryzacja** | Tylko rola **MENADZER** + token CSRF                         |
| **Parametry** | `loanId` (String) - UUID pozyczki, `newRate` (double) - nowe oprocentowanie roczne w % |
| **Zwraca**    | HTTP 302 redirect na `/menadzer/dashboard` z flash `success`/`error` |

**Logika:** Wywoluje `LoanService.changeInterestRate(loanId, newRate)`. Serwis:
1. Wyszukuje pozyczke w `LoanRepository.findById()` - jesli brak, rzuca `LoanNotFoundException`
2. Ustawia nowe oprocentowanie
3. Przelicza rate miesieczna, laczna kwote i odsetki wzorem PMT
4. Zapisuje zaktualizowana pozyczke

**Testy** (`MenadzerControllerTest`):
- `changeRate_menadzerRole_existingLoan_redirectsWithSuccess` - wstawia Loan do repo, zmienia oprocentowanie -> redirect + flash `success`
- `changeRate_nonExistingLoan_redirectsWithError` - nieistniejace ID -> redirect + flash `error`
- `changeRate_userRole_accessDenied` - USER -> 403
- `changeRate_bankierRole_accessDenied` - BANKIER -> 403
- `changeRate_withoutCsrf_forbidden` - brak CSRF -> 403

**Testy logiki biznesowej** (`LoanServiceTest`, Mockito):
- `changeInterestRate_existingLoan_updatesRate` - weryfikacja nowej stawki
- `changeInterestRate_recalculatesPayments` - 100000 PLN, zmiana z 5% na 7% -> rata 1161.08 PLN
- `changeInterestRate_savesToRepository` - `verify(loanRepository).save(loan)`
- `changeInterestRate_nonExistingLoan_throwsLoanNotFoundException` - `assertThrows(LoanNotFoundException.class)`
- `changeInterestRate_toZero_calculatesEqualPayments` - oprocentowanie 0% -> rata = kwota/okres

---

## Obsluga bledow - GlobalExceptionHandler

Plik: `exception/GlobalExceptionHandler.java`
Adnotacja: `@ControllerAdvice`

| Wyjatek                | Obsluga                                                        |
|------------------------|----------------------------------------------------------------|
| `AccessDeniedException`| Re-throw - Spring Security zwraca HTTP 403                     |
| `LoanNotFoundException`| Widok `error.html` z komunikatem "Nie znaleziono oferty kredytowej o ID: ..." |
| `RuntimeException`     | Widok `error.html` z komunikatem bledu (np. "Niewystarczajace srodki na koncie") |

Kolejnosc handlerow ma znaczenie - `AccessDeniedException` (dziedziczacy po `RuntimeException`) musi byc obsluzony PRZED ogolnym `RuntimeException`, zeby Spring Security mogl zwrocic 403 zamiast renderowac strone bledu z kodem 200.

---

## Testy

### Testy integracyjne kontrolerow (MockMvc + Spring Security)

Uzycie: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@WithMockUser`

| Klasa testowa             | Plik                                         | Liczba testow |
|---------------------------|----------------------------------------------|---------------|
| `HomeControllerTest`      | `test/.../controller/HomeControllerTest.java` | 5             |
| `UserControllerTest`      | `test/.../controller/UserControllerTest.java` | 11            |
| `BankierControllerTest`   | `test/.../controller/BankierControllerTest.java` | 12         |
| `MenadzerControllerTest`  | `test/.../controller/MenadzerControllerTest.java` | 9          |

Co testuja:
- **Autoryzacja** - `@WithMockUser(roles="...")` + `status().isForbidden()` weryfikuje ze `@PreAuthorize` na kontrolerze blokuje nieuprawnionych
- **Uwierzytelnienie** - requesty bez sesji -> redirect na `/login`
- **CSRF** - requesty POST bez `.with(csrf())` -> 403
- **Happy path** - poprawne dane + poprawna rola -> redirect z flash `success`
- **Bledy biznesowe** - np. brak srodkow -> redirect z flash `error`
- **Widoki i model** - `view().name(...)` + `model().attributeExists(...)` sprawdzaja ze kontroler zwraca wlasciwy szablon z danymi

### Testy jednostkowe serwisow (Mockito)

Uzycie: `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`

| Klasa testowa       | Plik                                       | Liczba testow |
|---------------------|--------------------------------------------|---------------|
| `BankServiceTest`   | `test/.../service/BankServiceTest.java`    | 19            |
| `LoanServiceTest`   | `test/.../service/LoanServiceTest.java`    | 21            |

Co testuja:
- **Izolacja** - repozytoria mockowane przez `@Mock`, serwisy tworzone przez `@InjectMocks`
- **Weryfikacja interakcji** - `verify()`, `verify(..., never())` sprawdzaja czy serwis wolal odpowiednie metody repo
- **ArgumentCaptor** - przechwytuje obiekty przekazane do `repository.save()` i weryfikuje ich pola
- **Wyjatki** - `assertThrows()` dla blednych danych, brakujacych rekordow
- **@ParameterizedTest @CsvSource** - 5 zestawow danych do weryfikacji kalkulacji PMT

### Uruchomienie

```bash
./mvnw test
```

Laczna liczba testow: **77** (37 integracyjnych + 40 jednostkowych)
