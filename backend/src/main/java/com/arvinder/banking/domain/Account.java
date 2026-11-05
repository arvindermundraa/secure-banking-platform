package com.arvinder.banking.domain;

import com.arvinder.banking.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    /** Full account number, encrypted at rest. Only the last 4 digits are stored in clear. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 512)
    private String accountNumber;

    @Column(nullable = false, length = 4)
    private String last4;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Version
    private long version;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Account() {
    }

    public Account(AppUser owner, String accountNumber) {
        this.owner = owner;
        this.accountNumber = accountNumber;
        this.last4 = accountNumber.substring(accountNumber.length() - 4);
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("insufficient funds");
        }
        balance = balance.subtract(amount);
    }

    public Long getId() { return id; }
    public AppUser getOwner() { return owner; }
    public String getAccountNumber() { return accountNumber; }
    public String getLast4() { return last4; }
    public BigDecimal getBalance() { return balance; }
    public Instant getCreatedAt() { return createdAt; }
}
