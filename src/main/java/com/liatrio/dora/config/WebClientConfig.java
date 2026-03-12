package com.liatrio.dora.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // GitHub action-run pages can be large; raise the codec buffer to 10 MB
    // to avoid DataBufferLimitException (which surfaces as WebClientResponseException 200).
    private static final int CODEC_MAX_BYTES = 10 * 1024 * 1024;

    @Bean
    public WebClient githubWebClient(WebClient.Builder builder) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(ClientCodecConfigurer::defaultCodecs)
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(CODEC_MAX_BYTES))
                .build();

        return builder
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .exchangeStrategies(strategies)
                .build();
    }
}
