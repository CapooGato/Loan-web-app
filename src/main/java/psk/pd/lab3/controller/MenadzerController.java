package psk.pd.lab3.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import psk.pd.lab3.service.LoanService;

/**
 * Kontroler panelu menadzera - przeglad historii pozyczek i zmiana oprocentowania.
 *
 * TODO 10: Dodaj adnotacje @PreAuthorize ograniczajaca dostep do calego kontrolera
 *          tylko dla uzytkownikow z rola MENADZER.
 *          Uzyj: @PreAuthorize("hasRole('MENADZER')")
 */
@PreAuthorize("hasRole('MENADZER')")
@Controller
@RequestMapping("/menadzer")
public class MenadzerController {

    private final LoanService loanService;

    public MenadzerController(LoanService loanService) {
        this.loanService = loanService;
    }

    /**
     * GET /menadzer/dashboard - wyswietla historie wszystkich pozyczek w systemie.
     *
     * TODO 11: Zaimplementuj metode:
     *   a) Dodaj do modelu atrybut "loans" - wywolaj loanService.getAllLoans()
     *   b) Zwroc nazwe widoku: "menadzer/dashboard"
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // TODO 11: Zaimplementuj zgodnie z opisem powyzej
        model.addAttribute("loans", loanService.getAllLoans());
        return "menadzer/dashboard";
    }

    /**
     * POST /menadzer/change-rate - zmiana oprocentowania istniejace pozyczki.
     *
     * TODO 12: Zaimplementuj metode:
     *   a) W bloku try:
     *      - Wywolaj loanService.changeInterestRate(loanId, newRate)
     *      - Dodaj flash attribute "success": "Zmieniono oprocentowanie pozyczki"
     *   b) W bloku catch(Exception e):
     *      - Dodaj flash attribute "error": e.getMessage()
     *   c) Zwroc redirect: "redirect:/menadzer/dashboard"
     */
    @PostMapping("/change-rate")
    public String changeRate(@RequestParam String loanId, @RequestParam double newRate,
                             RedirectAttributes ra) {
        // TODO 12: Zaimplementuj zgodnie z opisem powyzej
        try {
            loanService.changeInterestRate(loanId, newRate);
            ra.addFlashAttribute("success", "Zmieniono oprocentowanie pozyczki");
        } catch (Exception e){
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/menadzer/dashboard";
    }
}
