package com.example.wallet.repository;

import com.example.wallet.entity.IdempotencyEntry;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyEntry, UUID> {

  @Override
  @Cacheable(cacheNames = "idempotency-entry", key = "#correlationId", unless = "#result == null")
  Optional<IdempotencyEntry> findById(UUID correlationId);

  @Override
  @CachePut(cacheNames = "idempotency-entry", key = "#entity.correlationId")
  <S extends IdempotencyEntry> S save(S entity);
}
