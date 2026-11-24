# Role-Based Banking & Loan Web Application (`Loan web app`)

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-6DB33F?style=flat&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-RBAC%20%7C%20Method%20Security-6DB33F?style=flat&logo=springsecurity&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-SSR-005F0F?style=flat&logo=thymeleaf&logoColor=white)

## Overview
A multi-role web banking application built with **Spring Boot**, **Spring Security**, and **Thymeleaf**. The system enforces strict **Role-Based Access Control (RBAC)** and method-level authorization (`@PreAuthorize`) across three distinct user personas (**Customer (`USER`)**, **Banker (`BANKIER`)**, and **Manager (`MENADZER`)**), featuring audit-logged financial transactions, loan origination with PMT calculation, and dynamic interest rate management.

---

## Security & Role-Based Access Control (RBAC)
- **Spring Security Filter Chain & Method Security**:
  - Configured with `@EnableWebSecurity` and `@EnableMethodSecurity`.
  - Custom login page (`/login`), role-aware `AuthenticationSuccessHandler` redirecting users to their dedicated dashboards, and custom access-denied handling (`/login?denied`).
  - Controller-level protection via `@PreAuthorize("hasRole('...')")` preventing horizontal and vertical privilege escalation.
- **Role Capabilities**:
  - **`USER` (`/user/dashboard`)**: View personal account balance, audit log of transactions, and active loans; perform self-service deposits and withdrawals.
  - **`BANKIER` (`/bankier/dashboard`)**: Manage client accounts, execute deposits/withdrawals on behalf of customers (recorded with `performedBy` audit trail), and grant new loans.
  - **`MENADZER` (`/menadzer/dashboard`)**: Monitor system-wide loan portfolios and adjust annual interest rates with automatic PMT recalculation.

---

## Tech Stack
- **Language:** Java 21
- **Framework:** Spring Boot 3.4.4 (Spring Web, Spring Security, Spring Validation)
- **Frontend:** Thymeleaf + `thymeleaf-extras-springsecurity6`, HTML5, CSS3
- **Testing:** JUnit 5, Spring Security Test (`@WithMockUser`), MockMvc
- **DevSecOps:** GitLab CI/CD, JaCoCo, SonarQube, Semgrep, Trivy

---

## Running Locally

```bash
# Run unit and Spring Security MockMvc tests
./mvnw clean test

# Start the web application
./mvnw spring-boot:run
```

Once running, access `http://localhost:8080/login` using the configured test profiles (`jan`, `anna`, `bankier`, `menadzer`).
