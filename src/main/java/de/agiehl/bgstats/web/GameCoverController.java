package de.agiehl.bgstats.web;

import de.agiehl.bgstats.domain.Game;
import de.agiehl.bgstats.service.CatalogStartupLoader;
import de.agiehl.bgstats.service.PlayCatalogService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.Duration;

@RestController
public class GameCoverController {

    private final PlayCatalogService catalogService;
    private final CatalogStartupLoader catalogStartupLoader;
    private final RestClient restClient;

    public GameCoverController(PlayCatalogService catalogService, CatalogStartupLoader catalogStartupLoader) {
        this.catalogService = catalogService;
        this.catalogStartupLoader = catalogStartupLoader;
        this.restClient = RestClient.create();
    }

    @GetMapping("/covers/{gameId}")
    ResponseEntity<byte[]> cover(@PathVariable int gameId) {
        if (!catalogStartupLoader.isReady()) {
            return ResponseEntity.notFound().build();
        }
        return catalogService.getCatalog().plays().stream()
                .map(play -> play.game())
                .filter(game -> game.id() == gameId)
                .findFirst()
                .map(this::loadCover)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<byte[]> loadCover(Game game) {
        URI uri = validatedCoverUri(game.coverUrl());
        if (uri == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            ResponseEntity<byte[]> response = restClient.get().uri(uri).retrieve().toEntity(byte[].class);
            MediaType contentType = response.getHeaders().getContentType();
            if (contentType == null || !"image".equals(contentType.getType()) || response.getBody() == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                    .body(response.getBody());
        } catch (RestClientException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    private URI validatedCoverUri(String coverUrl) {
        if (coverUrl == null || coverUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(coverUrl);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && host != null
                    && (host.equals("geekdo-images.com") || host.endsWith(".geekdo-images.com")) ? uri : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
