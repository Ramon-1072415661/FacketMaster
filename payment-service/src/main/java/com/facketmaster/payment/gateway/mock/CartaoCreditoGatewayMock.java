package com.facketmaster.payment.gateway.mock;

import com.facketmaster.payment.gateway.GatewayPagamento;
import com.facketmaster.payment.messaging.PedidoPagamentoMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class CartaoCreditoGatewayMock implements GatewayPagamento {

    private static final BigDecimal LIMITE_MAXIMO = new BigDecimal("5000.00");

    @Value("${app.payment.mock-delay-ms:500}")
    private long mockDelayMs;

    @Override
    public ResultadoGateway processar(PedidoPagamentoMessage message) {
        log.info("[MOCK-CARTAO] Processando pagamento | pedidoId={} valor={}",
                message.getPedidoId(), message.getValorTotal());

        simularLatencia();

        PedidoPagamentoMessage.DadosCartao cartao = message.getDadosCartao();

        if (cartao == null) {
            log.warn("[MOCK-CARTAO] Dados do cartão ausentes | pedidoId={}", message.getPedidoId());
            return ResultadoGateway.recusado("DADOS_CARTAO_AUSENTES");
        }

        if (cartao.getNumeroMascarado() != null && cartao.getNumeroMascarado().endsWith("0000")) {
            log.warn("[MOCK-CARTAO] Cartão inválido | pedidoId={}", message.getPedidoId());
            return ResultadoGateway.recusado("CARTAO_INVALIDO");
        }

        if (message.getValorTotal().compareTo(LIMITE_MAXIMO) > 0) {
            log.warn("[MOCK-CARTAO] Limite insuficiente | pedidoId={} valor={}",
                    message.getPedidoId(), message.getValorTotal());
            return ResultadoGateway.recusado("LIMITE_INSUFICIENTE");
        }

        if (cartao.getParcelas() != null && cartao.getParcelas() > 12) {
            log.warn("[MOCK-CARTAO] Parcelamento não permitido | parcelas={}", cartao.getParcelas());
            return ResultadoGateway.recusado("PARCELAMENTO_NAO_PERMITIDO");
        }

        String txId = "CC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        String bandeira = cartao.getBandeira() != null ? cartao.getBandeira() : "DESCONHECIDA";
        String parcelas = cartao.getParcelas() != null ? cartao.getParcelas() + "x" : "1x";
        String codigoPagamento = String.format("%s | %s | NSU: %s",
                bandeira, parcelas, txId.substring(3, 11));

        log.info("[MOCK-CARTAO] Pagamento APROVADO | pedidoId={} txId={}", message.getPedidoId(), txId);
        return ResultadoGateway.aprovado(txId, codigoPagamento, null);
    }

    private void simularLatencia() {
        try {
            Thread.sleep(mockDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}