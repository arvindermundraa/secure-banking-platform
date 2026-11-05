package com.arvinder.banking.config;

import com.arvinder.banking.domain.Role;
import com.arvinder.banking.repo.UserRepository;
import com.arvinder.banking.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Creates the first admin from env vars (ADMIN_USERNAME / ADMIN_PASSWORD) if both are set. */
@Component
public class AdminSeeder implements ApplicationRunner {
    private final AuthService auth;
    private final UserRepository users;
    private final String username;
    private final String password;

    public AdminSeeder(AuthService auth, UserRepository users,
                       @Value("${ADMIN_USERNAME:}") String username,
                       @Value("${ADMIN_PASSWORD:}") String password) {
        this.auth = auth;
        this.users = users;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!username.isBlank() && !password.isBlank() && !users.existsByUsername(username)) {
            auth.create(username, password, null, Role.ADMIN);
        }
    }
}
