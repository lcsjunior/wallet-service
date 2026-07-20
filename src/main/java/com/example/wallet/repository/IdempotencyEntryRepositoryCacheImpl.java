package com.example.wallet.repository;

import com.example.wallet.entity.IdempotencyEntry;
import org.springframework.cache.annotation.CachePut;

public class IdempotencyEntryRepositoryCacheImpl implements IdempotencyEntryRepositoryCache {

  @Override
  @CachePut(cacheNames = "idempotencyEntry", key = "#entry.correlationId")
  public IdempotencyEntry cache(IdempotencyEntry entry) {
    return entry;
  }
}
