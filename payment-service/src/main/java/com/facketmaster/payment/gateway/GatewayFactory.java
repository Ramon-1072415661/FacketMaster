package com.facketmaster.payment.gateway;

import com.facketmaster.payment.enums.MetodoPagamento;
import com.facketmaster.payment.gateway.mock.BoletoGatewayMock;
import com.facketmaster.payment.gateway.mock.CartaoCreditoGatewayMock;
import com.facketmaster.payment.gateway.mock.PixGatewayMock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GatewayFactory {

    private final CartaoCreditoGatewayMock cartaoCreditoGateway;
    private final PixGatewayMock pixGateway;
    private final BoletoGatewayMock boletoGateway;

    public GatewayPagamento resolver(MetodoPagamento metodo) {
        return switch (metodo) {
            case CARTAO_CREDITO -> cartaoCreditoGateway;
            case PIX -> pixGateway;
            case BOLETO -> boletoGateway;
        };
    }
}