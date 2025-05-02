package com.challenge.wallet.exception;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

public class SameWalletException extends ServiceException {

    public static final String MESSAGE = "Cannot transfer to the same wallet";

    public SameWalletException() {
        super(MESSAGE, BAD_REQUEST);
    }
}
