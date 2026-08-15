package de.agiehl.bgstats.config;

import de.agiehl.bgg.BggClient;
import de.agiehl.bgg.config.BggClientConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BggClientConfiguration {

    @Bean
    BggClient bggClient(BggProperties properties) {
        BggProperties.Client client = properties.client();
        BggClientConfig config = BggClientConfig.builder()
                .apiKey(properties.apiKey() == null ? "" : properties.apiKey())
                .baseUri(client.baseUri())
                .apiKeyParameter(client.apiKeyParameter())
                .userAgent(client.userAgent())
                .connectTimeout(client.connectTimeout())
                .requestTimeout(client.requestTimeout())
                .maxRetries(client.maxRetries())
                .retryBackoff(client.retryBackoff())
                .build();
        return BggClient.builder().config(config).build();
    }
}
