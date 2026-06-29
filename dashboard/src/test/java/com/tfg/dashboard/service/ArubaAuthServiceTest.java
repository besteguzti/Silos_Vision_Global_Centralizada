package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;

import com.tfg.dashboard.model.OAuthToken;
import com.tfg.dashboard.repository.OAuthTokenRepository;

@ExtendWith(MockitoExtension.class)
class ArubaAuthServiceTest {

    @Mock
    private OAuthTokenRepository tokenRepository;

    @Test
    void unauthorizedTokenRefreshFailsInControlledWay() {

        AtomicReference<MockRestServiceServer> server =
                new AtomicReference<>();
        ArubaAuthService service =
                serviceWithMockServer(server);

        when(tokenRepository.findById(1L))
                .thenReturn(Optional.of(expiredToken()));

        server.get()
                .expect(requestTo("https://aruba.example/oauth2/token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(service::getAccessToken)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se pudo renovar el token Aruba");

        server.get().verify();
    }

    @Test
    void rejectsEmptyTokenRefreshResponse() {

        AtomicReference<MockRestServiceServer> server =
                new AtomicReference<>();
        ArubaAuthService service =
                serviceWithMockServer(server);

        when(tokenRepository.findById(1L))
                .thenReturn(Optional.of(expiredToken()));

        server.get()
                .expect(requestTo("https://aruba.example/oauth2/token"))
                .andRespond(withNoContent());

        assertThatThrownBy(service::getAccessToken)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Respuesta vacia renovando token Aruba");

        server.get().verify();
    }

    @Test
    void handlesRefreshConnectionErrors() {

        RestTemplateBuilder builder =
                new RestTemplateBuilder()
                        .additionalInterceptors((request, body, execution) -> {
                            throw new ResourceAccessException("timeout");
                        });

        ArubaAuthService service =
                service(builder);

        when(tokenRepository.findById(1L))
                .thenReturn(Optional.of(expiredToken()));

        assertThatThrownBy(service::getAccessToken)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se pudo conectar con Aruba");
    }

    private ArubaAuthService serviceWithMockServer(
            AtomicReference<MockRestServiceServer> server
    ) {

        RestTemplateBuilder builder =
                new RestTemplateBuilder()
                        .additionalCustomizers(restTemplate ->
                                server.set(
                                        MockRestServiceServer
                                                .bindTo(restTemplate)
                                                .build()
                                )
                        );

        return service(builder);
    }

    private ArubaAuthService service(RestTemplateBuilder builder) {

        ArubaAuthService service =
                new ArubaAuthService(tokenRepository,builder);

        ReflectionTestUtils.setField(service,"baseUrl","https://aruba.example");
        ReflectionTestUtils.setField(service,"clientId","client-id");
        ReflectionTestUtils.setField(service,"clientSecret","client-secret");

        return service;
    }

    private OAuthToken expiredToken() {

        OAuthToken token =
                new OAuthToken();
        token.setAccessToken("old-access-token");
        token.setRefreshToken("refresh-token");
        token.setExpiresAt(0L);

        return token;
    }
}
