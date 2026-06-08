package com.facketmaster.controller.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErroResponse {

    private int status;
    private String erro;
    private String mensagem;
    private LocalDateTime timestamp;
}