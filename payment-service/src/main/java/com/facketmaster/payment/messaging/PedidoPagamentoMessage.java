package com.facketmaster.payment.messaging;

import com.facketmaster.payment.enums.MetodoPagamento;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoPagamentoMessage implements Serializable {

    private UUID pedidoId;

    private Long eventoId;
    private String usuarioId;
    private Integer quantidade;
    private BigDecimal valorTotal;
    private MetodoPagamento metodoPagamento;

    private DadosCartao dadosCartao;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DadosCartao implements Serializable {
        private String numeroMascarado;
        private String nomeTitular;
        private String validade;
        private Integer parcelas;
        private String bandeira;
    }
}