package com.facketmaster.payment.repository;

import com.facketmaster.payment.entity.Ingresso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IngressoRepository extends JpaRepository<Ingresso, UUID> {

    List<Ingresso> findByUsuarioIdOrderByEmitidoEmDesc(String usuarioId);

    List<Ingresso> findByPedidoId(UUID pedidoId);

    Optional<Ingresso> findByCodigoIngresso(String codigoIngresso);
}
