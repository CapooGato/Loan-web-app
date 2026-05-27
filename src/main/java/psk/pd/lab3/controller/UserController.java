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
 * Kontroler panelu uzytkownika (klienta banku).
 *
 * TODO 1: Dodaj adnotacje @PreAuthorize ograniczajaca dostep do calego kontrolera
 *         tylko dla uzytkownikow z rola USER.
 *         Uzyj: @PreAuthorize("hasRole('USER')")
 */
@PreAuthorize("hasRole('USER')")
@Controller
@RequestMapping("/user")
public class UserController {

    private final BankService bankService;
    private final LoanService loanService;

    public UserController(BankService bankService, LoanService loanService) {
        this.bankService = bankService;
        this.loanService = loanService;
    }

    /**
     * GET /user/dashboard - wyswietla panel uzytkownika z saldem, pozyczkami i historia transakcji.
     *
     * TODO 2: Zaimplementuj metode:
     *   a) Pobierz nazwe zalogowanego uzytkownika z obiektu Principal: principal.getName()
     *   b) Dodaj do modelu atrybut "account" - wywolaj bankService.getAccount(username)
     *   c) Dodaj do modelu atrybut "transactions" - wywolaj bankService.getTransactions(username)
     *   d) Dodaj do modelu atrybut "loans" - wywolaj loanService.getLoansByBorrower(username)
     *   e) Zwroc nazwe widoku: "user/dashboard"
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        String username = principal.getName();
        model.addAttribute("account", bankService.getAccount(username));
        model.addAttribute("transactions", bankService.getTransactions(username));
        model.addAttribute("loans", loanService.getLoansByBorrower(username));
        return "user/dashboard";
    }

    /**
     * POST /user/deposit - wplata srodkow na wlasne konto.
     *
     * TODO 3: Zaimplementuj metode:
     *   a) W bloku try:
     *      - Wywolaj bankService.deposit(principal.getName(), amount, principal.getName())
     *      - Dodaj flash attribute "success" z komunikatem: "Wplacono " + amount + " PLN"
     *   b) W bloku catch(Exception e):
     *      - Dodaj flash attribute "error" z komunikatem: e.getMessage()
     *   c) Zwroc redirect: "redirect:/user/dashboard"
     */
    @PostMapping("/deposit")
    public String deposit(@RequestParam double amount, Principal principal, RedirectAttributes ra) {
        // TODO 3: Zaimplementuj zgodnie z opisem powyzej
        try{
            bankService.deposit(principal.getName(), amount, principal.getName());
            ra.addFlashAttribute("success", "Wplacono " + amount + " PLN");
        }catch (Exception e){
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/user/dashboard";
    }

    /**
     * POST /user/withdraw - wyplata srodkow z wlasnego konta.
     *
     * TODO 4: Zaimplementuj metode (analogicznie do deposit):
     *   a) W bloku try:
     *      - Wywolaj bankService.withdraw(principal.getName(), amount, principal.getName())
     *      - Dodaj flash attribute "success" z komunikatem: "Wyplacono " + amount + " PLN"
     *   b) W bloku catch(Exception e):
     *      - Dodaj flash attribute "error" z komunikatem: e.getMessage()
     *   c) Zwroc redirect: "redirect:/user/dashboard"
     */
    @PostMapping("/withdraw")
    public String withdraw(@RequestParam double amount, Principal principal, RedirectAttributes ra) {
        // TODO 4: Zaimplementuj zgodnie z opisem powyzej
        try{
            bankService.withdraw(principal.getName(), amount, principal.getName());
            ra.addFlashAttribute("success", "Wyplacono " + amount + " PLN");
        } catch (Exception e){
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/user/dashboard";
    }
}
