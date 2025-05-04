package com.challenge.wallet.exception;

import static com.challenge.wallet.constants.ValidationMessages.MSG_ENTITY_NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

public class WalletNotFoundException extends ServiceException {

    public WalletNotFoundException() {
        super("Wallet" + MSG_ENTITY_NOT_FOUND.getMessage(), NOT_FOUND);
    }
}
