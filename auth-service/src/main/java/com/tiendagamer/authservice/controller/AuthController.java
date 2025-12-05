package com.tiendagamer.authservice.controller;

import com.tiendagamer.authservice.dto.LoginRequest;
import com.tiendagamer.authservice.dto.RegisterRequest;
import com.tiendagamer.authservice.dto.AuthResponse;
import com.tiendagamer.authservice.dto.ErrorResponse;
import com.tiendagamer.authservice.model.Role;
import com.tiendagamer.authservice.model.User;
import com.tiendagamer.authservice.repository.UserRepository;
import com.tiendagamer.authservice.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // Check if user already exists
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                ErrorResponse error = ErrorResponse.builder()
                        .error("User already registered with this email")
                        .timestamp(System.currentTimeMillis())
                        .status(HttpStatus.CONFLICT.value())
                        .build();
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            // Determine role (default to END_USER if not provided)
            Role role = Role.END_USER;
            if (request.getRole() != null && !request.getRole().isEmpty()) {
                try {
                    role = Role.valueOf(request.getRole().toUpperCase());
                } catch (IllegalArgumentException e) {
                    ErrorResponse error = ErrorResponse.builder()
                            .error("Invalid role. Valid roles: BACK_OFFICE_ADMIN, SALES_MANAGER, END_USER")
                            .timestamp(System.currentTimeMillis())
                            .status(HttpStatus.BAD_REQUEST.value())
                            .build();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                }
            }

            // Create new user
            User user = User.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(role)
                    .build();

            User savedUser = userRepository.save(user);

            // Generate JWT token
            String token = jwtService.generateToken(savedUser.getEmail());

            AuthResponse response = AuthResponse.builder()
                    .message("User registered successfully")
                    .token(token)
                    .email(savedUser.getEmail())
                    .role(savedUser.getRole().name())
                    .timestamp(System.currentTimeMillis())
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            ErrorResponse error = ErrorResponse.builder()
                    .error("Registration failed: " + e.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Login user
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Find user by email
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

            if (userOptional.isEmpty()) {
                ErrorResponse error = ErrorResponse.builder()
                        .error("Invalid email or password")
                        .timestamp(System.currentTimeMillis())
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            User user = userOptional.get();

            // Validate password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                ErrorResponse error = ErrorResponse.builder()
                        .error("Invalid email or password")
                        .timestamp(System.currentTimeMillis())
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            // Generate JWT token with custom claims
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", user.getRole().name());
            claims.put("userId", user.getId());

            String token = jwtService.generateToken(user.getEmail(), claims);

            AuthResponse response = AuthResponse.builder()
                    .message("Login successful")
                    .token(token)
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .timestamp(System.currentTimeMillis())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ErrorResponse error = ErrorResponse.builder()
                    .error("Login failed: " + e.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Validate token
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ErrorResponse error = ErrorResponse.builder()
                        .error("Missing or invalid Authorization header")
                        .timestamp(System.currentTimeMillis())
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            String token = authHeader.substring(7);
            String username = jwtService.extractUsername(token);

            // Find user to verify still exists
            Optional<User> userOptional = userRepository.findByEmail(username);
            if (userOptional.isEmpty()) {
                ErrorResponse error = ErrorResponse.builder()
                        .error("User not found")
                        .timestamp(System.currentTimeMillis())
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            User user = userOptional.get();
            if (jwtService.validateToken(token, username)) {
                AuthResponse response = AuthResponse.builder()
                        .message("Token is valid")
                        .email(username)
                        .role(user.getRole().name())
                        .timestamp(System.currentTimeMillis())
                        .build();
                return ResponseEntity.ok(response);
            } else {
                ErrorResponse error = ErrorResponse.builder()
                        .error("Token is invalid or expired")
                        .timestamp(System.currentTimeMillis())
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
        } catch (Exception e) {
            ErrorResponse error = ErrorResponse.builder()
                    .error("Token validation failed: " + e.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ErrorResponse error = ErrorResponse.builder()
                        .error("Missing or invalid Authorization header")
                        .timestamp(System.currentTimeMillis())
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            String token = authHeader.substring(7);
            String username = jwtService.extractUsername(token);
            Optional<User> userOptional = userRepository.findByEmail(username);
            if (userOptional.isEmpty()) {
                ErrorResponse error = ErrorResponse.builder()
                        .error("User not found")
                        .timestamp(System.currentTimeMillis())
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            User user = userOptional.get();

            // Generate new token
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", user.getRole().name());
            claims.put("userId", user.getId());
            String newToken = jwtService.generateToken(username, claims);

            AuthResponse response = AuthResponse.builder()
                    .message("Token refreshed successfully")
                    .token(newToken)
                    .email(username)
                    .timestamp(System.currentTimeMillis())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ErrorResponse error = ErrorResponse.builder()
                    .error("Token refresh failed: " + e.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Logout user (client-side token invalidation)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ErrorResponse error = ErrorResponse.builder()
                        .error("Missing or invalid Authorization header")
                        .timestamp(System.currentTimeMillis())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            String token = authHeader.substring(7);
            String username = jwtService.extractUsername(token);

            AuthResponse response = AuthResponse.builder()
                    .message("Logout successful")
                    .email(username)
                    .timestamp(System.currentTimeMillis())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ErrorResponse error = ErrorResponse.builder()
                    .error("Logout failed: " + e.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
