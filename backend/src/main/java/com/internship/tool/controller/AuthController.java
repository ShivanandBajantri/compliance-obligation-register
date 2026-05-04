package com.internship.tool.controller;

import com.internship.tool.dto.AuthResponse;
import com.internship.tool.dto.LoginRequest;
import com.internship.tool.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtils jwtUtils,
                          UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils              = jwtUtils;
        this.userDetailsService    = userDetailsService;
    }

    /**
     * Login — returns a JWT on success.
     *
     * Bug fix: BadCredentialsException is now caught and returned as 401
     * instead of propagating as a 500. The GlobalExceptionHandler catches
     * AuthenticationException, but BadCredentialsException is a subclass of
     * it so this explicit handler gives a cleaner message.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if (loginRequest.getUsername() == null || loginRequest.getUsername().isBlank()
                || loginRequest.getPassword() == null || loginRequest.getPassword().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username and password are required"));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername().trim(),
                            loginRequest.getPassword()));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtils.generateToken(userDetails);
            return ResponseEntity.ok(new AuthResponse("Bearer", token));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid username or password"));
        }
    }

    /**
     * Token refresh — validates the existing token and issues a new one.
     *
     * Bug fix: missing null/blank check on Authorization header.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Authorization header with Bearer token required"));
        }

        String token = authHeader.substring(7).trim();
        if (!jwtUtils.validateToken(token)) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Token is invalid or expired"));
        }

        String username = jwtUtils.getUsernameFromToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String refreshed = jwtUtils.generateToken(userDetails);
        return ResponseEntity.ok(new AuthResponse("Bearer", refreshed));
    }

    /**
     * Registration placeholder.
     *
     * Bug fix: was returning a plain String with 200 OK even though nothing
     * was persisted. Now returns 501 Not Implemented so clients know this
     * endpoint is not yet functional.
     *
     * Note (P3): Full user registration (persist to DB, hash password, assign
     * role) is documented as a known limitation in README.md.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody LoginRequest request) {
        return ResponseEntity.status(501)
                .body(Map.of("message",
                        "Self-service registration is not enabled. " +
                        "Contact an administrator to create an account."));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> listUsers() {
        return ResponseEntity.ok("User management via DB — see users table");
    }

    @PostMapping("/assign-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> assignRole(@RequestBody String assignment) {
        return ResponseEntity.status(501)
                .body(Map.of("message",
                        "Role assignment via API is not yet implemented. " +
                        "Manage roles directly in the database."));
    }
}
