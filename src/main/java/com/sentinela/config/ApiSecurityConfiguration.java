package com.sentinela.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@Profile("api")
public class ApiSecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    PasswordEncoder apiPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService apiUsers(@Value("${sentinela.security.clients-json:}") String clientsJson,
                                @Value("${sentinela.security.username:}") String fallbackUsername,
                                @Value("${sentinela.security.password:}") String fallbackPassword,
                                PasswordEncoder apiPasswordEncoder) {
        Map<String, String> credentials = new LinkedHashMap<>();
        try {
            if (clientsJson != null && !clientsJson.isBlank()) {
                credentials.putAll(new ObjectMapper().readValue(clientsJson,
                        new TypeReference<Map<String, String>>() { }));
            } else if (!fallbackUsername.isBlank() && !fallbackPassword.isBlank()) {
                credentials.put(fallbackUsername, fallbackPassword);
            }
        } catch (Exception invalidClientConfiguration) {
            throw new IllegalStateException("API_CLIENTS_JSON must be a JSON object mapping usernames to passwords.",
                    invalidClientConfiguration);
        }
        if (credentials.isEmpty() || credentials.entrySet().stream()
                .anyMatch(entry -> entry.getKey().isBlank() || entry.getValue() == null || entry.getValue().isBlank())) {
            throw new IllegalStateException("Configure API clients with API_CLIENTS_JSON or API_USERNAME/API_PASSWORD.");
        }
        return new InMemoryUserDetailsManager(credentials.entrySet().stream()
                .map(entry -> User.withUsername(entry.getKey())
                        .password(apiPasswordEncoder.encode(entry.getValue()))
                        .roles("CLIENT")
                        .build())
                .toList());
    }
}
