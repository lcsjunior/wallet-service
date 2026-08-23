package com.example.wallet.service;

import static com.example.wallet.constants.Messages.CORRELATION_ID_CONFLICT;
import static org.springframework.http.HttpStatus.CONFLICT;

import com.example.wallet.entity.IdempotencyEntry;
import com.example.wallet.exception.ServiceException;
import com.example.wallet.repository.IdempotencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

  private static final String LOG_PREFIX = "[IDEMPOTENCY_SERVICE] ";

  private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

  private final IdempotencyRepository idempotencyRepository;

  public IdempotencyService(IdempotencyRepository idempotencyRepository) {
    this.idempotencyRepository = idempotencyRepository;
  }

  public boolean isReplay(IdempotencyEntry idempotencyEntry) {
    var correlationId = idempotencyEntry.getCorrelationId();
    var storedEntry = idempotencyRepository.findById(correlationId);
    if (storedEntry.isEmpty()) {
      return false;
    }
    var storedFingerprint = storedEntry.get().getRequestFingerprint();
    if (!storedFingerprint.equals(idempotencyEntry.getRequestFingerprint())) {
      throw ServiceException.of(CORRELATION_ID_CONFLICT, CONFLICT);
    }
    log.info(LOG_PREFIX + "Duplicate request ignored");
    return true;
  }

  public void save(IdempotencyEntry idempotencyEntry) {
    idempotencyRepository.save(idempotencyEntry);
  }
}
