package com.facketmaster.event.mapper;

import com.facketmaster.event.controller.request.AtualizarEventoRequest;
import com.facketmaster.event.controller.request.CriarEventoRequest;
import com.facketmaster.event.controller.response.EventoResponse;
import com.facketmaster.event.entity.Evento;
import org.springframework.stereotype.Component;

@Component
public class EventoMapper {

    public Evento toModel(CriarEventoRequest req) {
        return Evento.builder()
                .nome(req.name())
                .dataEvento(req.eventDate())
                .valor(req.ticketPrice())
                .quantidadeDisponivel(req.totalTicketsAmount())
                .quantidadeTotal(req.totalTicketsAmount())
                .descricao(req.description())
                .local(req.eventLocation())
                .status(Evento.StatusEvento.ATIVO)
                .build();
    }

    public EventoResponse toResponse(Evento evento) {
        return EventoResponse.builder()
                .id(evento.getId())
                .nome(evento.getNome())
                .dataEvento(evento.getDataEvento())
                .valor(evento.getValor())
                .quantidadeDisponivel(evento.getQuantidadeDisponivel())
                .quantidadeTotal(evento.getQuantidadeTotal())
                .descricao(evento.getDescricao())
                .local(evento.getLocal())
                .status(evento.getStatus())
                .criadoEm(evento.getCriadoEm())
                .atualizadoEm(evento.getAtualizadoEm())
                .build();
    }

    public void aplicarAtualizacao(AtualizarEventoRequest request, Evento evento) {
        if (request.getNome() != null) evento.setNome(request.getNome());
        if (request.getDataEvento() != null) evento.setDataEvento(request.getDataEvento());
        if (request.getValor() != null) evento.setValor(request.getValor());
        if (request.getDescricao() != null) evento.setDescricao(request.getDescricao());
        if (request.getLocal() != null) evento.setLocal(request.getLocal());
        if (request.getStatus() != null) evento.setStatus(request.getStatus());

        if (request.getQuantidadeDisponivel() != null) {
            evento.setQuantidadeDisponivel(request.getQuantidadeDisponivel());
            if (request.getQuantidadeDisponivel() > 0
                    && evento.getStatus() == Evento.StatusEvento.ESGOTADO) {
                evento.setStatus(Evento.StatusEvento.ATIVO);
            }
            if (request.getQuantidadeDisponivel() == 0) {
                evento.setStatus(Evento.StatusEvento.ESGOTADO);
            }
        }
    }
}
