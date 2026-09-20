package com.example.wallet.interceptor;

import static com.example.wallet.constants.AppHeader.IDEMPOTENCY_KEY_HEADER;
import static com.example.wallet.constants.Messages.ENTITY_CONFLICT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

import com.example.wallet.exception.ServiceException;
import com.example.wallet.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

  private final IdempotencyService idempotencyService;

  public IdempotencyInterceptor(IdempotencyService idempotencyService) {
    this.idempotencyService = idempotencyService;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    var idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
    if (idempotencyKey == null) {
      return true;
    }
    if (!idempotencyService.reserve(request.getRequestURI(), idempotencyKey)) {
      throw ServiceException.of(ENTITY_CONFLICT, CONFLICT);
    }
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    var idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
    if (idempotencyKey != null && response.getStatus() >= BAD_REQUEST.value()) {
      idempotencyService.release(request.getRequestURI(), idempotencyKey);
    }
  }
}
