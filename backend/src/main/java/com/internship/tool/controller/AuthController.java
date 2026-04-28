package com.internship.tool.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/register")
    public String register(@RequestBody String user) {
        return "User registered successfully";
    }

    @PostMapping("/login")
    public String login(@RequestBody String user) {
        return "JWT_TOKEN";
    }

    @PostMapping("/refresh")
    public String refreshToken() {
        return "New JWT_TOKEN";
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String listUsers() {
        return "All users (ADMIN only)";
    }

    @PostMapping("/assign-role")
    @PreAuthorize("hasRole('ADMIN')")
    public String assignRole(@RequestBody String assignment) {
        return "Role assigned (ADMIN only)";
    }
}