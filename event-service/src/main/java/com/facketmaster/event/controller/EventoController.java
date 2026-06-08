package com.facketmaster.event.controller;

import com.facketmaster.event.controller.request.AtualizarEventoRequest;
import com.facketmaster.event.controller.request.CriarEventoRequest;
import com.facketmaster.event.controller.request.UpdateStatusRequest;
import com.facketmaster.event.controller.response.EventoResponse;
import com.facketmaster.event.service.EventoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService service;

    @PostMapping("/admin/create")
    public ResponseEntity<EventoResponse> criar(
            @Valid @RequestBody CriarEventoRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<EventoResponse>> listar(
            @RequestParam(value = "nome", required = false) String nome,
            @RequestParam(value = "apenasDisponiveis", required = false, defaultValue = "false") boolean apenasDisponiveis) {

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
    public ResponseEntity<EventoResponse> buscarPorId(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PatchMapping("/admin/{id}")
    public ResponseEntity<EventoResponse> atualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody AtualizarEventoRequest request) {

        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @PatchMapping("/admin/quantidade/{id}")
    public ResponseEntity<EventoResponse> atualizarQuantidade(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Integer> body) {

        Integer quantidade = body.get("quantidade");

        if (quantidade == null || quantidade < 0) {
            return ResponseEntity.badRequest().build();
        }

        return service.atualizarQuantidade(id, quantidade)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/admin/preco/{id}")
    public ResponseEntity<EventoResponse> atualizarPreco(
            @PathVariable("id") Long id,
            @RequestBody Map<String, BigDecimal> body) {

        BigDecimal valor = body.get("valor");

        if (valor == null) {
            return ResponseEntity.badRequest().build();
        }

        AtualizarEventoRequest request = AtualizarEventoRequest.builder()
                .valor(valor)
                .build();

        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable("id") Long id) {

        service.deletar(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/status/{id}")
    public ResponseEntity<EventoResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {
        EventoResponse response = service.updateStatus(id, request.status());
        return ResponseEntity.ok(response);
    }
}