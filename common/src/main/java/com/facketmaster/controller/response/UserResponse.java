package com.facketmaster.controller.response;

import com.facketmaster.entity.Role;
import lombok.Builder;

@Builder
public record UserResponse(Long id,
                           String name,
                           String email,
                           Role role
) {
}