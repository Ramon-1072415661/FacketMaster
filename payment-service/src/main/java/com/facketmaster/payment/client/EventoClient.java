package com.facketmaster.payment.client;

import com.facketmaster.payment.exception.EstoqueInsuficienteException;
import com.facketmaster.payment.exception.EventoNotFoundException;
import com.facketmaster.payment.exception.EventoServiceIndisponivelException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Cliente HTTP para o event-service, usado para reservar e liberar
 * ingressos de forma síncrona e atômica, evitando overselling.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventoClient {

    private final RestTemplate restTemplate;

    @Value("${app.event-service.base-url}")
    private String baseUrl;

    /**
     * Reserva {@code quantidade} ingressos do evento, de forma atômica.
     * Deve ser chamado ANTES de persistir o pedido.
     *
     * @throws EstoqueInsuficienteException se não houver ingressos disponíveis
     * @throws EventoNotFoundException      se o evento não existir ou não estiver ativo
     * @throws EventoServiceIndisponivelException se o event-service não responder
     */
    public void reservar(Long eventoId, Integer quantidade, String authorizationHeader) {
        String url = baseUrl + "/api/v1/eventos/" + eventoId + "/reserva";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authorizationHeader != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("quantidade", quantidade), headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            log.info("[EVENTO-CLIENT] Reserva confirmada | eventoId={} quantidade={}", eventoId, quantidade);
        } catch (HttpStatusCodeException ex) {
            if (HttpStatus.CONFLICT.equals(ex.getStatusCode())) {
                log.warn("[EVENTO-CLIENT] Estoque insuficiente | eventoId={} quantidade={}", eventoId, quantidade);
                throw new EstoqueInsuficienteException(
                        "Ingressos insuficientes para o evento " + eventoId);
            }
            if (HttpStatus.NOT_FOUND.equals(ex.getStatusCode())) {
                throw new EventoNotFoundException(eventoId);
            }
            log.error("[EVENTO-CLIENT] Erro ao reservar ingressos | eventoId={} status={} body={}",
                    eventoId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new EventoServiceIndisponivelException(
                    "Falha ao reservar ingressos para o evento " + eventoId, ex);
        } catch (ResourceAccessException ex) {
            log.error("[EVENTO-CLIENT] event-service indisponível | eventoId={}", eventoId, ex);
            throw new EventoServiceIndisponivelException(
                    "event-service indisponível ao tentar reservar ingressos", ex);
        }
    }

    /**
     * Libera {@code quantidade} ingressos previamente reservados para o evento.
     * Usado como compensação quando, após a reserva, a criação do pedido falha.
     */
    public void liberar(Long eventoId, Integer quantidade, String authorizationHeader) {
        String url = baseUrl + "/api/v1/eventos/" + eventoId + "/liberar";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authorizationHeader != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("quantidade", quantidade), headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            log.info("[EVENTO-CLIENT] Reserva liberada | eventoId={} quantidade={}", eventoId, quantidade);
        } catch (HttpStatusCodeException | ResourceAccessException ex) {
            log.error("[EVENTO-CLIENT] Falha ao liberar ingressos | eventoId={} quantidade={} erro={}",
                    eventoId, quantidade, ex.getMessage(), ex);
            throw new EventoServiceIndisponivelException(
                    "Falha ao liberar ingressos do evento " + eventoId, ex);
        }
    }
}
