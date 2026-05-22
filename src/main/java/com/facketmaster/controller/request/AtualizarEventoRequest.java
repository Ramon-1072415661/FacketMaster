package com.facketmaster.controller.request;

import com.ingressos.eventservice.model.Evento.StatusEvento;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtualizarEventoRequest {

    @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
    private String nome;

    @Future(message = "A data do evento deve ser no futuro")
    private LocalDateTime dataEvento;

    @DecimalMin(value = "0.0", inclusive = false, message = "O valor deve ser maior que zero")
    @Digits(integer = 8, fraction = 2, message = "Valor com formato inválido")
    private BigDecimal valor;

    @Min(value = 0, message = "A quantidade não pode ser negativa")
    private Integer quantidadeDisponivel;

    @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres")
    private String descricao;

    @Size(max = 200, message = "O local pode ter no máximo 200 caracteres")
    private String local;

    private StatusEvento status;
}
