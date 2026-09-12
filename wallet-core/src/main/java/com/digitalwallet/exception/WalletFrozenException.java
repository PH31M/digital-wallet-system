package com.digitalwallet.exception;

/**
 * Thrown when a wallet is frozen.
 */
public class WalletFrozenException extends DigitalWalletException {

    public WalletFrozenException(String message) {
        super(ErrorCode.WALLET_FROZEN, message);
    }
}
