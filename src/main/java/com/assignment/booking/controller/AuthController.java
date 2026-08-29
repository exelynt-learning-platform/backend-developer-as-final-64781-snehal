package com.assignment.booking.controller;

import com.assignment.booking.dto.auth.LoginRequest;
import com.assignment.booking.dto.auth.LoginResponse;
import com.assignment.booking.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints. Test with the Postman collection in /postman
 * ("Auth" folder -> Login as Admin / Login as User).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
