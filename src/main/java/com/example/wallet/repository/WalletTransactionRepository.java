package com.example.wallet.repository;

import com.example.wallet.entity.WalletTransaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {}
