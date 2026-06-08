package com.facketmaster.payment.repository;

import com.facketmaster.payment.entity.Pedido;
import com.facketmaster.payment.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    List<Pedido> findByUsuarioIdOrderByCriadoEmDesc(String usuarioId);

    List<Pedido> findByEventoIdAndStatusPedido(Long eventoId, StatusPedido status);

    long countByEventoIdAndStatusPedido(Long eventoId, StatusPedido status);
}
