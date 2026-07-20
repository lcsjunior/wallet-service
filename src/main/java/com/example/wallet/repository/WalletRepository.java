package com.example.wallet.repository;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import com.example.wallet.entity.Wallet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

  @Lock(PESSIMISTIC_WRITE)
  @Query("select w from Wallet w where w.id = :id")
  Optional<Wallet> findByIdForUpdate(@Param("id") UUID id);
}
