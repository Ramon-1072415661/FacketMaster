package com.facketmaster.controller;

import com.ingressos.eventservice.dto.*;
import com.ingressos.eventservice.service.EventoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService service;

    @PostMapping
    public ResponseEntity<EventoResponse> criar(@Valid @RequestBody CriarEventoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<EventoResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false, defaultValue = "false") boolean apenasDisponiveis) {

        List<EventoResponse> eventos;
        if (nome != null && !nome.isBlank()) {
            eventos = service.buscarPorNome(nome);
        } else if (apenasDisponiveis) {
            eventos = service.listarDisponiveis();
        } else {
            eventos = service.listarTodos();
        }
        return ResponseEntity.ok(eventos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EventoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarEventoRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @PatchMapping("/{id}/quantidade")
    public ResponseEntity<EventoResponse> atualizarQuantidade(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        Integer quantidade = body.get("quantidade");
        if (quantidade == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(service.atualizarQuantidade(id, quantidade));
    }

    @PatchMapping("/{id}/preco")
    public ResponseEntity<EventoResponse> atualizarPreco(
            @PathVariable Long id,
            @RequestBody Map<String, java.math.BigDecimal> body) {
        java.math.BigDecimal valor = body.get("valor");
        if (valor == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(service.atualizar(id, AtualizarEventoRequest.builder().valor(valor).build()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
