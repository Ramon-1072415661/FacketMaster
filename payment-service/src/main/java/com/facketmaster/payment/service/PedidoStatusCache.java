package com.facketmaster.payment.service;

import com.facketmaster.payment.enums.MetodoPagamento;
import com.facketmaster.payment.enums.StatusPagamento;
import com.facketmaster.payment.enums.StatusPedido;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Snapshot do estado atual de um pedido, armazenado no Redis.
 * <p>
 * Chave Redis: pedido:{pedidoId}
 * TTL: configurável via app.payment.cache-ttl-seconds (padrão 10 min)
 * <p>
 * Atualizado sempre que o status do pedido ou pagamento muda,
 * garantindo que consultas de status sejam servidas pelo cache
 * sem pressionar o banco de dados.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoStatusCache implements Serializable {

    private UUID pedidoId;
    private Long eventoId;
    private String usuarioId;
    private Integer quantidade;
    private BigDecimal valorTotal;
    private MetodoPagamento metodoPagamento;
    private StatusPedido statusPedido;

    private UUID pagamentoId;
    private StatusPagamento statusPagamento;
    private String codigoTransacao;
    private String codigoPagamento;
    private String urlPagamento;
    private LocalDateTime dataExpiracao;
    private String motivoRecusa;

    private List<IngressoInfo> ingressos;

    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IngressoInfo implements Serializable {
        private UUID ingressoId;
        private String codigoIngresso;
        private String statusIngresso;
    }
}
