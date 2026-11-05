package com.arvinder.banking.repo;

import com.arvinder.banking.domain.EntryType;
import com.arvinder.banking.domain.LedgerEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findTop50ByAccountIdOrderByCreatedAtDescIdDesc(Long accountId);

    boolean existsByReferenceAndType(String reference, EntryType type);
}
