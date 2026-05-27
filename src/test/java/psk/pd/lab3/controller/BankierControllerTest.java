package psk.pd.lab3.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BankierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // --- autoryzacja GET ---

    @Test
    void dashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/bankier/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "bankier", roles = "BANKIER")
    void dashboard_bankierRole_returns200() throws Exception {
        mockMvc.perform(get("/bankier/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("bankier/dashboard"))
                .andExpect(model().attributeExists("accounts", "loans"));
    }

    @Test
    @WithMockUser(username = "jan", roles = "USER")
    void dashboard_userRole_accessDenied() throws Exception {
        mockMvc.perform(get("/bankier/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "menadzer", roles = "MENADZER")
    void dashboard_menadzerRole_accessDenied() throws Exception {
        mockMvc.perform(get("/bankier/dashboard"))
                .andExpect(status().isForbidden());
    }

    // --- POST /bankier/deposit ---

    @Test
    @WithMockUser(username = "bankier", roles = "BANKIER")
    void deposit_bankierRole_redirectsToDashboard() throws Exception {
        mockMvc.perform(post("/bankier/deposit")
                        .param("username", "jan")
                        .param("amount", "500.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bankier/dashboard"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @WithMockUser(username = "jan", roles = "USER")
    void deposit_userRole_accessDenied() throws Exception {
        mockMvc.perform(post("/bankier/deposit")
                        .param("username", "jan")
                        .param("amount", "500.00")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // --- POST /bankier/withdraw ---

    @Test
    @WithMockUser(username = "bankier", roles = "BANKIER")
    void withdraw_bankierRole_redirectsToDashboard() throws Exception {
        mockMvc.perform(post("/bankier/withdraw")
                        .param("username", "jan")
                        .param("amount", "10.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bankier/dashboard"));
    }

    @Test
    @WithMockUser(username = "jan", roles = "USER")
    void withdraw_userRole_accessDenied() throws Exception {
        mockMvc.perform(post("/bankier/withdraw")
                        .param("username", "jan")
                        .param("amount", "10.00")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // --- POST /bankier/grant-loan ---

    @Test
    @WithMockUser(username = "bankier", roles = "BANKIER")
    void grantLoan_bankierRole_redirectsToDashboard() throws Exception {
        mockMvc.perform(post("/bankier/grant-loan")
                        .param("username", "anna")
                        .param("amount", "50000")
                        .param("interestRate", "5.0")
                        .param("termMonths", "60")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bankier/dashboard"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @WithMockUser(username = "jan", roles = "USER")
    void grantLoan_userRole_accessDenied() throws Exception {
        mockMvc.perform(post("/bankier/grant-loan")
                        .param("username", "anna")
                        .param("amount", "50000")
                        .param("interestRate", "5.0")
                        .param("termMonths", "60")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "menadzer", roles = "MENADZER")
    void grantLoan_menadzerRole_accessDenied() throws Exception {
        mockMvc.perform(post("/bankier/grant-loan")
                        .param("username", "anna")
                        .param("amount", "50000")
                        .param("interestRate", "5.0")
                        .param("termMonths", "60")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "bankier", roles = "BANKIER")
    void grantLoan_withoutCsrf_forbidden() throws Exception {
        mockMvc.perform(post("/bankier/grant-loan")
                        .param("username", "anna")
                        .param("amount", "50000")
                        .param("interestRate", "5.0")
                        .param("termMonths", "60"))
                .andExpect(status().isForbidden());
    }
}
