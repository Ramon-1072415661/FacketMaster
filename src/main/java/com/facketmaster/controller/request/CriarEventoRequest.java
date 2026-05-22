package com.ingressos.eventservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriarEventoRequest {

    @NotBlank(message = "O nome do evento é obrigatório")
    @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
    private String nome;

    @NotNull(message = "A data do evento é obrigatória")
    @Future(message = "A data do evento deve ser no futuro")
    private LocalDateTime dataEvento;

    @NotNull(message = "O valor é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "O valor deve ser maior que zero")
    @Digits(integer = 8, fraction = 2, message = "Valor com formato inválido")
    private BigDecimal valor;

    @NotNull(message = "A quantidade disponível é obrigatória")
    @Min(value = 1, message = "A quantidade deve ser pelo menos 1")
    private Integer quantidadeDisponivel;

    @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres")
    private String descricao;

    @Size(max = 200, message = "O local pode ter no máximo 200 caracteres")
    private String local;
}
