package com.facketmaster.event.messaging;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Mirror local de {@code com.facketmaster.payment.messaging.ResultadoPagamentoMessage},
 * publicada pelo payment-service na fila {@code pagamentos.resultado}.
 * <p>
 * Os campos e nomes precisam ser compatíveis com a mensagem original para
 * que a desserialização via Jackson funcione corretamente.
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

    public enum StatusPedido {
        AGUARDANDO_PAGAMENTO, PROCESSANDO, APROVADO, RECUSADO, CANCELADO, EXPIRADO
    }

    public enum StatusPagamento {
        PENDENTE, PROCESSANDO, APROVADO, RECUSADO, CANCELADO, EXPIRADO
    }
}
