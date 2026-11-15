package com.challenge.department_store.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Objects;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class JwtSecurityConfiguration {

    @Bean
    private SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${app.security.jwt.enabled:false}") boolean jwtEnabled) throws Exception {
        
        http.csrf(AbstractHttpConfigurer::disable).sessionManagement(
                session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        if (jwtEnabled) {
            http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
        } else {
            http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        }

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.jwt.enabled", havingValue = "true")
    private JwtDecoder jwtDecoder(
            @Value("${app.security.jwt.jwk-set-file:}") String jwkSetFile,
            @Value("${app.security.jwt.issuer:}") String issuer,
            @Value("${app.security.jwt.audience:}") String audience) {

        if (jwkSetFile.isBlank()) {
            throw new IllegalStateException(
                    "app.security.jwt.jwk-set-file must be configured when JWT authentication is enabled."
            );
        }
        if (issuer.isBlank() || audience.isBlank()) {
            throw new IllegalStateException(
                    "app.security.jwt.issuer and app.security.jwt.audience must be configured when JWT authentication is enabled."
            );
        }

        Path jwkSetPath = Path.of(jwkSetFile);
        if (!Files.isRegularFile(jwkSetPath) || !Files.isReadable(jwkSetPath)) {
            throw new IllegalStateException("Configured JWK set file is missing or unreadable: " + jwkSetPath);
        }

        JWKSet jwkSet;
        try {
            jwkSet = JWKSet.load(jwkSetPath.toFile());
        } catch (IOException | ParseException exception) {
            throw new IllegalStateException("Configured JWK set file is invalid: " + jwkSetPath, exception);
        }

        if (jwkSet.getKeys().isEmpty()
                || jwkSet.getKeys().stream().anyMatch(JWK::isPrivate)
                || jwkSet.getKeys().stream().noneMatch(key -> key.getKeyType().getValue().equals("RSA"))) {
            throw new IllegalStateException(
                    "JWK set must contain at least one public RSA signing key and no private key material"
            );
        }

        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);
        DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource)
        );

        NimbusJwtDecoder decoder = new NimbusJwtDecoder(jwtProcessor);
        OAuth2TokenValidator<Jwt> requiredExpiration = jwt -> Objects.nonNull(jwt.getExpiresAt())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "The token must contain an expiration claim", null)
                );
        OAuth2TokenValidator<Jwt> requiredAudience = jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "The token audience is not accepted", null)
                );
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                requiredExpiration,
                requiredAudience
        ));
        return decoder;
    }
}
