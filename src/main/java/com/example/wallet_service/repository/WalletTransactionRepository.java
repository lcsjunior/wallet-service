package com.example.wallet_service.repository;

import com.example.wallet_service.entity.WalletTransaction;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    Optional<WalletTransaction> findTopByWalletIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(UUID walletId,
            Instant asOf);
}
