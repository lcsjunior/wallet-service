package com.example.wallet.repository;

import com.example.wallet.entity.IdempotencyEntry;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyEntryRepository extends JpaRepository<IdempotencyEntry, String> {

  @Override
  @Cacheable(cacheNames = "idempotencyEntry", key = "#correlationId", unless = "#result == null")
  Optional<IdempotencyEntry> findById(String correlationId);

  @Override
  @Cacheable(cacheNames = "idempotencyEntry", key = "#entity.correlationId")
  <S extends IdempotencyEntry> S save(S entity);
}
