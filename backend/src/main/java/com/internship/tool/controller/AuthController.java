package com.internship.tool.controller;

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
}