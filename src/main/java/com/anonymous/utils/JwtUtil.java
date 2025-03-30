package com.anonymous.utils;

import com.anonymous.entity.Role;
import com.anonymous.entity.User;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IInvalidatedTokenRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtUtil {

    IInvalidatedTokenRepository invalidatedTokenRepository;

    @NonFinal
    @Value("${com.anonymous.jwt.valid-duration}")
    Long VALID_DURATION;

    @NonFinal
    @Value("${com.anonymous.jwt.refreshable-duration}")
    Long REFRESHABLE_DURATION;

    @NonFinal
    @Value("${com.anonymous.jwt.secret-key}")
    String SECRET_KEY;


    public String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("com.anonymous")
                .subject(getIdFromUser(user))
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .claim("scope", buildScope(user.getRoles()))
                .claim("username", user.getUsername())
                .claim("refreshable_time", new Date(Instant.now().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .build();
        Payload payload = claims.toPayload();

        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            JWSSigner signer = new MACSigner(SECRET_KEY.getBytes());
            jwsObject.sign(signer);
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private String buildScope(Set<Role> roles) {
        return CollectionUtils.isEmpty(roles)
                ? ""
                : roles.stream()
                .map(role -> "ROLE_" + role.getCode())
                .collect(Collectors.joining(" "));
    }

    private String getIdFromUser(User user) {
        boolean isCustomer = user.getRoles().stream()
                .anyMatch(role -> role.getCode().equals("CUSTOMER"));
        if (isCustomer) {
            return user.getCustomer().getId();
        }
        return user.getEmployee().getId();
    }

    public SignedJWT verifyToken(String token, boolean isRefresh) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);

        boolean isTokenValid = isTokenValid(signedJWT);
        if (!isTokenValid) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        boolean isExpired = isTokenExpired(isRefresh, signedJWT);
        if (isExpired) {
            throw new AppException(ErrorCode.EXPIRATION_TOKEN);
        }

        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        boolean isTokenExistedInBlackList = invalidatedTokenRepository.existsById(jwtId);
        if (isTokenExistedInBlackList) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        
        return signedJWT;
    }

    private boolean isTokenValid(SignedJWT signedJWT) throws JOSEException {
        JWSVerifier verifier = new MACVerifier(SECRET_KEY.getBytes());
        return signedJWT.verify(verifier);
    }

    private boolean isTokenExpired(boolean isRefresh, SignedJWT signedJWT) throws ParseException {
        Date expirationTime = isRefresh
                ? signedJWT.getJWTClaimsSet().getDateClaim("refreshable_time")
                : signedJWT.getJWTClaimsSet().getExpirationTime();
        return expirationTime.before(new Date());
    }

}
