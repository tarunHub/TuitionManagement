package com.smartstart.portal.auth.service;

import com.smartstart.portal.auth.entity.AdminUser;
import com.smartstart.portal.auth.repository.AdminUserRepository;
import com.smartstart.portal.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the initial admin from ADMIN_USERNAME / ADMIN_PASSWORD when no such user exists yet. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final AdminUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = props.admin();
        if (admin == null || isBlank(admin.username()) || isBlank(admin.password())) {
            if (repository.count() == 0) {
                log.warn("No admin user exists and ADMIN_USERNAME/ADMIN_PASSWORD are not set; nobody can log in.");
            }
            return;
        }
        if (repository.existsByUsernameIgnoreCase(admin.username())) {
            return;
        }
        AdminUser user = new AdminUser();
        user.setUsername(admin.username().trim());
        user.setPasswordHash(passwordEncoder.encode(admin.password()));
        user.setFullName(isBlank(admin.fullName()) ? "Administrator" : admin.fullName());
        user.setEnabled(true);
        repository.save(user);
        log.info("Seeded initial admin user");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
