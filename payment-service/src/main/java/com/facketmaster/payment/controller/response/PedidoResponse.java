package com.facketmaster.payment.controller.response;

import com.facketmaster.payment.enums.MetodoPagamento;
import com.facketmaster.payment.enums.StatusPagamento;
import com.facketmaster.payment.enums.StatusPedido;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoResponse {

    private UUID pedidoId;
    private Long eventoId;
    private String usuarioId;
    private Integer quantidade;
    private BigDecimal valorUnitario;
    private BigDecimal valorTotal;
    private MetodoPagamento metodoPagamento;

    private StatusPedido statusPedido;
    private StatusPagamento statusPagamento;

    private UUID pagamentoId;
    private String codigoTransacao;

    private String codigoPagamento;

    private String urlPagamento;

    private LocalDateTime dataExpiracao;

    private String motivoRecusa;

    private List<IngressoResponse> ingressos;

    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IngressoResponse {
        private UUID ingressoId;
        private String codigoIngresso;
        private String statusIngresso;
        private LocalDateTime emitidoEm;
    }
}