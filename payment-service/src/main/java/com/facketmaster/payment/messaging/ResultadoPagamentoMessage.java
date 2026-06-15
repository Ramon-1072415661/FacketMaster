package com.facketmaster.payment.messaging;

import com.facketmaster.payment.enums.StatusPagamento;
import com.facketmaster.payment.enums.StatusPedido;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Mensagem publicada pelo payment-service na fila pagamentos.resultado
 * após o processamento de um pedido. Consumida por outros serviços
 * (ex.: event-service para decrementar estoque de ingressos).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoPagamentoMessage implements Serializable {

    private UUID pedidoId;
    private Long eventoId;
    private String usuarioId;
    private Integer quantidade;
    private StatusPedido statusPedido;
    private StatusPagamento statusPagamento;
    private String codigoTransacao;
    private String motivoRecusa;
    private LocalDateTime processadoEm;

    /**
     * Ingressos emitidos (preenchido apenas quando statusPedido = APROVADO).
     */
    private List<IngressoEmitido> ingressosEmitidos;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IngressoEmitido implements Serializable {
        private UUID ingressoId;
        private String codigoIngresso;
    }
}
