package com.facketmaster.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.facketmaster.controller.response.JwtTokenResponse;
import com.facketmaster.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class TokenService {

    @Value("${JWT_SECRET}")
    private String secret;

    public String generateToken(User user) {
        Algorithm alg = Algorithm.HMAC256(secret);

        return JWT.create()
                .withSubject(user.getEmail())
                .withClaim("userId", user.getId())
                .withClaim("role", user.getRole().name())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(86400))
                .sign(alg);
    }

    public Optional<JwtTokenResponse> verifyToken(String token) {
        try {
            Algorithm alg = Algorithm.HMAC256(secret);

            DecodedJWT decode = JWT.require(alg)
                    .build()
                    .verify(token);

            return Optional.of(JwtTokenResponse.builder()
                    .email(decode.getSubject())
                    .id(decode.getClaim("userId").asLong())
                    .role(decode.getClaim("role").asString())
                    .build());

        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }
}
