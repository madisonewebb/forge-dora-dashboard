package com.liatrio.dora.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Proxies GitHub's Device Authorization Grant flow so the client_id stays
 * server-side and no client_secret is ever sent to the browser.
 *
 * Required setup:
 *  1. Register a GitHub OAuth App (Settings → Developer settings → OAuth Apps).
 *  2. Enable "Device flow" on the app's settings page.
 *  3. Set GITHUB_CLIENT_ID env var (no client_secret needed for device flow).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${github.oauth.client-id:}")
    private String clientId;

    private final WebClient webClient;

    public AuthController(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://github.com")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    /**
     * Step 1 — request a device code and user code from GitHub.
     * Returns: { device_code, user_code, verification_uri, expires_in, interval }
     */
    @PostMapping("/device/init")
    public Mono<Map<String, Object>> initDevice() {
        if (clientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "GitHub OAuth is not configured on this server (GITHUB_CLIENT_ID missing).");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("scope", "repo");

        return webClient.post()
                .uri("/login/device/code")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    /**
     * Step 2 — poll until the user authorizes (or the code expires).
     * Accepts { deviceCode } in the request body to avoid logging the code in access logs.
     * Returns: { access_token, token_type, scope } on success,
     *          { error, error_description } while pending or on failure.
     */
    @PostMapping("/device/poll")
    public Mono<Map<String, Object>> pollDevice(@RequestBody Map<String, String> body) {
        String deviceCode = body.get("deviceCode");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("device_code", deviceCode);
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:device_code");

        return webClient.post()
                .uri("/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }
}
