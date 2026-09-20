package com.example.wallet.config;

import com.example.wallet.interceptor.IdempotencyInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private static final String DEPOSITS_PATH = "/v1/wallets/*/deposits";

  private static final String WITHDRAWALS_PATH = "/v1/wallets/*/withdrawals";

  private static final String TRANSFERS_PATH = "/v1/transfers";

  private final IdempotencyInterceptor idempotencyInterceptor;

  public WebMvcConfig(IdempotencyInterceptor idempotencyInterceptor) {
    this.idempotencyInterceptor = idempotencyInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(idempotencyInterceptor)
        .addPathPatterns(DEPOSITS_PATH, WITHDRAWALS_PATH, TRANSFERS_PATH);
  }
}
