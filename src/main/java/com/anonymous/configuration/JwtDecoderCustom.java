package com.anonymous.configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class JwtDecoderCustom implements JwtDecoder {

    @NonFinal
    @Value("${identity.service.jwks-uri:http://localhost:8080/.well-known/jwks.json}")
    String jwksUri;

    @NonFinal
    NimbusJwtDecoder jwksJwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            if (jwksJwtDecoder == null) {
                jwksJwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwksUri)
                        .jwsAlgorithms(algs -> {
                            algs.add(SignatureAlgorithm.RS512);
                            algs.add(SignatureAlgorithm.RS256);
                        })
                        .build();
            }
            return jwksJwtDecoder.decode(token);
        } catch (Exception e) {
            log.error("JWT validation via JWKS failed: {}", e.getMessage());
            throw new JwtException("Invalid or expired token: " + e.getMessage());
        }
    }
}
