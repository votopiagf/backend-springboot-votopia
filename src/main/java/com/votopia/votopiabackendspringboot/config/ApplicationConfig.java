package com.votopia.votopiabackendspringboot.config;

import com.votopia.votopiabackendspringboot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            // Avvolgiamo il risultato in un Optional per poter usare map e orElseThrow
            return java.util.Optional.ofNullable(userRepository.findUsersByEmail(username))
                    .map(CustomUserDetails::new)
                    .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}