package com.assignment.booking.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.assignment.booking.entity.Resource;
import com.assignment.booking.entity.Role;
import com.assignment.booking.entity.User;
import com.assignment.booking.repository.ResourceRepository;
import com.assignment.booking.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @org.springframework.beans.factory.annotation.Value("${app.seed.admin-password:Admin@123}")
    private String adminSeedPassword;

    @org.springframework.beans.factory.annotation.Value("${app.seed.user-password:User@123}")
    private String userSeedPassword;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Data seeding is disabled (app.seed.enabled=false)");
            return;
        }
        seedUsers();
        seedResources();
    }

    private void seedUsers() {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode(adminSeedPassword))
                    .role(Role.ADMIN)
                    .build());
            log.info("Seeded ADMIN user");
        }

        if (!userRepository.existsByUsername("user")) {
            userRepository.save(User.builder()
                    .username("user")
                    .password(passwordEncoder.encode(userSeedPassword))
                    .role(Role.USER)
                    .build());
            log.info("Seeded USER user");
        }
    }

    private void seedResources() {
        if (resourceRepository.count() == 0) {
            resourceRepository.save(Resource.builder()
                    .name("Conference Room A")
                    .type("ROOM")
                    .description("10-seat conference room with projector")
                    .available(true)
                    .build());

            resourceRepository.save(Resource.builder()
                    .name("Toyota Corolla - KA01AB1234")
                    .type("VEHICLE")
                    .description("Company sedan for local travel")
                    .available(true)
                    .build());

            resourceRepository.save(Resource.builder()
                    .name("Projector Epson EB-X05")
                    .type("EQUIPMENT")
                    .description("Portable projector, HDMI + VGA")
                    .available(true)
                    .build());

            log.info("Seeded 3 sample resources");
        }
    }
}
