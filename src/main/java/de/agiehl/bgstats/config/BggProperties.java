package de.agiehl.bgstats.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "bgg")
public record BggProperties(
        String apiKey,
        String username,
        String inputFile,
        @NotNull String snapshotFile,
        @Min(0) int playThreshold,
        @NotNull Duration cacheTtl,
        @Min(1) int coverBatchSize,
        @Valid @NotNull Client client) {

    public record Client(
            @NotNull URI baseUri,
            @NotNull String apiKeyParameter,
            @NotNull String userAgent,
            @NotNull Duration connectTimeout,
            @NotNull Duration requestTimeout,
            @Min(0) int maxRetries,
            @NotNull Duration retryBackoff) {
    }
}
