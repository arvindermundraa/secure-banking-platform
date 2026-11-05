package com.arvinder.banking.web;

import com.arvinder.banking.repo.UserRepository;
import com.arvinder.banking.web.Dtos.UserView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Only reachable with ROLE_ADMIN (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;

    public AdminController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/users")
    public List<UserView> users() {
        return users.findAll().stream().map(UserView::of).toList();
    }
}
