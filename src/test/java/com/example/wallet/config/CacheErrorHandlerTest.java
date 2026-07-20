package com.example.wallet.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

class CacheErrorHandlerTest {

  private final CacheErrorHandler cacheErrorHandler = new CacheConfig().errorHandler();

  @Test
  @DisplayName("should swallow a cache get failure instead of rethrowing it")
  void shouldSwallowCacheGetFailure() {
    var cache = mock(Cache.class);

    assertThatCode(
            () ->
                cacheErrorHandler.handleCacheGetError(
                    new RuntimeException("redis down"), cache, "correlation-id"))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("should swallow a cache put failure instead of rethrowing it")
  void shouldSwallowCachePutFailure() {
    var cache = mock(Cache.class);

    assertThatCode(
            () ->
                cacheErrorHandler.handleCachePutError(
                    new RuntimeException("redis down"), cache, "correlation-id", "value"))
        .doesNotThrowAnyException();
  }
}
