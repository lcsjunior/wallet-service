package com.example.wallet.filter;

import static com.example.wallet.constants.Constants.CORRELATION_ID_HEADER;
import static com.example.wallet.constants.Constants.CORRELATION_ID_MDC_KEY;
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

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    putCorrelationId(request);
    try {
      filterChain.doFilter(request, response);
    } finally {
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
