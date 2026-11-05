package com.arvinder.banking.web;

import com.arvinder.banking.security.JwtService;
import com.arvinder.banking.service.AuthService;
import com.arvinder.banking.web.Dtos.LoginRequest;
import com.arvinder.banking.web.Dtos.RegisterRequest;
import com.arvinder.banking.web.Dtos.TokenResponse;
import com.arvinder.banking.web.Dtos.UserView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final JwtService jwt;

    public AuthController(AuthService auth, JwtService jwt) {
        this.auth = auth;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody RegisterRequest req) {
        return UserView.of(auth.register(req.username(), req.password(), req.phone()));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return new TokenResponse(auth.login(req.username(), req.password()), jwt.ttlSeconds());
    }
}
