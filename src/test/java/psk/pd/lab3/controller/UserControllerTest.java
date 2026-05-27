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
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // --- autoryzacja GET ---

    @Test
    void dashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/user/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "jan", roles = "USER")
    void dashboard_userRole_returns200() throws Exception {
        mockMvc.perform(get("/user/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/dashboard"))
                .andExpect(model().attributeExists("account", "transactions", "loans"));
    }

    @Test
    @WithMockUser(username = "bankier", roles = "BANKIER")
    void dashboard_bankierRole_accessDenied() throws Exception {
        mockMvc.perform(get("/user/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "menadzer", roles = "MENADZER")
    void dashboard_menadzerRole_accessDenied() throws Exception {
        mockMvc.perform(get("/user/dashboard"))
                .andExpect(status().isForbidden());
    }

    // --- POST /user/deposit ---

    @Test
    @WithMockUser(username = "jan", roles = "USER")
    void deposit_userRole_redirectsToDashboard() throws Exception {
        mockMvc.perform(post("/user/deposit")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dashboard"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @WithMockUser(username = "bankier", roles = "BANKIER")
    void deposit_bankierRole_accessDenied() throws Exception {
        mockMvc.perform(post("/user/deposit")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deposit_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/user/deposit")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "jan", roles = "USER")
    void deposit_withoutCsrf_forbidden() throws Exception {
        mockMvc.perform(post("/user/deposit")
                        .param("amount", "100.00"))
                .andExpect(status().isForbidden());
    }

    // --- POST /user/withdraw ---

    @Test
    @WithMockUser(username = "jan", roles = "USER")
    void withdraw_userRole_redirectsToDashboard() throws Exception {
        mockMvc.perform(post("/user/withdraw")
                        .param("amount", "10.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dashboard"));
    }

    @Test
    @WithMockUser(username = "bankier", roles = "BANKIER")
    void withdraw_bankierRole_accessDenied() throws Exception {
        mockMvc.perform(post("/user/withdraw")
                        .param("amount", "10.00")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "jan", roles = "USER")
    void withdraw_insufficientFunds_redirectsWithError() throws Exception {
        mockMvc.perform(post("/user/withdraw")
                        .param("amount", "9999999.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dashboard"))
                .andExpect(flash().attributeExists("error"));
    }
}
