package com.example.wallet.filter;

import static com.example.wallet.constants.AppHeader.CORRELATION_ID_HEADER;
import static com.example.wallet.constants.AppHeader.IDEMPOTENCY_KEY_HEADER;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

  public static final String CORRELATION_ID_MDC_KEY = "correlationId";

  public static final String IDEMPOTENCY_KEY_MDC_KEY = "idempotencyKey";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    put(CORRELATION_ID_MDC_KEY, request.getHeader(CORRELATION_ID_HEADER));
    put(IDEMPOTENCY_KEY_MDC_KEY, request.getHeader(IDEMPOTENCY_KEY_HEADER));
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(CORRELATION_ID_MDC_KEY);
      MDC.remove(IDEMPOTENCY_KEY_MDC_KEY);
    }
  }

  private void put(String mdcKey, String headerValue) {
    if (headerValue != null) {
      MDC.put(mdcKey, headerValue);
    }
  }
}
