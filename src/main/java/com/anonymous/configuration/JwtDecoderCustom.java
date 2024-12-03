package com.anonymous.configuration;

import com.anonymous.exception.ErrorCode;
import com.anonymous.service.IAuthService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtDecoderCustom implements JwtDecoder {

    IAuthService authService;

    @NonFinal
    NimbusJwtDecoder nimbusJwtDecoder = null;

    @NonFinal
    @Value("${com.anonymous.jwt.secret-key}")
    String SECRET_KEY;

    @Override
    public Jwt decode(String token) throws JwtException {
        boolean isTokenValid = authService.introspectToken(token);
        if (!isTokenValid) {
            throw new JwtException(ErrorCode.TOKEN_INVALID.getMessage());
        }

        if (Objects.isNull(nimbusJwtDecoder)) {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "HS512");
            nimbusJwtDecoder = NimbusJwtDecoder
                    .withSecretKey(keySpec)
                    .macAlgorithm(MacAlgorithm.HS512)
                    .build();
        }

        return nimbusJwtDecoder.decode(token);
    }
}
