package psk.pd.lab3.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import psk.pd.lab3.service.BankService;
import psk.pd.lab3.service.LoanService;

import java.security.Principal;

/**
 * Kontroler panelu bankiera - obsluga wplat/wyplat klientow i udzielanie pozyczek.
 *
 * TODO 5: Dodaj adnotacje @PreAuthorize ograniczajaca dostep do calego kontrolera
 *         tylko dla uzytkownikow z rola BANKIER.
 *         Uzyj: @PreAuthorize("hasRole('BANKIER')")
 */
@PreAuthorize("hasRole('BANKIER')")
@Controller
@RequestMapping("/bankier")
public class BankierController {

    private final BankService bankService;
    private final LoanService loanService;

    public BankierController(BankService bankService, LoanService loanService) {
        this.bankService = bankService;
        this.loanService = loanService;
    }

    /**
     * GET /bankier/dashboard - wyswietla liste kont klientow i wszystkie pozyczki.
     *
     * TODO 6: Zaimplementuj metode:
     *   a) Dodaj do modelu atrybut "accounts" - wywolaj bankService.getAllAccounts()
     *   b) Dodaj do modelu atrybut "loans" - wywolaj loanService.getAllLoans()
     *   c) Zwroc nazwe widoku: "bankier/dashboard"
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // TODO 6: Zaimplementuj zgodnie z opisem powyzej
        model.addAttribute("accounts", bankService.getAllAccounts());
        model.addAttribute("loans", loanService.getAllLoans());
        return "bankier/dashboard";
    }

    /**
     * POST /bankier/deposit - wplata na konto wybranego klienta.
     *
     * TODO 7: Zaimplementuj metode:
     *   a) W bloku try:
     *      - Wywolaj bankService.deposit(username, amount, principal.getName())
     *        (trzeci argument to login bankiera - widoczny w historii transakcji klienta)
     *      - Dodaj flash attribute "success": "Wplacono " + amount + " PLN na konto " + username
     *   b) W bloku catch(Exception e):
     *      - Dodaj flash attribute "error": e.getMessage()
     *   c) Zwroc redirect: "redirect:/bankier/dashboard"
     */
    @PostMapping("/deposit")
    public String deposit(@RequestParam String username, @RequestParam double amount,
                          Principal principal, RedirectAttributes ra) {
        // TODO 7: Zaimplementuj zgodnie z opisem powyzej
        try {
            bankService.deposit(username, amount, principal.getName());
            ra.addFlashAttribute("success", "Wplacono " + amount + " PLN na konto " + username);
        } catch (Exception e){
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bankier/dashboard";
    }

    /**
     * POST /bankier/withdraw - wyplata z konta wybranego klienta.
     *
     * TODO 8: Zaimplementuj metode (analogicznie do deposit):
     *   a) W bloku try:
     *      - Wywolaj bankService.withdraw(username, amount, principal.getName())
     *      - Dodaj flash attribute "success": "Wyplacono " + amount + " PLN z konta " + username
     *   b) W bloku catch(Exception e):
     *      - Dodaj flash attribute "error": e.getMessage()
     *   c) Zwroc redirect: "redirect:/bankier/dashboard"
     */
    @PostMapping("/withdraw")
    public String withdraw(@RequestParam String username, @RequestParam double amount,
                           Principal principal, RedirectAttributes ra) {
        // TODO 8: Zaimplementuj zgodnie z opisem powyzej
        try {
            bankService.withdraw(username, amount, principal.getName());
            ra.addFlashAttribute("success", "Wyplacono " + amount + " PLN na konto " + username);
        } catch (Exception e){
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bankier/dashboard";
    }

    /**
     * POST /bankier/grant-loan - udzielenie pozyczki klientowi.
     *
     * TODO 9: Zaimplementuj metode:
     *   a) W bloku try:
     *      - Wywolaj loanService.grantLoan(username, amount, interestRate, termMonths, principal.getName())
     *      - Dodaj flash attribute "success": "Udzielono pozyczki " + amount + " PLN dla " + username
     *   b) W bloku catch(Exception e):
     *      - Dodaj flash attribute "error": e.getMessage()
     *   c) Zwroc redirect: "redirect:/bankier/dashboard"
     */
    @PostMapping("/grant-loan")
    public String grantLoan(@RequestParam String username, @RequestParam double amount,
                            @RequestParam double interestRate, @RequestParam int termMonths,
                            Principal principal, RedirectAttributes ra) {
        // TODO 9: Zaimplementuj zgodnie z opisem powyzej
        try {
            loanService.grantLoan(username, amount, interestRate, termMonths, principal.getName());
            ra.addFlashAttribute("success", "Udzielono pozyczki " + amount + " PLN dla " + username);
        } catch (Exception e){
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bankier/dashboard";
    }
}
