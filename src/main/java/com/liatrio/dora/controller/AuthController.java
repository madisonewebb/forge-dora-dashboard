package com.liatrio.dora.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

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
     * Returns: { access_token, token_type, scope } on success,
     *          { error, error_description } while pending or on failure.
     */
    @GetMapping("/device/poll")
    public Mono<Map<String, Object>> pollDevice(@RequestParam String deviceCode) {
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
