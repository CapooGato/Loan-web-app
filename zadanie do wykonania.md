# Laboratorium 4 - Spring Security + Thymeleaf

## Opis projektu

Aplikacja bankowa z trzema rolami użytkowników. Backend (modele, repozytoria, serwisy, obsługa błędów) jest już zaimplementowany. Zadaniem jest dokończenie warstwy kontrolerów, konfiguracji Spring Security i widoków Thymeleaf.

### Konta testowe (zdefiniowane w `application.yml`)

| Login      | Hasło        | Rola      | Saldo początkowe |
|------------|--------------|-----------|------------------|
| `jan`      | `jan123`     | USER      | 5000.00 PLN      |
| `anna`     | `anna123`    | USER      | 10000.00 PLN     |
| `bankier`  | `bank123`    | BANKIER   | -                |
| `menadzer` | `menadzer123`| MENADZER  | -                |

### Uprawnienia ról

| Rola     | Może                                                        |
|----------|-------------------------------------------------------------|
| USER     | Wpłacać i wypłacać środki na własnym koncie, podglądać swoje pożyczki i transakcje |
| BANKIER  | Wpłacać/wypłacać środki na kontach klientów, udzielać pożyczek |
| MENADZER | Podglądać historię wszystkich pożyczek, zmieniać oprocentowanie |

---

## Co jest już gotowe

- **Modele**: `BankAccount`, `Loan`, `Transaction` (`model/`)
- **Repozytoria**: `AccountRepository`, `LoanRepository`, `TransactionRepository` (`repository/`)
- **Serwisy**: `BankService` (wpłaty, wypłaty), `LoanService` (pożyczki, kalkulacja PMT) (`service/`)
- **Konfiguracja**: `AppUsersConfig` (ładowanie użytkowników z YML), `DataInitializer` (tworzenie kont przy starcie)
- **Obsługa błędów**: `GlobalExceptionHandler` (`exception/`)
- **Kontrolery**: `HomeController` (strona główna, logowanie) - gotowy
- **Szablony**: `login.html`, `error.html` - gotowe
- **CSS**: `static/css/style.css` - gotowy
- **Testy**: Wszystkie testy jednostkowe i integracyjne - gotowe (służą do weryfikacji rozwiązania)
- **CI/CD**: Pipeline `.gitlab-ci.yml` - gotowy (build, test, analiza statyczna, bezpieczeństwo)

---

## Przydatne API do wykorzystania w zadaniach

Przed rozpoczęciem implementacji warto zapoznać się z dostępnymi metodami serwisów i wzorcami Spring MVC.

### Metody serwisów

**BankService:**
- `getAccount(String username)` - zwraca obiekt `BankAccount` dla podanego loginu
- `getAllAccounts()` - zwraca listę wszystkich kont bankowych
- `deposit(String username, double amount, String performedBy)` - wpłata na konto
- `withdraw(String username, double amount, String performedBy)` - wypłata z konta
- `getTransactions(String username)` - zwraca historię transakcji użytkownika

**LoanService:**
- `getAllLoans()` - zwraca listę wszystkich pożyczek
- `getLoansByBorrower(String username)` - zwraca pożyczki danego użytkownika
- `grantLoan(String borrower, double amount, double rate, int months, String grantedBy)` - udzielenie pożyczki
- `changeInterestRate(String loanId, double newRate)` - zmiana oprocentowania

### Wzorce Spring MVC używane w kontrolerach

- `Principal principal` - obiekt reprezentujący zalogowanego użytkownika; `principal.getName()` zwraca login
- `Model model` - kontener na dane przekazywane do widoku; `model.addAttribute("klucz", wartosc)`
- `RedirectAttributes ra` - kontener na dane flash przekazywane po redirectcie; `ra.addFlashAttribute("klucz", wartosc)`
- Metody GET zwracają nazwę widoku Thymeleaf (np. `"user/dashboard"`)
- Metody POST zwracają redirect (np. `"redirect:/user/dashboard"`)

### Składnia Thymeleaf

