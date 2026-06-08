package com.facketmaster.controller.response;

import lombok.Builder;

@Builder
public record JwtTokenResponse(Long id,
                               String email,
                               String role
) {
}
