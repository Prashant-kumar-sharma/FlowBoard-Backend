package com.flowboard.auth.config;

import com.flowboard.auth.entity.User;
import com.flowboard.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.email}")
    private String adminEmail;

    @Value("${app.default-admin.password}")
    private String adminPassword;

    @Value("${app.default-admin.full-name:Platform Administrator}")
    private String adminFullName;

    @Override
    public void run(String... args) {
        userRepository.findByEmail(adminEmail).ifPresentOrElse(existingAdmin -> {
            boolean changed = false;
            if (existingAdmin.getRole() != User.Role.PLATFORM_ADMIN) {
                existingAdmin.setRole(User.Role.PLATFORM_ADMIN);
                changed = true;
            }
            if (!Boolean.TRUE.equals(existingAdmin.getIsActive())) {
                existingAdmin.setIsActive(true);
                changed = true;
            }
            if (existingAdmin.getProvider() != User.AuthProvider.LOCAL) {
                existingAdmin.setProvider(User.AuthProvider.LOCAL);
                existingAdmin.setProviderId(null);
                changed = true;
            }
            if (existingAdmin.getPasswordHash() == null
                    || existingAdmin.getPasswordHash().isBlank()
                    || !passwordEncoder.matches(adminPassword, existingAdmin.getPasswordHash())) {
                existingAdmin.setPasswordHash(passwordEncoder.encode(adminPassword));
                changed = true;
            }
            if (changed) {
                userRepository.save(existingAdmin);
                log.warn("Default admin account restored to active PLATFORM_ADMIN with local credentials: {}", adminEmail);
            } else {
                log.info("Admin User already exists.");
            }
        }, () -> {
            User admin = User.builder()
                    .email(adminEmail)
                    .fullName(adminFullName)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(User.Role.PLATFORM_ADMIN)
                    .provider(User.AuthProvider.LOCAL)
                    .isActive(true)
                    .build();

            userRepository.save(admin);
            log.info("==========================================================");
            log.info("Default Admin User Created:");
            log.info("Email: {}", adminEmail);
            log.info("Password sourced from environment configuration");
            log.info("==========================================================");
        });
    }
}