- `th:action="@{/sciezka}"` - ustawia URL akcji formularza (automatycznie dodaje token CSRF)
- `th:text="${wyrażenie}"` - wyświetla wartość wyrażenia jako tekst
- `th:if="${warunek}"` - renderuje element tylko gdy warunek jest prawdziwy
- `th:each="element : ${lista}"` - iteracja po liście
- `th:value="${wyrażenie}"` - ustawia atrybut value
- `#numbers.formatDecimal(liczba, 1, 2)` - formatowanie liczby do 2 miejsc po przecinku
- `#temporals.format(data, 'yyyy-MM-dd HH:mm')` - formatowanie daty
- `sec:authentication="name"` - wyświetla login zalogowanego użytkownika (wymaga namespace `xmlns:sec`)

---

## Zadania do wykonania

### Część 1 - Adnotacje @PreAuthorize na kontrolerach (TODO 1, 5, 10)

Spring Security pozwala ograniczać dostęp do metod i klas za pomocą adnotacji `@PreAuthorize`. W projekcie jest już włączona obsługa tych adnotacji (`@EnableMethodSecurity` w `SecurityConfig`). Import `@PreAuthorize` jest już dodany w każdym kontrolerze.

**Pliki do edycji:**
- `controller/UserController.java` - TODO 1
- `controller/BankierController.java` - TODO 5
- `controller/MenadzerController.java` - TODO 10

**Opis:**
Na poziomie klasy każdego kontrolera należy dodać adnotację `@PreAuthorize`, która ograniczy dostęp do wszystkich endpointów danego kontrolera wyłącznie dla użytkowników z odpowiednią rolą. Składnia adnotacji: `@PreAuthorize("hasRole('NAZWA_ROLI')")`.

Każdy kontroler obsługuje jedną rolę - nazwa roli odpowiada nazwie kontrolera (np. `UserController` → rola `USER`).

**Weryfikacja:**
Testy kontrolerów sprawdzają, czy użytkownik z inną rolą dostaje HTTP 403:
```bash
./mvnw test -pl . -Dtest="UserControllerTest#dashboard_bankierRole_accessDenied"
./mvnw test -pl . -Dtest="BankierControllerTest#dashboard_userRole_accessDenied"
./mvnw test -pl . -Dtest="MenadzerControllerTest#dashboard_userRole_accessDenied"
```

---

### Część 2 - Konfiguracja SecurityFilterChain (TODO 13, 14)

**Plik do edycji:** `config/SecurityConfig.java`

W metodzie `securityFilterChain()` jest już skonfigurowana autoryzacja requestów i obsługa odmowy dostępu. Brakuje dwóch bloków konfiguracyjnych odpowiedzialnych za logowanie i wylogowanie.

**TODO 13 - Konfiguracja formularza logowania:**
Należy dodać blok `.formLogin(...)` konfigurujący trzy rzeczy:
- Niestandardową stronę logowania dostępną pod adresem `/login` (zamiast domyślnej strony Spring Security)
- Handler sukcesu logowania - metoda `authenticationSuccessHandler()` jest już zdefiniowana w klasie, wystarczy ją podpiąć
- Publiczny dostęp do strony logowania (`.permitAll()`) - aby niezalogowani mogli na nią wejść

**TODO 14 - Konfiguracja wylogowania:**
Należy dodać blok `.logout(...)` konfigurujący:
- URL, na który użytkownik zostanie przekierowany po wylogowaniu - powinno to być `/login` z parametrem `?logout`
- Publiczny dostęp do endpointu wylogowania

