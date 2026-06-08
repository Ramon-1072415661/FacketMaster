package com.facketmaster.payment.exception;

import java.util.UUID;

public class PedidoNotFoundException extends RuntimeException {
    public PedidoNotFoundException(UUID id) {
        super("Pedido não encontrado: " + id);
    }
}