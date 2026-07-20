package com.example.wallet.repository;

import com.example.wallet.entity.IdempotencyEntry;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyEntryRepository
    extends JpaRepository<IdempotencyEntry, String>, IdempotencyEntryRepositoryCache {

  @Override
  @Cacheable(cacheNames = "idempotencyEntry", key = "#correlationId")
  Optional<IdempotencyEntry> findById(String correlationId);
}
