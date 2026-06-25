package com.tfg.dashboard.client;

/**
 * Error controlado para fallos de comunicacion con Aruba Central.
 */
public class ArubaApiException extends RuntimeException {

    public ArubaApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
