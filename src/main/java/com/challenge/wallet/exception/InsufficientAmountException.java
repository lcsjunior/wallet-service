package com.challenge.wallet.exception;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

public class InsufficientAmountException extends ServiceException {

    public static String MESSAGE = "Amount must be greater than zero";

    public InsufficientAmountException() {
        super(MESSAGE, BAD_REQUEST);
    }
}
