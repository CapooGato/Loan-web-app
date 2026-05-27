package psk.pd.lab3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Konfiguracja lancucha filtrow bezpieczenstwa.
     *
     * Czesc .authorizeHttpRequests i .exceptionHandling jest juz gotowa.
     *
     * TODO 13: Skonfiguruj formularz logowania (.formLogin):
     *   - Ustaw wlasna strone logowania: .loginPage("/login")
     *   - Ustaw handler sukcesu: .successHandler(authenticationSuccessHandler())
     *   - Zezwol na dostep publiczny: .permitAll()
     *
     * TODO 14: Skonfiguruj wylogowanie (.logout):
     *   - Ustaw URL po wylogowaniu: .logoutSuccessUrl("/login?logout")
     *   - Zezwol na dostep publiczny: .permitAll()
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**").permitAll()
                .anyRequest().authenticated()
            )
            // TODO 13: Dodaj konfiguracje .formLogin(form -> form. ...)
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(authenticationSuccessHandler())
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
            // TODO 14: Dodaj konfiguracje .logout(logout -> logout. ...)
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/login?denied")
            );
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            var roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            if (roles.contains("ROLE_MENADZER")) {
                response.sendRedirect("/menadzer/dashboard");
            } else if (roles.contains("ROLE_BANKIER")) {
                response.sendRedirect("/bankier/dashboard");
            } else {
                response.sendRedirect("/user/dashboard");
            }
        };
    }

    @Bean
    public UserDetailsService userDetailsService(AppUsersConfig config) {
        List<UserDetails> users = config.getUsers().stream()
                .map(u -> User.withUsername(u.getUsername())
                        .password("{noop}" + u.getPassword())
                        .roles(u.getRole())
                        .build())
                .toList();
        return new InMemoryUserDetailsManager(users);
    }
}
