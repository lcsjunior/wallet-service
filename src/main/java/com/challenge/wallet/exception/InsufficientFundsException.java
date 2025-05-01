package com.challenge.wallet.exception;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

public class InsufficientFundsException extends ServiceException {

    public static String MESSAGE = "Insufficient balance to complete this operation";

    public InsufficientFundsException() {
        super(MESSAGE, BAD_REQUEST);
    }
}
