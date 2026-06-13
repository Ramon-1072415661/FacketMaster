package com.facketmaster.payment.controller;

import com.facketmaster.controller.response.JwtTokenResponse;
import com.facketmaster.payment.controller.request.CriarPedidoRequest;
import com.facketmaster.payment.controller.response.PedidoResponse;
import com.facketmaster.payment.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(
            @Valid @RequestBody CriarPedidoRequest request,
            @AuthenticationPrincipal JwtTokenResponse user,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        log.info(
                "[API] POST /pedidos | eventoId={} usuarioId={} metodo={}",
                request.getEventoId(),
                user.id(),
                request.getMetodoPagamento());

        PedidoResponse response = pedidoService.criar(request, user.id().toString(), authorizationHeader);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscarPorId(@PathVariable("id") UUID id) {

        return ResponseEntity.ok(pedidoService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listarPorUsuario(
            @AuthenticationPrincipal JwtTokenResponse user) {

        return ResponseEntity.ok(pedidoService.listarPorUsuario(user.id().toString()));
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<PedidoResponse> confirmarPagamento(
            @PathVariable("id") UUID id) {

        log.info("[API] POST /pedidos/{}/confirmar (webhook simulado)", id);

        return ResponseEntity.ok(pedidoService.confirmarPagamento(id));
    }
}