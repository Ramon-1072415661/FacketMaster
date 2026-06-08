package com.facketmaster.payment.gateway;

import com.facketmaster.payment.messaging.PedidoPagamentoMessage;

public interface GatewayPagamento {

    ResultadoGateway processar(PedidoPagamentoMessage message);

    record ResultadoGateway(
            boolean aprovado,
            String codigoTransacao,
            String codigoPagamento,
            String urlPagamento,
            String motivoRecusa,
            java.time.LocalDateTime dataExpiracao
    ) {

        public static ResultadoGateway aprovado(String codigoTransacao,
                                                String codigoPagamento,
                                                String urlPagamento) {
            return new ResultadoGateway(true, codigoTransacao, codigoPagamento,
                    urlPagamento, null, null);
        }

        public static ResultadoGateway aprovadoComExpiracao(String codigoTransacao,
                                                            String codigoPagamento,
                                                            java.time.LocalDateTime dataExpiracao) {
            return new ResultadoGateway(true, codigoTransacao, codigoPagamento,
                    null, null, dataExpiracao);
        }

        public static ResultadoGateway pendente(String codigoTransacao,
                                                String codigoPagamento,
                                                String urlPagamento,
                                                java.time.LocalDateTime dataExpiracao) {
            return new ResultadoGateway(false, codigoTransacao, codigoPagamento,
                    urlPagamento, null, dataExpiracao);
        }

        public static ResultadoGateway recusado(String motivo) {
            return new ResultadoGateway(false, null, null, null, motivo, null);
        }
    }
}