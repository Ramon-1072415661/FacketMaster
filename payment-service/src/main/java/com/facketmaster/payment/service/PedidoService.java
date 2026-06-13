package com.facketmaster.payment.service;

import com.facketmaster.payment.client.EventoClient;
import com.facketmaster.payment.controller.request.CriarPedidoRequest;
import com.facketmaster.payment.controller.response.PedidoResponse;
import com.facketmaster.payment.entity.Pagamento;
import com.facketmaster.payment.entity.Pedido;
import com.facketmaster.payment.enums.StatusPagamento;
import com.facketmaster.payment.enums.StatusPedido;
import com.facketmaster.payment.exception.PedidoNotFoundException;
import com.facketmaster.payment.mapper.PedidoMapper;
import com.facketmaster.payment.messaging.PaymentPublisher;
import com.facketmaster.payment.messaging.PedidoPagamentoMessage;
import com.facketmaster.payment.repository.PagamentoRepository;
import com.facketmaster.payment.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquestra a criação de pedidos e publicação na fila de mensageria.
 * <p>
 * Fluxo de criação:
 * 1. Persiste o Pedido com status AGUARDANDO_PAGAMENTO
 * 2. Cria o Pagamento inicial (PENDENTE)
 * 3. Publica PedidoPagamentoMessage na fila RabbitMQ
 * 4. Retorna o PedidoResponse imediatamente (resposta síncrona)
 * 5. O processamento real acontece de forma assíncrona no PaymentConsumer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final PedidoMapper mapper;
    private final PaymentPublisher publisher;
    private final PedidoCacheService cacheService;
    private final ProcessamentoPagamentoService processamentoService;
    private final EventoClient eventoClient;
    private final EmailService emailService;

    @Transactional
    public PedidoResponse criar(CriarPedidoRequest request, String usuarioId, String usuarioEmail, String authorizationHeader) {
        log.info("[PEDIDO] Criando pedido | eventoId={} usuario={} metodo={}",
                request.getEventoId(), usuarioId, request.getMetodoPagamento());

        eventoClient.reservar(request.getEventoId(), request.getQuantidade(), authorizationHeader);

        try {
            BigDecimal valorTotal = request.getValorUnitario()
                    .multiply(BigDecimal.valueOf(request.getQuantidade()));

            Pedido pedido = Pedido.builder()
                    .eventoId(request.getEventoId())
                    .usuarioId(usuarioId)
                    .usuarioEmail(usuarioEmail)
                    .quantidade(request.getQuantidade())
                    .valorUnitario(request.getValorUnitario())
                    .valorTotal(valorTotal)
                    .metodoPagamento(request.getMetodoPagamento())
                    .statusPedido(StatusPedido.AGUARDANDO_PAGAMENTO)
                    .build();

            pedido = pedidoRepository.save(pedido);
            emailService.notificar(usuarioEmail, pedido.getId(), StatusPedido.AGUARDANDO_PAGAMENTO);

            Pagamento pagamento = Pagamento.builder()
                    .pedido(pedido)
                    .metodoPagamento(request.getMetodoPagamento())
                    .statusPagamento(StatusPagamento.PENDENTE)
                    .tentativas(0)
                    .build();
            pagamento = pagamentoRepository.save(pagamento);

            cacheService.atualizarCache(pedido, pagamento, List.of());

            PedidoPagamentoMessage message = mapper.toMessage(pedido, request);

            // Publish only after the transaction commits so the consumer always
            // finds the Pedido in the DB (avoids the publish-before-commit race condition).
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.publicarPedido(message);
                }
            });

            log.info("[PEDIDO] Pedido criado e publicado na fila | pedidoId={}", pedido.getId());
            return mapper.toResponse(pedido, pagamento, List.of());
        } catch (Exception ex) {
            log.error("[PEDIDO] Falha ao criar pedido após reserva de ingressos, liberando reserva | eventoId={} quantidade={}",
                    request.getEventoId(), request.getQuantidade(), ex);
            eventoClient.liberar(request.getEventoId(), request.getQuantidade(), authorizationHeader);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(UUID id) {
        Optional<PedidoStatusCache> cached = cacheService.buscarPorId(id.toString());
        if (cached.isPresent()) {
            return mapper.fromCache(cached.get());
        }

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new PedidoNotFoundException(id));

        Pagamento pagamento = pagamentoRepository
                .findTopByPedidoIdOrderByCriadoEmDesc(id)
                .orElseThrow(() -> new PedidoNotFoundException(id));

        return mapper.toResponse(pedido, pagamento, pedido.getIngressos());
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPorUsuario(String usuarioId) {
        return pedidoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId)
                .stream()
                .map(p -> {
                    Optional<PedidoStatusCache> cached = cacheService.buscarPorId(p.getId().toString());
                    if (cached.isPresent()) return mapper.fromCache(cached.get());

                    Pagamento pag = pagamentoRepository
                            .findTopByPedidoIdOrderByCriadoEmDesc(p.getId())
                            .orElse(Pagamento.builder()
                                    .statusPagamento(StatusPagamento.PENDENTE).build());
                    return mapper.toResponse(p, pag, p.getIngressos());
                })
                .toList();
    }

    @Transactional
    public PedidoResponse confirmarPagamento(UUID pedidoId) {
        processamentoService.confirmarPagamento(pedidoId);
        return buscarPorId(pedidoId);
    }

    @Transactional
    public PedidoResponse cancelarPedido(UUID pedidoId) {
        processamentoService.cancelarPedido(pedidoId);
        return buscarPorId(pedidoId);
    }
}
