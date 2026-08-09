package com.example.wallet.exception;

import static com.example.wallet.constants.Messages.MISSING_REQUIRED_HEADER;
import static com.example.wallet.constants.Messages.VALIDATION_ERROR;
import static org.springframework.http.HttpStatus.CONFLICT;

import com.example.wallet.mapper.FieldErrorMapper;
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
  private static final String BUSINESS_ERROR_TITLE = "Business violation";
  private static final String VALIDATION_ERROR_TITLE = "Validation error";
  private static final String ENTITY_CONFLICT_DETAIL = "Conflict detected, please try again";

  private final FieldErrorMapper fieldErrorMapper;

  public GlobalExceptionHandler(FieldErrorMapper fieldErrorMapper) {
    this.fieldErrorMapper = fieldErrorMapper;
  }

  @ExceptionHandler(ServiceException.class)
  public ResponseEntity<ProblemDetail> handleServiceException(ServiceException ex) {
    var problemDetail = ProblemDetail.forStatus(ex.getHttpStatus());
    problemDetail.setTitle(BUSINESS_ERROR_TITLE);
    problemDetail.setDetail(ex.getMessage());
    return ResponseEntity.status(ex.getHttpStatus()).body(problemDetail);
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ProblemDetail> handleOptimisticLock() {
    var problemDetail = ProblemDetail.forStatus(CONFLICT);
    problemDetail.setTitle(BUSINESS_ERROR_TITLE);
    problemDetail.setDetail(ENTITY_CONFLICT_DETAIL);
    return ResponseEntity.status(CONFLICT).body(problemDetail);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<Object> handleMissingRequestHeader(MissingRequestHeaderException ex) {
    var problemDetail = ex.getBody();
    problemDetail.setTitle(VALIDATION_ERROR_TITLE);
    problemDetail.setDetail(MISSING_REQUIRED_HEADER.formatted(ex.getHeaderName()));
    return ResponseEntity.status(problemDetail.getStatus()).body(problemDetail);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var problemDetail = ex.getBody();
    problemDetail.setTitle(VALIDATION_ERROR_TITLE);
    problemDetail.setDetail(VALIDATION_ERROR);
    problemDetail.setProperty(
        PROPERTY_ERRORS,
        fieldErrorMapper.toFieldErrorDetails(ex.getBindingResult().getFieldErrors()));
    return ResponseEntity.status(problemDetail.getStatus()).body(problemDetail);
  }
}
