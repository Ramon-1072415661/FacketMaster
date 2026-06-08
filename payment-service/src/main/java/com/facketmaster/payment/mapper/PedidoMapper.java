package com.facketmaster.payment.mapper;

import com.facketmaster.payment.controller.request.CriarPedidoRequest;
import com.facketmaster.payment.controller.response.PedidoResponse;
import com.facketmaster.payment.entity.Ingresso;
import com.facketmaster.payment.entity.Pagamento;
import com.facketmaster.payment.entity.Pedido;
import com.facketmaster.payment.messaging.PedidoPagamentoMessage;
import com.facketmaster.payment.service.PedidoStatusCache;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PedidoMapper {

    public PedidoResponse toResponse(Pedido pedido, Pagamento pagamento, List<Ingresso> ingressos) {
        return PedidoResponse.builder()
                .pedidoId(pedido.getId())
                .eventoId(pedido.getEventoId())
                .usuarioId(pedido.getUsuarioId())
                .quantidade(pedido.getQuantidade())
                .valorUnitario(pedido.getValorUnitario())
                .valorTotal(pedido.getValorTotal())
                .metodoPagamento(pedido.getMetodoPagamento())
                .statusPedido(pedido.getStatusPedido())
                .statusPagamento(pagamento.getStatusPagamento())
                .pagamentoId(pagamento.getId())
                .codigoTransacao(pagamento.getCodigoTransacao())
                .codigoPagamento(pagamento.getCodigoPagamento())
                .urlPagamento(pagamento.getUrlPagamento())
                .dataExpiracao(pagamento.getDataExpiracao())
                .motivoRecusa(pagamento.getMotivoRecusa())
                .ingressos(toIngressoResponse(ingressos))
                .criadoEm(pedido.getCriadoEm())
                .atualizadoEm(pedido.getAtualizadoEm())
                .build();
    }

    public PedidoResponse fromCache(PedidoStatusCache cache) {
        List<PedidoResponse.IngressoResponse> ingressos = cache.getIngressos() == null
                ? List.of()
                : cache.getIngressos().stream()
                .map(i -> PedidoResponse.IngressoResponse.builder()
                        .ingressoId(i.getIngressoId())
                        .codigoIngresso(i.getCodigoIngresso())
                        .statusIngresso(i.getStatusIngresso())
                        .build())
                .toList();

        return PedidoResponse.builder()
                .pedidoId(cache.getPedidoId())
                .eventoId(cache.getEventoId())
                .usuarioId(cache.getUsuarioId())
                .quantidade(cache.getQuantidade())
                .valorTotal(cache.getValorTotal())
                .metodoPagamento(cache.getMetodoPagamento())
                .statusPedido(cache.getStatusPedido())
                .statusPagamento(cache.getStatusPagamento())
                .pagamentoId(cache.getPagamentoId())
                .codigoTransacao(cache.getCodigoTransacao())
                .codigoPagamento(cache.getCodigoPagamento())
                .urlPagamento(cache.getUrlPagamento())
                .dataExpiracao(cache.getDataExpiracao())
                .motivoRecusa(cache.getMotivoRecusa())
                .ingressos(ingressos)
                .criadoEm(cache.getCriadoEm())
                .atualizadoEm(cache.getAtualizadoEm())
                .build();
    }

    public PedidoPagamentoMessage toMessage(Pedido pedido, CriarPedidoRequest request) {
        PedidoPagamentoMessage.PedidoPagamentoMessageBuilder builder = PedidoPagamentoMessage.builder()
                .pedidoId(pedido.getId())
                .eventoId(pedido.getEventoId())
                .usuarioId(pedido.getUsuarioId())
                .quantidade(pedido.getQuantidade())
                .valorTotal(pedido.getValorTotal())
                .metodoPagamento(pedido.getMetodoPagamento());

        if (request.getDadosCartao() != null) {
            CriarPedidoRequest.DadosCartaoRequest c = request.getDadosCartao();
            String mascarado = "****.**** .****." + c.getNumero().substring(c.getNumero().length() - 4);
            builder.dadosCartao(PedidoPagamentoMessage.DadosCartao.builder()
                    .numeroMascarado(mascarado)
                    .nomeTitular(c.getNomeTitular())
                    .validade(c.getValidade())
                    .parcelas(c.getParcelas())
                    .bandeira(c.getBandeira())
                    .build());
        }

        return builder.build();
    }

    private List<PedidoResponse.IngressoResponse> toIngressoResponse(List<Ingresso> ingressos) {
        if (ingressos == null) return List.of();
        return ingressos.stream()
                .map(i -> PedidoResponse.IngressoResponse.builder()
                        .ingressoId(i.getId())
                        .codigoIngresso(i.getCodigoIngresso())
                        .statusIngresso(i.getStatusIngresso().name())
                        .emitidoEm(i.getEmitidoEm())
                        .build())
                .toList();
    }
}