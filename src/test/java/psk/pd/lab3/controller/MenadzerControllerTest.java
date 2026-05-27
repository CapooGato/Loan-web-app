package psk.pd.lab3.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import psk.pd.lab3.model.Loan;
import psk.pd.lab3.repository.LoanRepository;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MenadzerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository loanRepository;

    // --- autoryzacja GET ---

    @Test
    void dashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/menadzer/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "menadzer", roles = "MENADZER")
    void dashboard_menadzerRole_returns200() throws Exception {
        mockMvc.perform(get("/menadzer/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("menadzer/dashboard"))
                .andExpect(model().attributeExists("loans"));
    }

    @Test
    @WithMockUser(username = "jan", roles = "USER")
    void dashboard_userRole_accessDenied() throws Exception {
        mockMvc.perform(get("/menadzer/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "bankier", roles = "BANKIER")
    void dashboard_bankierRole_accessDenied() throws Exception {
        mockMvc.perform(get("/menadzer/dashboard"))
                .andExpect(status().isForbidden());
    }

    // --- POST /menadzer/change-rate ---

    @Test
    @WithMockUser(username = "menadzer", roles = "MENADZER")
    void changeRate_menadzerRole_existingLoan_redirectsWithSuccess() throws Exception {
        Loan loan = new Loan();
        loan.setId("test-loan-1");
        loan.setBorrowerUsername("jan");
        loan.setAmount(100000);
        loan.setAnnualInterestRate(5.0);
        loan.setTermMonths(120);
        loan.setMonthlyPayment(1060.66);
        loan.setTotalAmount(127279.20);
        loan.setTotalInterest(27279.20);
        loan.setStatus("ACTIVE");
        loan.setGrantedBy("bankier");
        loan.setCreatedAt(LocalDateTime.now());
        loanRepository.save(loan);

        mockMvc.perform(post("/menadzer/change-rate")
                        .param("loanId", "test-loan-1")
                        .param("newRate", "7.0")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/menadzer/dashboard"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @WithMockUser(username = "menadzer", roles = "MENADZER")
    void changeRate_nonExistingLoan_redirectsWithError() throws Exception {
        mockMvc.perform(post("/menadzer/change-rate")
                        .param("loanId", "nie-istnieje")
                        .param("newRate", "7.0")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/menadzer/dashboard"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "jan", roles = "USER")
    void changeRate_userRole_accessDenied() throws Exception {
        mockMvc.perform(post("/menadzer/change-rate")
                        .param("loanId", "test-loan-1")
                        .param("newRate", "7.0")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "bankier", roles = "BANKIER")
    void changeRate_bankierRole_accessDenied() throws Exception {
        mockMvc.perform(post("/menadzer/change-rate")
                        .param("loanId", "test-loan-1")
                        .param("newRate", "7.0")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "menadzer", roles = "MENADZER")
    void changeRate_withoutCsrf_forbidden() throws Exception {
        mockMvc.perform(post("/menadzer/change-rate")
                        .param("loanId", "test-loan-1")
                        .param("newRate", "7.0"))
                .andExpect(status().isForbidden());
    }
}
