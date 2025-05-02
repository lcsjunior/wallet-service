package com.challenge.wallet.exception;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

public class SameWalletTransferException extends ServiceException {

    public static final String MESSAGE = "Cannot transfer to the same wallet";

    public SameWalletTransferException() {
        super(MESSAGE, BAD_REQUEST);
    }
}
