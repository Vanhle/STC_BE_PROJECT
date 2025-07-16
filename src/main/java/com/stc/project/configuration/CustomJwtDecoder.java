package com.stc.project.configuration;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.stc.project.exception.AppException;
import com.stc.project.service.serviceImpl.AuthenticationImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CustomJwtDecoder implements JwtDecoder {
    @Value("${jwt.signed_key}")
    private String signerKey;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    private final AuthenticationImpl authenticationImpl;

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            SignedJWT signedJWT = authenticationImpl.verifyToken(token);

            if (Objects.isNull(nimbusJwtDecoder)) {
                SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
                nimbusJwtDecoder = NimbusJwtDecoder
                        .withSecretKey(secretKeySpec)
                        .macAlgorithm(MacAlgorithm.HS512)
                        .build();
            }
            return nimbusJwtDecoder.decode(token);
        } catch (AppException e) {
            throw new JwtException("Unauthenticated");
        } catch (JOSEException | ParseException e) {
            throw new JwtException(e.getMessage());
        }
    }
}

