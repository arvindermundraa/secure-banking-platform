package com.arvinder.banking.web;

import com.arvinder.banking.domain.Account;
import com.arvinder.banking.domain.AppUser;
import com.arvinder.banking.domain.LedgerEntry;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class Dtos {
    private Dtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[a-zA-Z0-9_.-]+") String username,
            @NotBlank @Size(min = 10, max = 100) String password,
            @Pattern(regexp = "\\+?[0-9 -]{7,20}") String phone) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record TokenResponse(String token, long expiresInSeconds) {
    }

    public record AmountRequest(@NotNull BigDecimal amount) {
    }

    public record TransferRequest(@NotNull Long fromAccountId, @NotNull Long toAccountId, @NotNull BigDecimal amount) {
    }

    /** Never exposes the full account number. */
    public record AccountView(Long id, String maskedNumber, BigDecimal balance, Instant createdAt) {
        public static AccountView of(Account a) {
            return new AccountView(a.getId(), "****" + a.getLast4(), a.getBalance(), a.getCreatedAt());
        }
    }

    public record EntryView(Long id, String type, BigDecimal amount, BigDecimal balanceAfter, Instant createdAt) {
        public static EntryView of(LedgerEntry e) {
            return new EntryView(e.getId(), e.getType().name(), e.getAmount(), e.getBalanceAfter(), e.getCreatedAt());
        }
    }

    public record UserView(Long id, String username, String role, Instant createdAt) {
        public static UserView of(AppUser u) {
            return new UserView(u.getId(), u.getUsername(), u.getRole().name(), u.getCreatedAt());
        }
    }

    public record ErrorResponse(int status, String error) {
    }
}
