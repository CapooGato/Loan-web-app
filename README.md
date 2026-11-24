# HELP - Banking Application Endpoints Documentation

## Table of Contents

1. [Architecture and Security](#architecture-and-security)
2. [Accounts and Roles](#accounts-and-roles)
3. [Public Endpoints (HomeController)](#public-endpoints---homecontroller)
4. [User Endpoints (UserController)](#user-endpoints---usercontroller)
5. [Banker Endpoints (BankierController)](#banker-endpoints---bankiercontroller)
6. [Manager Endpoints (MenadzerController)](#manager-endpoints---menadzercontroller)
7. [Error Handling (GlobalExceptionHandler)](#error-handling---globalexceptionhandler)
8. [Tests](#tests)

---

## Architecture and Security

The application uses **Spring Security** with two access control layers:

1. **SecurityFilterChain** (`SecurityConfig.java`) - HTTP-level filter:
   - `/login` and `/css/**` - public access (`permitAll`)
   - Any other request requires authentication (`anyRequest().authenticated()`)
   - CSRF enabled - every POST request must contain a CSRF token
   - Login form located at `/login`, redirect to `/login?logout` after logging out

2. **@PreAuthorize** on controllers - method-level authorization:
   - Enabled via `@EnableMethodSecurity` in `SecurityConfig`
   - Every controller has `@PreAuthorize("hasRole('...')")` annotation at the class level
   - Attempting access with an invalid role returns **HTTP 403 Forbidden**

Users and passwords are defined in `application.yml` and loaded into `InMemoryUserDetailsManager` via `AppUsersConfig` + `SecurityConfig.userDetailsService()`. Passwords are stored with the `{noop}` prefix (no encoding - laboratory/development environment).

---

## Accounts and Roles

Defined in `application.yml`:

| Username   | Password     | Role     | Initial Balance |
|------------|--------------|----------|-----------------|
| `jan`      | `jan123`     | USER     | 5000.00 PLN     |
| `anna`     | `anna123`    | USER     | 10000.00 PLN    |
| `bankier`  | `bank123`    | BANKIER  | -               |
| `menadzer` | `menadzer123`| MENADZER | -               |

Bank accounts (with balance) are automatically created at application startup by `DataInitializer` only for users with the `USER` role.

---

## Public Endpoints - HomeController

File: `controller/HomeController.java`  
No `@PreAuthorize` annotation - access regulated by `SecurityFilterChain`.

### GET /

| Field             | Value                                                                                                                                           |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| **Purpose**       | Redirect to the appropriate dashboard based on user role                                                                                        |
| **Authorization** | Any authenticated user                                                                                                                          |
| **Parameters**    | None                                                                                                                                            |
| **Returns**       | HTTP 302 redirect to `/user/dashboard`, `/bankier/dashboard`, or `/menadzer/dashboard` depending on role. Unauthenticated -> redirect to `/login` |

**Logic:** The controller reads `Authentication.getAuthorities()`, checks for the presence of `ROLE_MENADZER`, `ROLE_BANKIER`, or defaults to `/user/dashboard`.

**Tests** (`HomeControllerTest`):
- `root_unauthenticated_redirectsToLogin` - no session -> redirect `/login`
- `root_userRole_redirectsToUserDashboard` - `@WithMockUser(roles="USER")` -> redirect `/user/dashboard`
- `root_bankierRole_redirectsToBankierDashboard` - `@WithMockUser(roles="BANKIER")` -> redirect `/bankier/dashboard`
- `root_menadzerRole_redirectsToMenadzerDashboard` - `@WithMockUser(roles="MENADZER")` -> redirect `/menadzer/dashboard`

### GET /login

| Field             | Value                                                    |
|-------------------|----------------------------------------------------------|
| **Purpose**       | Display the login form                                   |
| **Authorization** | Public (`permitAll` in SecurityFilterChain)              |
| **Parameters**    | Optional query params: `?error`, `?logout`, `?denied`    |
| **Returns**       | HTTP 200, view `login.html`                              |

**Tests** (`HomeControllerTest`):
- `login_unauthenticated_returns200` - login page accessible without a session

---

## User Endpoints - UserController

File: `controller/UserController.java`  
Class Annotation: **`@PreAuthorize("hasRole('USER')")`**  
Prefix: `/user`

### GET /user/dashboard

| Field             | Value                                                                                 |
|-------------------|---------------------------------------------------------------------------------------|
| **Purpose**       | User main dashboard - balance, loans, transaction history                             |
| **Authorization** | Only **USER** role. BANKIER/MENADZER -> 403                                           |
| **Parameters**    | None (username retrieved from `Principal.getName()`)                                  |
| **Model**         | `account` (BankAccount), `transactions` (List\<Transaction\>), `loans` (List\<Loan\>) |
| **Returns**       | HTTP 200, view `user/dashboard.html`                                                  |

**Logic:** Retrieves authenticated user's data from `BankService.getAccount()`, `BankService.getTransactions()`, and `LoanService.getLoansByBorrower()`.

**Tests** (`UserControllerTest`):
- `dashboard_unauthenticated_redirectsToLogin` - no session -> redirect `/login`
- `dashboard_userRole_returns200` - `@WithMockUser(roles="USER")` -> 200, verifies `view()` and `model().attributeExists()`
- `dashboard_bankierRole_accessDenied` - `@WithMockUser(roles="BANKIER")` -> 403
- `dashboard_menadzerRole_accessDenied` - `@WithMockUser(roles="MENADZER")` -> 403

### POST /user/deposit

| Field             | Value                                                                           |
|-------------------|---------------------------------------------------------------------------------|
| **Purpose**       | Deposit funds into user's own account                                           |
| **Authorization** | Only **USER** role + CSRF token                                                 |
| **Parameters**    | `amount` (double, form param) - deposit amount (> 0)                            |
| **Returns**       | HTTP 302 redirect to `/user/dashboard` with flash attribute `success` or `error` |

**Logic:** Invokes `BankService.deposit(username, amount, username)`. The `performedBy` field is set to the authenticated user's username (self-deposit). Creates a `Transaction` object of type `DEPOSIT`.

**Tests** (`UserControllerTest`):
- `deposit_userRole_redirectsToDashboard` - valid deposit with `.with(csrf())` -> redirect + flash `success`
- `deposit_bankierRole_accessDenied` - BANKIER -> 403
- `deposit_unauthenticated_redirectsToLogin` - no session -> redirect `/login`
- `deposit_withoutCsrf_forbidden` - missing CSRF token -> 403

**Business Logic Tests** (`BankServiceTest`, Mockito):
- `deposit_validAmount_increasesBalance` - balance increases by the deposited amount
- `deposit_createsTransaction` - `ArgumentCaptor` verifies DEPOSIT type, amount, and performedBy
- `deposit_zeroAmount_throwsException` - amount 0 -> RuntimeException
- `deposit_negativeAmount_throwsException` - negative amount -> RuntimeException
- `deposit_nonExistingUser_throwsException` - non-existent user -> RuntimeException

### POST /user/withdraw

| Field             | Value                                                                           |
|-------------------|---------------------------------------------------------------------------------|
| **Purpose**       | Withdraw funds from user's own account                                          |
| **Authorization** | Only **USER** role + CSRF token                                                 |
| **Parameters**    | `amount` (double, form param) - withdrawal amount (> 0, <= balance)             |
| **Returns**       | HTTP 302 redirect to `/user/dashboard` with flash attribute `success` or `error` |

**Logic:** Invokes `BankService.withdraw(username, amount, username)`. Checks if balance >= amount; if not -> RuntimeException. Creates a `Transaction` object of type `WITHDRAWAL`.

**Tests** (`UserControllerTest`):
- `withdraw_userRole_redirectsToDashboard` - valid withdrawal -> redirect
- `withdraw_bankierRole_accessDenied` - BANKIER -> 403
- `withdraw_insufficientFunds_redirectsWithError` - amount 9999999 -> redirect + flash `error`

**Business Logic Tests** (`BankServiceTest`, Mockito):
- `withdraw_validAmount_decreasesBalance` - balance decreases
- `withdraw_createsTransaction` - `ArgumentCaptor` verifies WITHDRAWAL type
- `withdraw_insufficientFunds_throwsException` - insufficient funds -> RuntimeException, `verify(accountRepository, never()).save()`
- `withdraw_exactBalance_succeeds` - full balance withdrawal -> balance = 0
- `withdraw_zeroAmount_throwsException` - amount 0 -> RuntimeException
- `withdraw_negativeAmount_throwsException` - negative amount -> RuntimeException
- `withdraw_nonExistingUser_throwsException` - non-existent user -> RuntimeException

---

## Banker Endpoints - BankierController

File: `controller/BankierController.java`  
Class Annotation: **`@PreAuthorize("hasRole('BANKIER')")`**  
Prefix: `/bankier`

### GET /bankier/dashboard

| Field             | Value                                                                        |
|-------------------|------------------------------------------------------------------------------|
| **Purpose**       | Banker dashboard - list of customer accounts and all loans                   |
| **Authorization** | Only **BANKIER** role. USER/MENADZER -> 403                                  |
| **Parameters**    | None                                                                         |
| **Model**         | `accounts` (List\<BankAccount\>), `loans` (List\<Loan\>)                     |
| **Returns**       | HTTP 200, view `bankier/dashboard.html`                                      |

**Logic:** Retrieves all accounts via `BankService.getAllAccounts()` and all loans via `LoanService.getAllLoans()`. Accounts are also used to populate dropdowns in forms.

**Tests** (`BankierControllerTest`):
- `dashboard_unauthenticated_redirectsToLogin` - no session -> redirect `/login`
- `dashboard_bankierRole_returns200` - `@WithMockUser(roles="BANKIER")` -> 200, `model().attributeExists("accounts", "loans")`
- `dashboard_userRole_accessDenied` - USER -> 403
- `dashboard_menadzerRole_accessDenied` - MENADZER -> 403

### POST /bankier/deposit

| Field             | Value                                                                              |
|-------------------|------------------------------------------------------------------------------------|
| **Purpose**       | Deposit funds into a selected customer's account                                   |
| **Authorization** | Only **BANKIER** role + CSRF token                                                 |
| **Parameters**    | `username` (String) - customer login, `amount` (double) - amount                   |
| **Returns**       | HTTP 302 redirect to `/bankier/dashboard` with flash attribute `success`/`error`  |

**Logic:** Invokes `BankService.deposit(username, amount, principal.getName())`. The `performedBy` field is set to the banker's username (visible in the customer's transaction history).

**Tests** (`BankierControllerTest`):
- `deposit_bankierRole_redirectsToDashboard` - valid deposit -> redirect + flash `success`
- `deposit_userRole_accessDenied` - USER -> 403

### POST /bankier/withdraw

| Field             | Value                                                                              |
|-------------------|------------------------------------------------------------------------------------|
| **Purpose**       | Withdraw funds from a selected customer's account                                  |
| **Authorization** | Only **BANKIER** role + CSRF token                                                 |
| **Parameters**    | `username` (String) - customer login, `amount` (double) - amount                   |
| **Returns**       | HTTP 302 redirect to `/bankier/dashboard` with flash attribute `success`/`error`  |

**Logic:** Invokes `BankService.withdraw(username, amount, principal.getName())`. Balance validation is identical to user withdrawal.

**Tests** (`BankierControllerTest`):
- `withdraw_bankierRole_redirectsToDashboard` - valid withdrawal -> redirect
- `withdraw_userRole_accessDenied` - USER -> 403

### POST /bankier/grant-loan

| Field             | Value                                                                                                                                                                          |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Purpose**       | Grant a loan to a selected customer                                                                                                                                            |
| **Authorization** | Only **BANKIER** role + CSRF token                                                                                                                                             |
| **Parameters**    | `username` (String) - customer login, `amount` (double) - loan amount, `interestRate` (double) - annual interest rate in %, `termMonths` (int) - duration in months             |
| **Returns**       | HTTP 302 redirect to `/bankier/dashboard` with flash attribute `success`/`error`                                                                                               |

**Logic:** Invokes `LoanService.grantLoan(username, amount, interestRate, termMonths, principal.getName())`. Service steps:
1. Creates a `Loan` object with UUID, status ACTIVE, and creation timestamp.
2. Calculates monthly payment using the PMT formula: $M = P \cdot \frac{r(1+r)^n}{(1+r)^n - 1}$ (for 0% interest rate, payment = amount / termMonths).
3. Calculates total repayment amount and total interest.
4. Deposits the loan amount into the customer's account (`BankService.deposit`).
5. Saves the loan into `LoanRepository`.

**Tests** (`BankierControllerTest`):
- `grantLoan_bankierRole_redirectsToDashboard` - successful grant -> redirect + flash `success`
- `grantLoan_userRole_accessDenied` - USER -> 403
- `grantLoan_menadzerRole_accessDenied` - MENADZER -> 403
- `grantLoan_withoutCsrf_forbidden` - missing CSRF -> 403

**Business Logic Tests** (`LoanServiceTest`, Mockito):
- `grantLoan_createsLoanWithCorrectFields` - field verification: borrowerUsername, amount, rate, status, grantedBy, id, createdAt
- `grantLoan_calculatesMonthlyPayment` - 100,000 PLN, 5%, 120 mos. -> 1,060.66 PLN monthly payment
- `grantLoan_calculatesTotalAmount` - total amount ~127,279.20 PLN
- `grantLoan_calculatesTotalInterest` - interest ~27,279.20 PLN
- `grantLoan_depositsAmountToBorrowerAccount` - `verify(bankService).deposit(...)` confirms account deposit
- `grantLoan_savesToRepository` - `ArgumentCaptor<Loan>` verifies saving
- `grantLoan_zeroInterestRate_calculatesEqualPayments` - 12,000 / 12 = 1,000 PLN monthly payment, 0 interest
- `grantLoan_variousInputs_correctMonthlyPayment` - **@ParameterizedTest @CsvSource** with 5 datasets

---

## Manager Endpoints - MenadzerController

File: `controller/MenadzerController.java`  
Class Annotation: **`@PreAuthorize("hasRole('MENADZER')")`**  
Prefix: `/menadzer`

### GET /menadzer/dashboard

| Field             | Value                                                                        |
|-------------------|------------------------------------------------------------------------------|
| **Purpose**       | Manager dashboard - full history of all loans in the system                  |
| **Authorization** | Only **MENADZER** role. USER/BANKIER -> 403                                  |
| **Parameters**    | None                                                                         |
| **Model**         | `loans` (List\<Loan\>)                                                       |
| **Returns**       | HTTP 200, view `menadzer/dashboard.html`                                     |

**Logic:** Retrieves all loans via `LoanService.getAllLoans()`. View displays a table with complete history: ID, customer, amount, interest rate, monthly payment, total, interest, status, granted by, date. Each row contains a form to modify the interest rate.

**Tests** (`MenadzerControllerTest`):
- `dashboard_unauthenticated_redirectsToLogin` - no session -> redirect `/login`
- `dashboard_menadzerRole_returns200` - `@WithMockUser(roles="MENADZER")` -> 200, `model().attributeExists("loans")`
- `dashboard_userRole_accessDenied` - USER -> 403
- `dashboard_bankierRole_accessDenied` - BANKIER -> 403

### POST /menadzer/change-rate

| Field             | Value                                                                              |
|-------------------|------------------------------------------------------------------------------------|
| **Purpose**       | Update interest rate of an existing loan                                           |
| **Authorization** | Only **MENADZER** role + CSRF token                                                |
| **Parameters**    | `loanId` (String) - loan UUID, `newRate` (double) - new annual interest rate in %   |
| **Returns**       | HTTP 302 redirect to `/menadzer/dashboard` with flash attribute `success`/`error`  |

**Logic:** Invokes `LoanService.changeInterestRate(loanId, newRate)`. Service steps:
1. Searches for the loan using `LoanRepository.findById()` - throws `LoanNotFoundException` if missing.
2. Sets the new interest rate.
3. Recalculates monthly payment, total amount, and interest using the PMT formula.
4. Saves the updated loan.

**Tests** (`MenadzerControllerTest`):
- `changeRate_menadzerRole_existingLoan_redirectsWithSuccess` - inserts Loan into repo, changes rate -> redirect + flash `success`
- `changeRate_nonExistingLoan_redirectsWithError` - non-existent ID -> redirect + flash `error`
- `changeRate_userRole_accessDenied` - USER -> 403
- `changeRate_bankierRole_accessDenied` - BANKIER -> 403
- `changeRate_withoutCsrf_forbidden` - missing CSRF -> 403

**Business Logic Tests** (`LoanServiceTest`, Mockito):
- `changeInterestRate_existingLoan_updatesRate` - verification of new rate
- `changeInterestRate_recalculatesPayments` - 100,000 PLN, change from 5% to 7% -> 1,161.08 PLN payment
- `changeInterestRate_savesToRepository` - `verify(loanRepository).save(loan)`
- `changeInterestRate_nonExistingLoan_throwsLoanNotFoundException` - `assertThrows(LoanNotFoundException.class)`
- `changeInterestRate_toZero_calculatesEqualPayments` - 0% interest -> payment = amount / term

---

## Error Handling - GlobalExceptionHandler

File: `exception/GlobalExceptionHandler.java`  
Annotation: `@ControllerAdvice`

| Exception               | Handling                                                                                                     |
|-------------------------|--------------------------------------------------------------------------------------------------------------|
| `AccessDeniedException` | Re-thrown - Spring Security handles and returns HTTP 403                                                     |
| `LoanNotFoundException` | Renders `error.html` view with message "Credit offer with ID not found: ..."                                 |
| `RuntimeException`      | Renders `error.html` view with error message (e.g., "Insufficient funds on account")                         |

Handler order is crucial - `AccessDeniedException` (inheriting from `RuntimeException`) must be handled BEFORE general `RuntimeException` so that Spring Security can return 403 instead of rendering an error page with HTTP 200.

---

## Tests

### Controller Integration Tests (MockMvc + Spring Security)

Usage: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@WithMockUser`

| Test Class                | File Path                                    | Number of Tests |
|---------------------------|----------------------------------------------|-----------------|
| `HomeControllerTest`      | `test/.../controller/HomeControllerTest.java` | 5               |
| `UserControllerTest`      | `test/.../controller/UserControllerTest.java` | 11              |
| `BankierControllerTest`   | `test/.../controller/BankierControllerTest.java` | 12              |
| `MenadzerControllerTest`  | `test/.../controller/MenadzerControllerTest.java` | 9               |

What they test:
- **Authorization** - `@WithMockUser(roles="...")` + `status().isForbidden()` verifies that `@PreAuthorize` on controllers blocks unauthorized users
- **Authentication** - requests without session -> redirect to `/login`
- **CSRF** - POST requests without `.with(csrf())` -> 403
- **Happy path** - valid data + valid role -> redirect with flash `success`
- **Business errors** - e.g., insufficient funds -> redirect with flash `error`
- **Views and model** - `view().name(...)` + `model().attributeExists(...)` verify the controller returns the correct template with data

### Service Unit Tests (Mockito)

Usage: `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`

| Test Class          | File Path                                  | Number of Tests |
|---------------------|--------------------------------------------|-----------------|
| `BankServiceTest`   | `test/.../service/BankServiceTest.java`    | 19              |
| `LoanServiceTest`   | `test/.../service/LoanServiceTest.java`    | 21              |

What they test:
- **Isolation** - repositories mocked via `@Mock`, services instantiated via `@InjectMocks`
- **Interaction verification** - `verify()`, `verify(..., never())` ensure service calls appropriate repository methods
- **ArgumentCaptor** - captures objects passed to `repository.save()` and verifies their fields
- **Exceptions** - `assertThrows()` for invalid inputs and missing records
- **@ParameterizedTest @CsvSource** - 5 datasets to verify PMT calculations

### Execution

```bash
./mvnw test
```

Total number of tests: **77** (37 integration + 40 unit)
