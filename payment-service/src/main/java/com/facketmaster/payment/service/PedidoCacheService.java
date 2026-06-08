package com.facketmaster.payment.service;

import com.facketmaster.payment.entity.Ingresso;
import com.facketmaster.payment.entity.Pagamento;
import com.facketmaster.payment.entity.Pedido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Gerencia o cache Redis para consultas rápidas de status de pedidos.
 * <p>
 * Padrão de chave: pedido:{uuid}
 * TTL configurável via app.payment.cache-ttl-seconds
 * <p>
 * Toda atualização de status persiste no banco e atualiza o cache.
 * Consultas de status leem do cache primeiro; só vão ao banco se o cache miss.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoCacheService {

    private static final String KEY_PREFIX = "pedido:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.payment.cache-ttl-seconds:600}")
    private long cacheTtlSeconds;

    public void atualizarCache(Pedido pedido, Pagamento pagamento, List<Ingresso> ingressos) {
        String key = KEY_PREFIX + pedido.getId();

        List<PedidoStatusCache.IngressoInfo> ingressosInfo = ingressos.stream()
                .map(i -> PedidoStatusCache.IngressoInfo.builder()
                        .ingressoId(i.getId())
                        .codigoIngresso(i.getCodigoIngresso())
                        .statusIngresso(i.getStatusIngresso().name())
                        .build())
                .toList();

        PedidoStatusCache cache = PedidoStatusCache.builder()
                .pedidoId(pedido.getId())
                .eventoId(pedido.getEventoId())
                .usuarioId(pedido.getUsuarioId())
                .quantidade(pedido.getQuantidade())
                .valorTotal(pedido.getValorTotal())
                .metodoPagamento(pedido.getMetodoPagamento())
                .statusPedido(pedido.getStatusPedido())
                .pagamentoId(pagamento.getId())
                .statusPagamento(pagamento.getStatusPagamento())
                .codigoTransacao(pagamento.getCodigoTransacao())
                .codigoPagamento(pagamento.getCodigoPagamento())
                .urlPagamento(pagamento.getUrlPagamento())
                .dataExpiracao(pagamento.getDataExpiracao())
                .motivoRecusa(pagamento.getMotivoRecusa())
                .ingressos(ingressosInfo)
                .criadoEm(pedido.getCriadoEm())
                .atualizadoEm(pedido.getAtualizadoEm())
                .build();

        try {
            redisTemplate.opsForValue().set(key, cache, Duration.ofSeconds(cacheTtlSeconds));
            log.debug("[CACHE] Pedido atualizado no Redis | key={} status={}", key, pedido.getStatusPedido());
        } catch (Exception ex) {
            log.error("[CACHE] Falha ao atualizar Redis | key={} erro={}", key, ex.getMessage());
        }
    }

    public Optional<PedidoStatusCache> buscarPorId(String pedidoId) {
        String key = KEY_PREFIX + pedidoId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof PedidoStatusCache status) {
                log.debug("[CACHE] Cache HIT | key={}", key);
                return Optional.of(status);
            }
        } catch (Exception ex) {
            log.warn("[CACHE] Falha ao ler Redis | key={} erro={}", key, ex.getMessage());
        }
        log.debug("[CACHE] Cache MISS | key={}", key);
        return Optional.empty();
    }

    public void invalidar(String pedidoId) {
        String key = KEY_PREFIX + pedidoId;
        try {
            redisTemplate.delete(key);
            log.debug("[CACHE] Cache invalidado | key={}", key);
        } catch (Exception ex) {
            log.warn("[CACHE] Falha ao invalidar cache | key={} erro={}", key, ex.getMessage());
        }
    }
}
