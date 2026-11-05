package com.arvinder.banking.web;

import com.arvinder.banking.service.AccountService;
import com.arvinder.banking.web.Dtos.AccountView;
import com.arvinder.banking.web.Dtos.AmountRequest;
import com.arvinder.banking.web.Dtos.EntryView;
import com.arvinder.banking.web.Dtos.TransferRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    public List<AccountView> list(Principal me) {
        return service.listFor(me.getName()).stream().map(AccountView::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView open(Principal me) {
        return AccountView.of(service.open(me.getName()));
    }

    @GetMapping("/{id}/transactions")
    public List<EntryView> history(Principal me, @PathVariable Long id) {
        return service.history(me.getName(), id).stream().map(EntryView::of).toList();
    }

    @PostMapping("/{id}/deposit")
    public AccountView deposit(Principal me, @PathVariable Long id, @Valid @RequestBody AmountRequest req,
                               @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return AccountView.of(service.deposit(me.getName(), id, req.amount(), keyOrRandom(key)));
    }

    @PostMapping("/transfer")
    public AccountView transfer(Principal me, @Valid @RequestBody TransferRequest req,
                                @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return AccountView.of(service.transfer(me.getName(), req.fromAccountId(), req.toAccountId(),
                req.amount(), keyOrRandom(key)));
    }

    private static String keyOrRandom(String key) {
        return key == null || key.isBlank() ? UUID.randomUUID().toString() : key.substring(0, Math.min(64, key.length()));
    }
}
