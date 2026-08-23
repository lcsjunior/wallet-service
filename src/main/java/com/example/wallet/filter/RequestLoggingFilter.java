package com.example.wallet.filter;

import static com.example.wallet.constants.Constants.CORRELATION_ID_HEADER;
import static com.example.wallet.constants.Constants.CORRELATION_ID_MDC_KEY;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final String LOG_PREFIX = "[REQUEST_LOGGING_FILTER] ";

  private static final long NANOS_PER_MILLI = 1_000_000L;

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    putCorrelationId(request);
    var startedAt = System.nanoTime();
    log.info(
        LOG_PREFIX + "Request started method={} uri={}",
        request.getMethod(),
        request.getRequestURI());
    try {
      filterChain.doFilter(request, response);
    } finally {
      log.info(
          LOG_PREFIX + "Request finished method={} uri={} status={} durationMs={}",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          (System.nanoTime() - startedAt) / NANOS_PER_MILLI);
      MDC.remove(CORRELATION_ID_MDC_KEY);
    }
  }

  private void putCorrelationId(HttpServletRequest request) {
    var correlationId = request.getHeader(CORRELATION_ID_HEADER);
    if (correlationId != null) {
      MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
    }
  }
}
