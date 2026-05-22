package com.facketmaster.exception;

public class EventoNotFoundException extends RuntimeException {
    public EventoNotFoundException(Long id) {
        super("Evento não encontrado com id: " + id);
    }
}
