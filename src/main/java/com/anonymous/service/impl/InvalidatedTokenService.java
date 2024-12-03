package com.anonymous.service.impl;

import com.anonymous.entity.InvalidatedToken;
import com.anonymous.repository.IInvalidatedTokenRepository;
import com.anonymous.service.IInvalidatedTokenService;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvalidatedTokenService implements IInvalidatedTokenService {

    IInvalidatedTokenRepository invalidatedTokenRepository;

    @Override
    public void insert(SignedJWT signedJWT) throws ParseException {
        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        Instant expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime().toInstant();

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jwtId)
                .expiryTime(expiryTime)
                .build();
        invalidatedTokenRepository.save(invalidatedToken);
    }
}
