package de.agiehl.bgstats.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CatalogStartupLoaderTest {

    @Test
    void schedulesCatalogLoadingWithoutBlockingTheStartupThread() {
        PlayCatalogService catalogService = mock(PlayCatalogService.class);
        QueuedTaskExecutor taskExecutor = new QueuedTaskExecutor();
        CatalogStartupLoader loader = new CatalogStartupLoader(catalogService, taskExecutor);

        loader.loadAtStartup();

        assertThat(loader.status().state()).isEqualTo(CatalogLoadingState.LOADING);
        assertThat(taskExecutor.tasks).hasSize(1);
        verifyNoInteractions(catalogService);

        taskExecutor.runNext();

        assertThat(loader.status()).isEqualTo(
                new CatalogLoadingStatus(CatalogLoadingState.READY, "Alle Daten wurden geladen."));
        verify(catalogService).reload();
    }

    @Test
    void preventsConcurrentLoadsAndExposesFailuresForTheWaitingPage() {
        PlayCatalogService catalogService = mock(PlayCatalogService.class);
        doThrow(new MissingConfigurationException("Der BGG-API-Key fehlt."))
                .when(catalogService).reload();
        QueuedTaskExecutor taskExecutor = new QueuedTaskExecutor();
        CatalogStartupLoader loader = new CatalogStartupLoader(catalogService, taskExecutor);

        assertThat(loader.startLoading()).isTrue();
        assertThat(loader.startLoading()).isFalse();
        assertThat(taskExecutor.tasks).hasSize(1);

        taskExecutor.runNext();

        assertThat(loader.status()).isEqualTo(
                new CatalogLoadingStatus(CatalogLoadingState.FAILED, "Der BGG-API-Key fehlt."));
    }

    private static final class QueuedTaskExecutor implements TaskExecutor {

        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void execute(Runnable task) {
            tasks.add(task);
        }

        private void runNext() {
            tasks.removeFirst().run();
        }
    }
}
