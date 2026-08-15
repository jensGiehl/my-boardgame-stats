package de.agiehl.bgstats.service;

import de.agiehl.bgg.exception.BggClientException;
import de.agiehl.bgstats.integration.CatalogFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class CatalogStartupLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogStartupLoader.class);
    private static final String LOADING_MESSAGE = "Deine BoardGameGeek-Daten werden vollständig geladen.";

    private final PlayCatalogService catalogService;
    private final TaskExecutor taskExecutor;
    private final AtomicBoolean loading = new AtomicBoolean();
    private final AtomicReference<CatalogLoadingStatus> status = new AtomicReference<>(
            new CatalogLoadingStatus(CatalogLoadingState.LOADING, LOADING_MESSAGE));

    public CatalogStartupLoader(
            PlayCatalogService catalogService,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.catalogService = catalogService;
        this.taskExecutor = taskExecutor;
    }

    @EventListener(ApplicationReadyEvent.class)
    void loadAtStartup() {
        startLoading();
    }

    public boolean startLoading() {
        if (!loading.compareAndSet(false, true)) {
            return false;
        }
        status.set(new CatalogLoadingStatus(CatalogLoadingState.LOADING, LOADING_MESSAGE));
        try {
            taskExecutor.execute(this::loadCatalog);
            return true;
        } catch (RuntimeException exception) {
            loading.set(false);
            fail(exception);
            return false;
        }
    }

    public CatalogLoadingStatus status() {
        return status.get();
    }

    public boolean isReady() {
        return status.get().state() == CatalogLoadingState.READY;
    }

    private void loadCatalog() {
        try {
            catalogService.reload();
            status.set(new CatalogLoadingStatus(CatalogLoadingState.READY, "Alle Daten wurden geladen."));
        } catch (RuntimeException exception) {
            fail(exception);
        } finally {
            loading.set(false);
        }
    }

    private void fail(RuntimeException exception) {
        LOGGER.error("Catalog loading failed", exception);
        status.set(new CatalogLoadingStatus(CatalogLoadingState.FAILED, failureMessage(exception)));
    }

    private String failureMessage(RuntimeException exception) {
        return switch (exception) {
            case MissingConfigurationException missingConfiguration -> missingConfiguration.getMessage();
            case CatalogFileException catalogFileException -> catalogFileException.getMessage();
            case BggClientException ignored -> "BoardGameGeek ist gerade nicht erreichbar. Bitte versuche es später erneut.";
            default -> "Die Daten konnten nicht geladen werden. Bitte versuche es erneut.";
        };
    }
}
