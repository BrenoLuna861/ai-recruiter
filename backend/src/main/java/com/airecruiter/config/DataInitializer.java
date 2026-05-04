package com.airecruiter.config;

import com.airecruiter.entity.User;
import com.airecruiter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")  // NEVER runs in production
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createIfAbsent("Candidato Demo", "candidato@demo.com", "demo123", User.Role.CANDIDATE);
        createIfAbsent("Recrutador Demo", "recrutador@demo.com", "demo123", User.Role.RECRUITER);
        log.info("Demo users ready — candidato@demo.com / recrutador@demo.com (password: demo123)");
    }

    private void createIfAbsent(String name, String email, String password, User.Role role) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.save(User.builder()
                .name(name).email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role).active(true).build());
        }
    }
}
