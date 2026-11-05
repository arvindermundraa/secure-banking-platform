package com.arvinder.banking.service;

import com.arvinder.banking.domain.Account;
import com.arvinder.banking.domain.AppUser;
import com.arvinder.banking.domain.EntryType;
import com.arvinder.banking.domain.LedgerEntry;
import com.arvinder.banking.repo.AccountRepository;
import com.arvinder.banking.repo.LedgerEntryRepository;
import com.arvinder.banking.repo.UserRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private static final BigDecimal MAX_TRANSFER = new BigDecimal("50000.00");
    private final AccountRepository accounts;
    private final LedgerEntryRepository ledger;
    private final UserRepository users;
    private final SecureRandom random = new SecureRandom();

    public AccountService(AccountRepository accounts, LedgerEntryRepository ledger, UserRepository users) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.users = users;
    }

    @Transactional
    public Account open(String username) {
        AppUser owner = users.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user not found"));
        StringBuilder number = new StringBuilder("4000");
        for (int i = 0; i < 8; i++) {
            number.append(random.nextInt(10));
        }
        return accounts.save(new Account(owner, number.toString()));
    }

    @Transactional(readOnly = true)
    public List<Account> listFor(String username) {
        return accounts.findByOwnerUsernameOrderByIdAsc(username);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> history(String username, Long accountId) {
        owned(username, accounts.findById(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "account not found")));
        return ledger.findTop50ByAccountIdOrderByCreatedAtDescIdDesc(accountId);
    }

    @Transactional
    public Account deposit(String username, Long accountId, BigDecimal amount, String reference) {
        validate(amount);
        Account acct = owned(username, lock(accountId));
        if (ledger.existsByReferenceAndType(reference, EntryType.DEPOSIT)) {
            return acct;  // retry of a request that was already applied
        }
        acct.credit(amount);
        ledger.save(new LedgerEntry(acct, EntryType.DEPOSIT, amount, reference));
        return acct;
    }

    /**
     * Moves money between two accounts in one database transaction.
     * Both rows are locked FOR UPDATE in ascending id order, so two opposite transfers can't deadlock.
     * A repeated idempotency key returns without moving money again.
     */
    @Transactional
    public Account transfer(String username, Long fromId, Long toId, BigDecimal amount, String reference) {
        validate(amount);
        if (fromId.equals(toId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "cannot transfer to the same account");
        }
        Account first = lock(Math.min(fromId, toId));
        Account second = lock(Math.max(fromId, toId));
        Account from = first.getId().equals(fromId) ? first : second;
        Account to = from == first ? second : first;

        owned(username, from);
        if (ledger.existsByReferenceAndType(reference, EntryType.TRANSFER_OUT)) {
            return from;  // retry of a transfer that was already applied
        }
        if (from.getBalance().compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "insufficient funds");
        }
        from.debit(amount);
        to.credit(amount);
        ledger.save(new LedgerEntry(from, EntryType.TRANSFER_OUT, amount.negate(), reference));
        ledger.save(new LedgerEntry(to, EntryType.TRANSFER_IN, amount, reference));
        return from;
    }

    private Account lock(Long id) {
        return accounts.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "account not found"));
    }

    /** Ownership check: 404 rather than 403 so account ids of other customers aren't confirmed. */
    private Account owned(String username, Account acct) {
        if (!acct.getOwner().getUsername().equals(username)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "account not found");
        }
        return acct;
    }

    private static void validate(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "amount must be positive with at most 2 decimals");
        }
        if (amount.compareTo(MAX_TRANSFER) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "amount exceeds the per-transaction limit");
        }
    }
}
