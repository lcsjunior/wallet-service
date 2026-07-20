package com.example.wallet.repository;

import com.example.wallet.entity.IdempotencyEntry;

public interface IdempotencyEntryRepositoryCache {

  IdempotencyEntry cache(IdempotencyEntry entry);
}
