package com.example.wallet.filter;

import static com.example.wallet.constants.Constants.CORRELATION_ID_HEADER;
import static com.example.wallet.constants.Constants.CORRELATION_ID_MDC_KEY;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import com.example.wallet.config.HttpLoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@Order(HIGHEST_PRECEDENCE)
public class HttpLoggingFilter extends OncePerRequestFilter {

  private static final long NANOS_PER_MILLI = 1_000_000L;

  private static final String SKIPPED_BODY = "<not logged>";

  private static final String JSON = "json";

  private static final Logger log = LoggerFactory.getLogger(HttpLoggingFilter.class);

  private final HttpLoggingProperties properties;
  private final HttpLogSanitizer sanitizer;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  public HttpLoggingFilter(HttpLoggingProperties properties, HttpLogSanitizer sanitizer) {
    this.properties = properties;
    this.sanitizer = sanitizer;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return properties.excludedPaths().stream()
        .anyMatch(pattern -> pathMatcher.match(pattern, request.getRequestURI()));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    putCorrelationId(request);
    var incomingRequest =
        isJson(request.getContentType()) ? CachedBodyRequest.of(request) : request;
    var cachingResponse = new ContentCachingResponseWrapper(response);
    var startedAt = System.nanoTime();
    logIncoming(incomingRequest);
    try {
      filterChain.doFilter(incomingRequest, cachingResponse);
    } finally {
      logOutgoing(cachingResponse, System.nanoTime() - startedAt);
      cachingResponse.copyBodyToResponse();
      MDC.remove(CORRELATION_ID_MDC_KEY);
    }
  }

  private void logIncoming(HttpServletRequest request) {
    if (!log.isEnabledForLevel(properties.level())) {
      return;
    }
    log.atLevel(properties.level())
        .log(
            "HTTP INCOMING {} {} headers={} body={}",
            request.getMethod(),
            requestUri(request),
            sanitizer.headers(new ServletServerHttpRequest(request).getHeaders()),
            requestBody(request));
  }

  private void logOutgoing(ContentCachingResponseWrapper response, long elapsed) {
    if (!log.isEnabledForLevel(properties.level())) {
      return;
    }
    log.atLevel(properties.level())
        .log(
            "HTTP OUTGOING status={} durationMs={} headers={} body={}",
            response.getStatus(),
            elapsed / NANOS_PER_MILLI,
            sanitizer.headers(responseHeaders(response)),
            sanitizer.body(response.getContentAsByteArray()));
  }

  private String requestBody(HttpServletRequest request) {
    return request instanceof CachedBodyRequest cachedRequest
        ? sanitizer.body(cachedRequest.getBody())
        : SKIPPED_BODY;
  }

  private boolean isJson(String contentType) {
    return contentType != null && contentType.contains(JSON);
  }

  private String requestUri(HttpServletRequest request) {
    var queryString = request.getQueryString();
    return queryString == null
        ? request.getRequestURI()
        : request.getRequestURI() + "?" + queryString;
  }

  private HttpHeaders responseHeaders(ContentCachingResponseWrapper response) {
    var headers = new HttpHeaders();
    response
        .getHeaderNames()
        .forEach(name -> headers.addAll(name, List.copyOf(response.getHeaders(name))));
    return headers;
  }

  private void putCorrelationId(HttpServletRequest request) {
    var correlationId = request.getHeader(CORRELATION_ID_HEADER);
    if (correlationId != null) {
      MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
    }
  }
}
