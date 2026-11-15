package com.challenge.department_store.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class JwtSecurityConfigurationTest {

    private static final String ISSUER = "https://issuer.example.test";
    private static final String AUDIENCE = "department-store-api";
    private static final RSAKey SIGNING_KEY = createSigningKey();
    private static final Path JWKS_FILE = createJwksFile();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.enabled", () -> true);
        registry.add("app.security.jwt.jwk-set-file", () -> JWKS_FILE.toString());
        registry.add("app.security.jwt.issuer", () -> ISSUER);
        registry.add("app.security.jwt.audience", () -> AUDIENCE);
    }

    @AfterAll
    static void removeJwksFile() throws Exception {
        Files.deleteIfExists(JWKS_FILE);
    }

    @Test
    void rejectsRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenSignedByUntrustedKey() throws Exception {
        RSAKey otherKey = new com.nimbusds.jose.jwk.gen.RSAKeyGenerator(2048)
                .keyID("untrusted-key")
                .generate();

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + createToken(otherKey, Instant.now().plusSeconds(60))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsValidTokenAndAllowsApiAccess() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + createToken(SIGNING_KEY, Instant.now().plusSeconds(60))))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsTokenWithoutExpiration() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + createTokenWithoutExpiration()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWithIncorrectIssuer() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + createToken(
                                SIGNING_KEY,
                                "https://unexpected-issuer.example.test",
                                AUDIENCE,
                                Instant.now().plusSeconds(60)
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWithIncorrectAudience() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + createToken(
                                SIGNING_KEY,
                                ISSUER,
                                "another-api",
                                Instant.now().plusSeconds(60)
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + createToken(
                                SIGNING_KEY,
                                ISSUER,
                                AUDIENCE,
                                Instant.now().minusSeconds(60)
                        )))
                .andExpect(status().isUnauthorized());
    }

    private static RSAKey createSigningKey() {
        try {
            return new com.nimbusds.jose.jwk.gen.RSAKeyGenerator(2048)
                    .keyID("trusted-key")
                    .generate();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static Path createJwksFile() {
        try {
            Path file = Files.createTempFile("department-store-jwks-", ".json");
            Files.writeString(file, new JWKSet(SIGNING_KEY.toPublicJWK()).toString());
            return file;
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static String createToken(RSAKey key, Instant expiration) throws Exception {
        return createToken(key, ISSUER, AUDIENCE, expiration);
    }

    private static String createToken(
            RSAKey key,
            String issuer,
            String audience,
            Instant expiration) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("test-user")
                .issuer(issuer)
                .audience(List.of(audience))
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(expiration))
                .build();
        return signToken(key, claims);
    }

    private static String createTokenWithoutExpiration() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("test-user")
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE))
                .issueTime(Date.from(Instant.now()))
                .build();
        return signToken(SIGNING_KEY, claims);
    }

    private static String signToken(RSAKey key, JWTClaimsSet claims) throws Exception {
        SignedJWT token = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(key.getKeyID())
                        .type(JOSEObjectType.JWT)
                        .build(),
                claims
        );
        token.sign(new RSASSASigner(key));
        return token.serialize();
    }
}
