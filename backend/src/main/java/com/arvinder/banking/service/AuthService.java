package com.arvinder.banking.service;

import com.arvinder.banking.domain.AppUser;
import com.arvinder.banking.domain.Role;
import com.arvinder.banking.repo.UserRepository;
import com.arvinder.banking.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /** Self-registration always creates a CUSTOMER; admins are created by another admin. */
    @Transactional
    public AppUser register(String username, String password, String phone) {
        return create(username, password, phone, Role.CUSTOMER);
    }

    @Transactional
    public AppUser create(String username, String password, String phone, Role role) {
        if (users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "username already taken");
        }
        return users.save(new AppUser(username, encoder.encode(password), role, phone));
    }

    /** Same error for unknown user and wrong password, so usernames can't be probed. */
    @Transactional(readOnly = true)
    public String login(String username, String password) {
        AppUser user = users.findByUsername(username)
                .filter(u -> encoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "invalid credentials"));
        return jwt.issue(user.getUsername(), user.getRole().name());
    }
}
