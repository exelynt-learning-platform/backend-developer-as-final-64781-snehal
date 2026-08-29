package com.assignment.booking.config;

import com.assignment.booking.entity.Resource;
import com.assignment.booking.entity.Role;
import com.assignment.booking.entity.User;
import com.assignment.booking.repository.ResourceRepository;
import com.assignment.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the database with demo ADMIN/USER accounts and a handful of sample
 * resources on startup, so the API is immediately testable.
 *
 * Seed credentials (also documented in README):
 *   admin / Admin@123   (ROLE_ADMIN)
 *   user  / User@123    (ROLE_USER)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedResources();
    }

    private void seedUsers() {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .build());
            log.info("Seeded ADMIN user");
        }

        if (!userRepository.existsByUsername("user")) {
            userRepository.save(User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("User@123"))
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
