package de.agiehl.bgstats.web;

import de.agiehl.bgstats.domain.Game;
import de.agiehl.bgstats.domain.Participant;
import de.agiehl.bgstats.domain.PlayCatalog;
import de.agiehl.bgstats.domain.PlayRecord;
import de.agiehl.bgstats.integration.PlayCatalogGateway;
import de.agiehl.bgstats.service.CatalogLoadingState;
import de.agiehl.bgstats.service.CatalogStartupLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "bgg.api-key=test-key",
        "bgg.username=Owner",
        "bgg.snapshot-file=target/test-data/statistics-pages.json",
        "bgg.play-threshold=0"
})
@AutoConfigureMockMvc
@Import(StatisticsPagesTest.GatewayConfiguration.class)
class StatisticsPagesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CatalogStartupLoader catalogStartupLoader;

    @BeforeEach
    void waitUntilCatalogIsReady() {
        await().untilAsserted(() -> assertThat(catalogStartupLoader.status().state())
                .isEqualTo(CatalogLoadingState.READY));
    }

    @Test
    void rendersAllStatisticsPages() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Catan")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "src=\"https://cf.geekdo-images.com/example/filters:format(jpeg)/catan.jpg\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("style=\"--chart-value: 100.0%\"")));
        mockMvc.perform(get("/years"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Jahresstatistik")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "aria-label=\"Balkendiagramm der Partien pro Monat im Jahr 2026\"")));
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Statistik pro Person")));
        mockMvc.perform(get("/locations"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Zuhause")));
        mockMvc.perform(get("/custom").param("category", "Strategy"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Eigene Statistik erstellen")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Für WhatsApp formatiert kopieren")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Für BoardGameGeek formatiert kopieren")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("*Catan*")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("[b][thing=13]Catan[/thing][/b]")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("*Verschiedene Spieltage:* 1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("*Max. Spielzeit an einem Tag:* 2 Stunden und 30 Minuten")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Häufigste Kategorien")));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class GatewayConfiguration {

        @Bean
        @Primary
        PlayCatalogGateway testPlayCatalogGateway() {
            Game game = new Game(
                    13,
                    "Catan",
                    "https://cf.geekdo-images.com/example/filters:format(jpeg)/catan.jpg",
                    1995,
                    3,
                    4,
                    90,
                    10,
                    7.1,
                    2.3,
                    600,
                    List.of("Strategy"),
                    List.of("Trading"));
            Participant owner = new Participant("Owner", "Alex", true);
            Participant guest = new Participant("Guest", "Bea", false);
            PlayRecord play = new PlayRecord(
                    1,
                    LocalDate.of(2026, 5, 3),
                    2,
                    75,
                    false,
                    "Zuhause",
                    game,
                    List.of(owner, guest));
            return username -> new PlayCatalog(
                    username,
                    Instant.parse("2026-08-13T10:00:00Z"),
                    List.of(play));
        }
    }
}
