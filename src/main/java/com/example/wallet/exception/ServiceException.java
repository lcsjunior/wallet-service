package com.example.wallet.exception;

import org.springframework.http.HttpStatus;

public class ServiceException extends RuntimeException {

  private final HttpStatus httpStatus;

  private ServiceException(String message, HttpStatus httpStatus) {
    super(message);
    this.httpStatus = httpStatus;
  }

  public static ServiceException of(String message, HttpStatus httpStatus) {
    return new ServiceException(message, httpStatus);
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
