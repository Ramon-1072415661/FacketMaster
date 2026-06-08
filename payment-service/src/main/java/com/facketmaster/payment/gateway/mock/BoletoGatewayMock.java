package com.facketmaster.payment.gateway.mock;

import com.facketmaster.payment.gateway.GatewayPagamento;
import com.facketmaster.payment.messaging.PedidoPagamentoMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
public class BoletoGatewayMock implements GatewayPagamento {

    private static final String BANCO_CODIGO = "237";
    private static final String BASE_URL_BOLETO = "https://boletos.facketmaster.com.br/boleto/";
    private static final Random RANDOM = new Random();

    @Value("${app.payment.boleto-expiracao-dias:3}")
    private long boletoExpiracaoDias;

    @Value("${app.payment.mock-delay-ms:500}")
    private long mockDelayMs;

    @Override
    public ResultadoGateway processar(PedidoPagamentoMessage message) {
        log.info("[MOCK-BOLETO] Gerando boleto | pedidoId={} valor={}",
                message.getPedidoId(), message.getValorTotal());

        simularLatencia();

        if (message.getValorTotal().doubleValue() < 5.0) {
            log.warn("[MOCK-BOLETO] Valor abaixo do mínimo | pedidoId={}", message.getPedidoId());
            return ResultadoGateway.recusado("VALOR_MINIMO_BOLETO");
        }

        String txId = "BOL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        LocalDateTime dataVencimento = LocalDateTime.now().plusDays(boletoExpiracaoDias);

        String codigoBarras = gerarCodigoBarrasSimulado(message);
        String linhaDigitavel = formatarLinhaDigitavel(codigoBarras);
        String urlBoleto = BASE_URL_BOLETO + txId.toLowerCase() + ".pdf";

        log.info("[MOCK-BOLETO] Boleto gerado | pedidoId={} txId={} vencimento={}",
                message.getPedidoId(), txId, dataVencimento.toLocalDate());

        return ResultadoGateway.pendente(txId, linhaDigitavel, urlBoleto, dataVencimento);
    }

    private String gerarCodigoBarrasSimulado(PedidoPagamentoMessage message) {
        long valorEmCentavos = message.getValorTotal()
                .multiply(new java.math.BigDecimal("100"))
                .longValue();
        long fatorVencimento = 1000 + RANDOM.nextInt(8999);
        String campoLivre = String.valueOf(message.getPedidoId())
                .replace("-", "")
                .substring(0, 25);

        return String.format("%s9%d%04d%010d%s",
                BANCO_CODIGO,
                RANDOM.nextInt(9) + 1,
                fatorVencimento,
                valorEmCentavos,
                campoLivre
        );
    }

    private String formatarLinhaDigitavel(String codigoBarras) {
        if (codigoBarras.length() < 44) {
            return codigoBarras;
        }
        return String.format("%s.%s %s.%s %s.%s %s %s",
                codigoBarras.substring(0, 5),
                codigoBarras.substring(5, 10),
                codigoBarras.substring(10, 15),
                codigoBarras.substring(15, 21),
                codigoBarras.substring(21, 26),
                codigoBarras.substring(26, 32),
                codigoBarras.substring(32, 33),
                codigoBarras.substring(33)
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