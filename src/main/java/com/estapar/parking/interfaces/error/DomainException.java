package com.estapar.parking.interfaces.error;

public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