Dokumentacja: [Spring Security - Java Configuration - HttpSecurity](https://docs.spring.io/spring-security/reference/servlet/configuration/java.html)

**Weryfikacja:**
```bash
./mvnw test -pl . -Dtest="HomeControllerTest"
```

---

### Część 3 - Logika kontrolerów (TODO 2-4, 6-9, 11-12)

Każdy kontroler ma metody ze szkieletem (sygnatura, adnotacje `@GetMapping`/`@PostMapping`, Javadoc z opisem) ale bez implementacji - zwracają `null`. Należy uzupełnić ciała metod.

Ogólny wzorzec dla metod w kontrolerach:
- **Metody GET (dashboard)** - pobierają dane z serwisów, dodają je do modelu jako atrybuty, zwracają nazwę widoku Thymeleaf
- **Metody POST (operacje)** - wywołują odpowiednią metodę serwisu w bloku try-catch; w przypadku sukcesu dodają flash attribute `"success"` z komunikatem, w przypadku błędu - flash attribute `"error"` z treścią wyjątku; na końcu zwracają redirect na dashboard danego kontrolera

#### UserController (TODO 2, 3, 4)

**Plik:** `controller/UserController.java`

**TODO 2 - dashboard():** Panel użytkownika wyświetla trzy sekcje danych - saldo konta, listę pożyczek i historię transakcji. Wszystkie dane dotyczą zalogowanego użytkownika. Login zalogowanego użytkownika jest dostępny w obiekcie `Principal`. Widok do zwrócenia: `"user/dashboard"`.

**TODO 3 - deposit():** Wpłata na własne konto. W parametrze `performedBy` (trzeci argument metody `deposit`) należy przekazać login zalogowanego użytkownika - to on sam wykonuje operację.

**TODO 4 - withdraw():** Wypłata z własnego konta. Logika analogiczna do wpłaty, ale z użyciem metody `withdraw` serwisu.

**Weryfikacja:**
```bash
./mvnw test -pl . -Dtest="UserControllerTest"
```

#### BankierController (TODO 6, 7, 8, 9)

**Plik:** `controller/BankierController.java`

**TODO 6 - dashboard():** Panel bankiera wyświetla dwie sekcje - listę wszystkich kont klientów i listę wszystkich pożyczek. Widok: `"bankier/dashboard"`.

**TODO 7 - deposit():** Wpłata na konto wybranego klienta. Parametr `username` pochodzi z formularza (select z listą klientów). W parametrze `performedBy` należy przekazać login bankiera - to on wykonuje operację w imieniu klienta.

**TODO 8 - withdraw():** Wypłata z konta wybranego klienta. Logika analogiczna do wpłaty bankiera.

**TODO 9 - grantLoan():** Udzielenie pożyczki. Metoda `grantLoan` serwisu przyjmuje 5 argumentów: login klienta, kwotę, oprocentowanie, okres w miesiącach i login bankiera udzielającego pożyczki. Wszystkie parametry oprócz ostatniego pochodzą z formularza.

**Weryfikacja:**
```bash
./mvnw test -pl . -Dtest="BankierControllerTest"
```

#### MenadzerController (TODO 11, 12)

**Plik:** `controller/MenadzerController.java`

**TODO 11 - dashboard():** Panel menedżera wyświetla listę wszystkich pożyczek w systemie. Widok: `"menadzer/dashboard"`.

**TODO 12 - changeRate():** Zmiana oprocentowania pożyczki. Metoda `changeInterestRate` serwisu przyjmuje ID pożyczki i nowe oprocentowanie - oba parametry pochodzą z formularza.

**Weryfikacja:**
```bash
./mvnw test -pl . -Dtest="MenadzerControllerTest"
```

---

### Część 4 - Szablony Thymeleaf (TODO 15-25)

Szablony HTML zawierają komentarze z opisem brakujących elementów. Nagłówki tabel (`<th>`) są już na miejscu - brakuje treści dynamicznych (formularze, iteracje, wyświetlanie danych).

#### user/dashboard.html (TODO 15-21)

**Plik:** `templates/user/dashboard.html`

**TODO 15** - Komunikaty flash. Po operacjach (wpłata, wypłata) kontroler dodaje do modelu atrybut `"success"` lub `"error"`. Należy wyświetlić te komunikaty warunkowo - każdy w osobnym `<div>` z odpowiednią klasą CSS (`success` lub `error`). Element powinien renderować się tylko wtedy, gdy dany atrybut istnieje w modelu.

**TODO 16** - Wyświetlenie salda konta. Atrybut modelu `account` zawiera obiekt `BankAccount` z polem `balance`. Kwotę należy sformatować do dwóch miejsc po przecinku i dodać suffix " PLN".

**TODO 17** - Formularz wpłaty. Formularz POST kierujący na endpoint `/user/deposit` z jednym polem liczbowym o nazwie `amount`. Ważne: użycie `th:action` (zamiast zwykłego `action`) jest konieczne, ponieważ Thymeleaf automatycznie doda token CSRF wymagany przez Spring Security.

**TODO 18** - Formularz wypłaty. Analogiczny do formularza wpłaty, ale kierujący na endpoint wypłaty.

**TODO 19** - Tabela pożyczek. Iteracja po liście `loans` z modelu. Każdy wiersz tabeli powinien wyświetlać: kwotę, oprocentowanie, okres, ratę miesięczną, łączną kwotę, odsetki, status i datę utworzenia. Nagłówki kolumn są już zdefiniowane - kolejność danych w wierszach powinna im odpowiadać. Pola modelu `Loan` można sprawdzić w pliku `model/Loan.java`.

**TODO 20** - Tabela transakcji. Iteracja po liście `transactions`. Każdy wiersz: data, typ transakcji, kwota, kto wykonał operację. Pola modelu `Transaction` - patrz `model/Transaction.java`.

**TODO 21** - Formularz wylogowania. Formularz POST kierujący na endpoint `/logout`. Spring Security obsługuje ten endpoint automatycznie. Przycisk powinien mieć klasę CSS `danger`.

#### bankier/dashboard.html (TODO 22-24)

**Plik:** `templates/bankier/dashboard.html`

Tabela kont klientów i tabela pożyczek są już zaimplementowane. Brakuje trzech formularzy operacji.

**TODO 22** - Formularz wpłaty na konto klienta. Formularz POST z dwoma polami: wybór klienta (element `<select>` z opcjami wygenerowanymi przez iterację po liście `accounts` z modelu) i kwota. Nazwa pola select musi odpowiadać parametrowi `username` w kontrolerze.

**TODO 23** - Formularz wypłaty z konta klienta. Struktura analogiczna do formularza wpłaty, ale kierujący na endpoint wypłaty.

**TODO 24** - Formularz udzielenia pożyczki. Formularz z czterema polami: wybór klienta (select jak wyżej), kwota pożyczki (`amount`), oprocentowanie roczne w procentach (`interestRate`) i okres w miesiącach (`termMonths`). Nazwy pól muszą odpowiadać parametrom `@RequestParam` w metodzie kontrolera.

#### menadzer/dashboard.html (TODO 25)

**Plik:** `templates/menadzer/dashboard.html`

**TODO 25** - Tabela pożyczek z formularzem zmiany oprocentowania. Iteracja po liście `loans` - każdy wiersz wyświetla dane pożyczki (ID, klient, kwota, oprocentowanie, okres, rata, łącznie, odsetki, status, kto udzielił, data). W ostatniej kolumnie każdego wiersza powinien znajdować się formularz POST pozwalający zmienić oprocentowanie danej pożyczki. Formularz potrzebuje ukrytego pola z ID pożyczki (`loanId`) oraz pola liczbowego z nowym oprocentowaniem (`newRate`), którego wartość domyślna to aktualne oprocentowanie.

---

## Pipeline CI/CD i analiza statyczna

Pipeline CI/CD jest już skonfigurowany w pliku `.gitlab-ci.yml`. Składa się z następujących etapów:

| Etap       | Job              | Opis                                                    |
|------------|------------------|---------------------------------------------------------|
| build      | `compile`        | Kompilacja projektu Maven                               |
| test       | `unit-tests`     | Uruchomienie testów JUnit + raport pokrycia JaCoCo      |
| quality    | `sonarqube`      | Analiza statyczna kodu w SonarQube                      |
| security   | `sast-semgrep`   | Statyczna analiza bezpieczeństwa (SAST)                 |
| security   | `dependency-scan`| Skanowanie zależności pod kątem podatności (Trivy)       |
| security   | `secret-detection`| Wykrywanie wycieków sekretów w kodzie (Gitleaks)       |
| summary    | `pipeline-summary`| Podsumowanie wyników pipeline'u                        |

### Konfiguracja zmiennych SonarQube

Aby analiza SonarQube działała poprawnie, należy dodać dwie zmienne w ustawieniach CI/CD projektu na GitLab:

**Settings → CI/CD → Variables:**

| Zmienna          | Opis                                              | Protected | Masked |
|------------------|----------------------------------------------------|-----------|--------|
| `SONAR_HOST_URL` | Adres serwera SonarQube (np. `https://sonarqube.oroshi.app`) | Nie       | Nie    |
| `SONAR_TOKEN`    | Token uwierzytelniający do SonarQube               | Nie       | Tak    |

`SONAR_HOST_URL` może być jawny - to publiczny adres serwera. `SONAR_TOKEN` powinien być ustawiony jako **Masked**, aby nie był widoczny w logach pipeline'u.

---

## Uruchomienie i testowanie

### Kompilacja
```bash
./mvnw clean compile
```

### Uruchomienie testów (weryfikacja rozwiązania)
```bash
./mvnw test
```

Testy są podzielone na:

**Testy integracyjne kontrolerów** (`@SpringBootTest` + `MockMvc` + `@WithMockUser`):
- `HomeControllerTest` (5 testów) - redirecty po logowaniu według roli
- `UserControllerTest` (11 testów) - autoryzacja, wpłaty, wypłaty, CSRF
- `BankierControllerTest` (12 testów) - autoryzacja, operacje na kontach klientów, pożyczki
- `MenadzerControllerTest` (9 testów) - autoryzacja, zmiana oprocentowania

**Testy jednostkowe serwisów** (`@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`):
- `BankServiceTest` (19 testów) - logika wpłat, wypłat, walidacja
- `LoanServiceTest` (21 testów) - logika pożyczek, kalkulacja PMT, `@ParameterizedTest`

**Łącznie: 77 testów** - wszystkie muszą przejść po poprawnym uzupełnieniu TODO.

### Uruchomienie aplikacji
```bash
./mvnw spring-boot:run
```
Aplikacja dostępna pod: http://localhost:8080

### Testowanie ręczne

1. Wejście na http://localhost:8080 - powinno przekierować na `/login`
2. Logowanie jako `jan` / `jan123` - powinien wyświetlić się panel użytkownika z saldem 5000 PLN
3. Wykonanie wpłaty i wypłaty - saldo powinno się odpowiednio zmienić
4. Wylogowanie i zalogowanie jako `bankier` / `bank123` - panel bankiera
5. Udzielenie pożyczki użytkownikowi `anna` - kwota powinna pojawić się na jej koncie
6. Zalogowanie jako `menadzer` / `menadzer123` - powinna być widoczna udzielona pożyczka
7. Zmiana oprocentowania - rata powinna się przeliczyć

---

## Podsumowanie TODO

| Nr  | Plik                        | Opis                                    |
|-----|-----------------------------|-----------------------------------------|
| 1   | UserController.java         | Adnotacja @PreAuthorize z rolą USER     |
| 2   | UserController.java         | Metoda dashboard() - model i widok      |
| 3   | UserController.java         | Metoda deposit() - wpłata + redirect    |
| 4   | UserController.java         | Metoda withdraw() - wypłata + redirect  |
| 5   | BankierController.java      | Adnotacja @PreAuthorize z rolą BANKIER  |
| 6   | BankierController.java      | Metoda dashboard() - model i widok      |
| 7   | BankierController.java      | Metoda deposit() - wpłata na konto klienta |
| 8   | BankierController.java      | Metoda withdraw() - wypłata z konta klienta |
| 9   | BankierController.java      | Metoda grantLoan() - udzielenie pożyczki |
| 10  | MenadzerController.java     | Adnotacja @PreAuthorize z rolą MENADZER |
| 11  | MenadzerController.java     | Metoda dashboard() - model i widok      |
| 12  | MenadzerController.java     | Metoda changeRate() - zmiana oprocentowania |
| 13  | SecurityConfig.java         | Konfiguracja formularza logowania       |
| 14  | SecurityConfig.java         | Konfiguracja wylogowania                |
| 15  | user/dashboard.html         | Komunikaty flash success/error          |
| 16  | user/dashboard.html         | Wyświetlenie salda konta                |
| 17  | user/dashboard.html         | Formularz wpłaty                        |
| 18  | user/dashboard.html         | Formularz wypłaty                       |
| 19  | user/dashboard.html         | Iteracja po pożyczkach                  |
| 20  | user/dashboard.html         | Iteracja po transakcjach                |
| 21  | user/dashboard.html         | Formularz wylogowania                   |
| 22  | bankier/dashboard.html      | Formularz wpłaty na konto klienta       |
| 23  | bankier/dashboard.html      | Formularz wypłaty z konta klienta       |
| 24  | bankier/dashboard.html      | Formularz udzielenia pożyczki           |
| 25  | menadzer/dashboard.html     | Tabela pożyczek + formularz zmiany oprocentowania |
