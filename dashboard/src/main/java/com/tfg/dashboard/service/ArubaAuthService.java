package com.tfg.dashboard.service;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.tfg.dashboard.model.OAuthToken;
import com.tfg.dashboard.repository.OAuthTokenRepository;

/**
 * Gestiona la autenticación OAuth contra Aruba Central.
 *
 * Lee el refresh token persistido en MySQL, renueva el access token cuando
 * caduca y usa timeouts para que un problema de red no bloquee indefinidamente
 * la sincronización.
 */
@Service
public class ArubaAuthService {

        private static final Logger log = LoggerFactory.getLogger(ArubaAuthService.class);

        @Value("${aruba.client.id}")
        private String clientId;

        @Value("${aruba.client.secret}")
        private String clientSecret;

        @Value("${aruba.base.url}")
        private String baseUrl;

        private final OAuthTokenRepository tokenRepository;
        private final RestTemplate restTemplate;

        public ArubaAuthService(
                        OAuthTokenRepository tokenRepository,
                        RestTemplateBuilder restTemplateBuilder
        ) {
                this.tokenRepository = tokenRepository;
                this.restTemplate = restTemplateBuilder
                                .setConnectTimeout(Duration.ofSeconds(10))
                                .setReadTimeout(Duration.ofSeconds(30))
                                .build();
        }

        /**
         * Devuelve un access token válido para llamar a Aruba Central.
         *
         * Si el token almacenado ha caducado, solicita uno nuevo y actualiza la
         * tabla OAuthToken. Los errores HTTP o de timeout se transforman en una
         * excepción controlada con log.
         */
        public String getAccessToken() {

                OAuthToken token = tokenRepository.findById(1L)
                                .orElseThrow(() -> new RuntimeException(
                                                "No existe token OAuth en MySQL"));

                long now = System.currentTimeMillis();

                // Si el token aún es válido, se reutiliza para evitar llamadas
                // innecesarias a Aruba.
                if (token.getExpiresAt() != null
                                && now < token.getExpiresAt()) {

                        return token.getAccessToken();
                }

                log.info("Renovando token Aruba");

                String url = baseUrl + "/oauth2/token";

                HttpHeaders headers = new HttpHeaders();

                headers.setContentType(
                                MediaType.APPLICATION_FORM_URLENCODED);

                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

                body.add("grant_type", "refresh_token");
                body.add("refresh_token", token.getRefreshToken());
                body.add("client_id", clientId);
                body.add("client_secret", clientSecret);

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

                try {

                        ResponseEntity<Map<String, Object>> response =
                                        restTemplate.exchange(
                                                        url,
                                                        HttpMethod.POST,
                                                        request,
                                                        new ParameterizedTypeReference<>() {
                                                        }
                                        );

                        Map<String, Object> responseBody =
                                        response.getBody();

                        if (responseBody == null) {
                                throw new IllegalStateException(
                                                "Respuesta vacia renovando token Aruba"
                                );
                        }

                        String newAccessToken =
                                        (String) responseBody.get("access_token");

                        String newRefreshToken =
                                        (String) responseBody.get("refresh_token");

                        Object expiresInValue =
                                        responseBody.get("expires_in");

                        if (newAccessToken == null || newAccessToken.isBlank()) {
                                throw new IllegalStateException(
                                                "La respuesta OAuth de Aruba no incluye access_token"
                                );
                        }

                        if (!(expiresInValue instanceof Number expiresIn)) {
                                throw new IllegalStateException(
                                                "La respuesta OAuth de Aruba no incluye expires_in valido"
                                );
                        }

                        token.setAccessToken(newAccessToken);

                        if (newRefreshToken != null && !newRefreshToken.isBlank()) {
                                token.setRefreshToken(newRefreshToken);
                        }

                        token.setExpiresAt(
                                        now + ((expiresIn.longValue() - 60) * 1000L)
                        );

                        tokenRepository.save(token);

                        return newAccessToken;

                } catch (HttpStatusCodeException exception) {

                        if (exception.getStatusCode().value() == 401
                                        || exception.getStatusCode().value() == 403) {
                                log.error(
                                                "Aruba rechaza la renovacion OAuth. Status: {}",
                                                exception.getStatusCode()
                                );
                        } else {
                                log.error(
                                                "Error HTTP renovando token Aruba. Status: {}",
                                                exception.getStatusCode(),
                                                exception
                                );
                        }

                        throw new IllegalStateException(
                                        "No se pudo renovar el token Aruba",
                                        exception
                        );

                } catch (ResourceAccessException exception) {

                        log.error(
                                        "Timeout o error de conexion renovando token Aruba",
                                        exception
                        );

                        throw new IllegalStateException(
                                        "No se pudo conectar con Aruba para renovar el token",
                                        exception
                        );
                }
        }
}

