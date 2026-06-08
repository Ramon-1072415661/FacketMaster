package com.facketmaster.config;

import com.facketmaster.controller.response.JwtTokenResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtValidationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (Strings.isNotEmpty(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring("Bearer ".length());

            Optional<JwtTokenResponse> optToken = tokenService.verifyToken(token);

            if (optToken.isPresent()) {
                JwtTokenResponse tokenData = optToken.get();

                List<SimpleGrantedAuthority> authorities = List.of();
                if (tokenData.role() != null) {
                    authorities = List.of(new SimpleGrantedAuthority("ROLE_" + tokenData.role()));
                }

                UsernamePasswordAuthenticationToken authUser =
                        new UsernamePasswordAuthenticationToken(tokenData, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authUser);
            }
        }

        filterChain.doFilter(request, response);
    }
}
