package com.facketmaster.event.repository;

import com.facketmaster.event.entity.ResultadoProcessado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ResultadoProcessadoRepository extends JpaRepository<ResultadoProcessado, UUID> {
}
