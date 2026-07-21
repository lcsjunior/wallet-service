package com.example.wallet.exception;

import static com.example.wallet.exception.ErrorCode.MISSING_REQUIRED_HEADER;
import static com.example.wallet.exception.ErrorCode.VALIDATION_ERROR;
import static org.springframework.http.HttpStatus.CONFLICT;

import com.example.wallet.mapper.FieldErrorMapper;
import com.example.wallet.service.resolver.MessageResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final String PROPERTY_ERRORS = "errors";

  private final MessageResolver messageResolver;
  private final FieldErrorMapper fieldErrorMapper;

  public GlobalExceptionHandler(
      MessageResolver messageResolver, FieldErrorMapper fieldErrorMapper) {
    this.messageResolver = messageResolver;
    this.fieldErrorMapper = fieldErrorMapper;
  }

  @ExceptionHandler(ServiceException.class)
  public ResponseEntity<ProblemDetail> handleServiceException(ServiceException ex) {
    var problemDetail = ProblemDetail.forStatus(ex.getHttpStatus());
    problemDetail.setTitle(messageResolver.resolve("business.error.title"));
    problemDetail.setDetail(messageResolver.resolve(ex.getMessage()));
    return ResponseEntity.status(ex.getHttpStatus()).body(problemDetail);
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ProblemDetail> handleOptimisticLock() {
    var problemDetail = ProblemDetail.forStatus(CONFLICT);
    problemDetail.setTitle(messageResolver.resolve("business.error.title"));
    problemDetail.setDetail(messageResolver.resolve("entity.conflict"));
    return ResponseEntity.status(CONFLICT).body(problemDetail);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<Object> handleMissingRequestHeader(MissingRequestHeaderException ex) {
    var problemDetail = ex.getBody();
    problemDetail.setTitle(messageResolver.resolve("validation.error.title"));
    problemDetail.setDetail(
        messageResolver.resolve(MISSING_REQUIRED_HEADER.getMessageKey(), ex.getHeaderName()));
    return ResponseEntity.status(problemDetail.getStatus()).body(problemDetail);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var problemDetail = ex.getBody();
    problemDetail.setTitle(messageResolver.resolve("validation.error.title"));
    problemDetail.setDetail(messageResolver.resolve(VALIDATION_ERROR.getMessageKey()));
    problemDetail.setProperty(
        PROPERTY_ERRORS,
        fieldErrorMapper.toFieldErrorDetails(ex.getBindingResult().getFieldErrors()));
    return ResponseEntity.status(problemDetail.getStatus()).body(problemDetail);
  }
}
