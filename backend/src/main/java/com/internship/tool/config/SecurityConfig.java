package com.internship.tool.config;

import com.internship.tool.repository.UserRepository;
import com.internship.tool.security.JwtAuthenticationFilter;
import com.internship.tool.security.JwtUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Load users from the database (users + user_roles tables managed by Flyway).
     *
     * The @EntityGraph on UserRepository.findByUsername ensures roles are fetched
     * in a single JOIN query — no N+1 on every authenticated request.
     *
     * Role names in the DB are stored as "VIEWER", "MANAGER", "ADMIN".
     * Spring Security's hasRole() prepends "ROLE_", so we prefix here.
     *
     * For local development with H2, provide default users if database is empty.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            // For local development, provide default users
            if (isLocalDevelopment()) {
                return getDefaultUser(username);
            }

            com.internship.tool.entity.User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            var authorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                    .collect(Collectors.toList());

            return User.builder()
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .authorities(authorities)
                    .disabled(!user.isEnabled())
                    .build();
        };
    }

    private boolean isLocalDevelopment() {
        // Check if we're using H2 (local dev) vs PostgreSQL (production)
        return "org.h2.Driver".equals(System.getProperty("spring.datasource.driver-class-name", "org.h2.Driver"));
    }

    private UserDetails getDefaultUser(String username) {
        // Default users for local development
        switch (username) {
            case "admin":
                return User.builder()
                        .username("admin")
                        .password(passwordEncoder().encode("password"))
                        .authorities("ROLE_ADMIN")
                        .build();
            case "manager":
                return User.builder()
                        .username("manager")
                        .password(passwordEncoder().encode("password"))
                        .authorities("ROLE_MANAGER")
                        .build();
            case "viewer":
                return User.builder()
                        .username("viewer")
                        .password(passwordEncoder().encode("password"))
                        .authorities("ROLE_VIEWER")
                        .build();
            default:
                throw new UsernameNotFoundException("User not found: " + username);
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public JwtUtils jwtUtils() {
        return new JwtUtils();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtils jwtUtils,
                                                            UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtUtils, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
