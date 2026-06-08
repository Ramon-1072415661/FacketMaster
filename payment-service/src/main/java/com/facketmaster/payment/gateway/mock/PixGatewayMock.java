package com.facketmaster.payment.gateway.mock;

import com.facketmaster.payment.gateway.GatewayPagamento;
import com.facketmaster.payment.messaging.PedidoPagamentoMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
public class PixGatewayMock implements GatewayPagamento {

    @Value("${app.payment.pix-expiracao-minutos:30}")
    private long pixExpiracaoMinutos;

    @Value("${app.payment.mock-delay-ms:500}")
    private long mockDelayMs;

    @Override
    public ResultadoGateway processar(PedidoPagamentoMessage message) {
        log.info("[MOCK-PIX] Gerando QR Code/Chave PIX | pedidoId={} valor={}",
                message.getPedidoId(), message.getValorTotal());

        simularLatencia();

        if (message.getValorTotal().doubleValue() > 10_000.0) {
            log.warn("[MOCK-PIX] Valor acima do limite | pedidoId={}", message.getPedidoId());
            return ResultadoGateway.recusado("VALOR_ACIMA_DO_LIMITE_PIX");
        }

        String txId = "PIX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        LocalDateTime expiracao = LocalDateTime.now().plusMinutes(pixExpiracaoMinutos);

        String chavePix = gerarPayloadPixSimulado(txId, message);

        log.info("[MOCK-PIX] Chave PIX gerada | pedidoId={} txId={} expira={}",
                message.getPedidoId(), txId, expiracao);

        return ResultadoGateway.aprovadoComExpiracao(txId, chavePix, expiracao);
    }

    private String gerarPayloadPixSimulado(String txId, PedidoPagamentoMessage message) {
        return String.format(
                "00020126580014BR.GOV.BCB.PIX0136facketmaster@pix.com.br5204000053039865" +
                        "4%05.2f5802BR5913FacketMaster6009SAO PAULO62070503%s6304ABCD",
                message.getValorTotal().doubleValue(),
                txId.substring(4)
        );
    }

    private void simularLatencia() {
        try {
            Thread.sleep(mockDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}