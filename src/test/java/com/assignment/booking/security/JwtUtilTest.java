package com.assignment.booking.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void generatesTokenContainingUsernameAndRole() {
        String token = jwtUtil.generateToken("alice", "USER");

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("USER");
    }

    @Test
    void validatesTokenForMatchingUsername() {
        String token = jwtUtil.generateToken("bob", "ADMIN");

        assertThat(jwtUtil.isTokenValid(token, "bob")).isTrue();
        assertThat(jwtUtil.isTokenValid(token, "someone-else")).isFalse();
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtUtil.generateToken("carol", "USER");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtUtil.extractUsername(tampered))
                .isInstanceOf(Exception.class);
    }
}
