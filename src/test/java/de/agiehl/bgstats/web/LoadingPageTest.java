package de.agiehl.bgstats.web;

import de.agiehl.bgstats.service.CatalogLoadingState;
import de.agiehl.bgstats.service.CatalogLoadingStatus;
import de.agiehl.bgstats.service.CatalogStartupLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class LoadingPageTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogStartupLoader catalogStartupLoader;

    @Test
    void rendersWaitingAnimationUntilTheCatalogIsReady() throws Exception {
        when(catalogStartupLoader.status()).thenReturn(new CatalogLoadingStatus(
                CatalogLoadingState.LOADING,
                "Deine BoardGameGeek-Daten werden vollständig geladen."));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("loading"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bitte warten")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("loading-animation")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/loading.js")));
        mockMvc.perform(get("/api/catalog/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("LOADING"));
    }

    @Test
    void rendersRetryActionAfterLoadingFailed() throws Exception {
        when(catalogStartupLoader.status()).thenReturn(new CatalogLoadingStatus(
                CatalogLoadingState.FAILED,
                "BoardGameGeek ist gerade nicht erreichbar."));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("loading"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Laden fehlgeschlagen")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Erneut versuchen")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "BoardGameGeek ist gerade nicht erreichbar.")));
    }
}
