package de.agiehl.bgstats.integration;

import de.agiehl.bgg.BggClient;
import de.agiehl.bgg.api.PlaysApi;
import de.agiehl.bgg.api.ThingApi;
import de.agiehl.bgg.model.common.DecimalValue;
import de.agiehl.bgg.model.common.IntValue;
import de.agiehl.bgg.model.common.Link;
import de.agiehl.bgg.model.common.Name;
import de.agiehl.bgg.model.plays.Play;
import de.agiehl.bgg.model.plays.PlayItem;
import de.agiehl.bgg.model.plays.PlayPlayer;
import de.agiehl.bgg.model.thing.Thing;
import de.agiehl.bgg.model.thing.ThingRank;
import de.agiehl.bgg.model.thing.ThingRatings;
import de.agiehl.bgg.model.thing.ThingResponse;
import de.agiehl.bgg.model.thing.ThingStatistics;
import de.agiehl.bgg.request.PlaysRequest;
import de.agiehl.bgg.request.ThingRequest;
import de.agiehl.bgstats.config.BggProperties;
import de.agiehl.bgstats.domain.Game;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BggClientGatewayTest {

    @Test
    void loadsCompleteGameDetailsWithStatistics() {
        BggClient client = mock(BggClient.class);
        PlaysApi playsApi = mock(PlaysApi.class);
        ThingApi thingApi = mock(ThingApi.class);
        when(client.plays()).thenReturn(playsApi);
        when(client.things()).thenReturn(thingApi);
        when(playsApi.allItems(any(PlaysRequest.class))).thenReturn(List.of(play()));
        when(thingApi.fetch(any(ThingRequest.class))).thenReturn(ThingResponse.builder().item(thing()).build());
        BggClientGateway gateway = new BggClientGateway(client, properties());

        Game game = gateway.load("Owner").plays().getFirst().game();

        ArgumentCaptor<ThingRequest> request = ArgumentCaptor.forClass(ThingRequest.class);
        verify(thingApi).fetch(request.capture());
        assertThat(request.getValue().isStats()).isTrue();
        assertThat(game.name()).isEqualTo("Catan");
        assertThat(game.yearPublished()).isEqualTo(1995);
        assertThat(game.averageRating()).isEqualTo(7.1);
        assertThat(game.complexity()).isEqualTo(2.3);
        assertThat(game.boardGameRank()).isEqualTo(600);
        assertThat(game.categories()).containsExactly("Strategy");
        assertThat(game.mechanics()).containsExactly("Trading");
    }

    @Test
    void retainsOnlyPlaysContainingRequestedUsername() {
        BggClient client = mock(BggClient.class);
        PlaysApi playsApi = mock(PlaysApi.class);
        ThingApi thingApi = mock(ThingApi.class);
        when(client.plays()).thenReturn(playsApi);
        when(client.things()).thenReturn(thingApi);
        when(playsApi.allItems(any(PlaysRequest.class))).thenReturn(List.of(
                play(1, player(" owner ")),
                play(2, player("SomeoneElse")),
                play(3)));
        when(thingApi.fetch(any(ThingRequest.class))).thenReturn(ThingResponse.builder().item(thing()).build());
        BggClientGateway gateway = new BggClientGateway(client, properties());

        assertThat(gateway.load("Owner").plays())
                .extracting(play -> play.id())
                .containsExactly(1);
    }

    private Play play() {
        return play(1, player("Owner"));
    }

    private Play play(int id, PlayPlayer... players) {
        return Play.builder()
                .id(id)
                .date("2026-05-03")
                .quantity(1)
                .length(90)
                .location("Zuhause")
                .item(PlayItem.builder().objectid(13).name("Catan alt").build())
                .players(List.of(players))
                .build();
    }

    private PlayPlayer player(String username) {
        return PlayPlayer.builder().username(username).name(username).build();
    }

    private Thing thing() {
        ThingRatings ratings = ThingRatings.builder()
                .average(decimalValue(7.1))
                .averageweight(decimalValue(2.3))
                .rank(ThingRank.builder().name("boardgame").value("600").build())
                .build();
        return Thing.builder()
                .id(13)
                .name(Name.builder().type("primary").value("Catan").build())
                .image("https://example.com/catan.jpg")
                .yearpublished(intValue(1995))
                .minplayers(intValue(3))
                .maxplayers(intValue(4))
                .playingtime(intValue(90))
                .minage(intValue(10))
                .link(Link.builder().type("boardgamecategory").value("Strategy").build())
                .link(Link.builder().type("boardgamemechanic").value("Trading").build())
                .statistics(ThingStatistics.builder().ratings(ratings).build())
                .build();
    }

    private IntValue intValue(int value) {
        return IntValue.builder().value(value).build();
    }

    private DecimalValue decimalValue(double value) {
        return DecimalValue.builder().value(value).build();
    }

    private BggProperties properties() {
        return new BggProperties(
                "key",
                "Owner",
                "",
                "bg-stats-data.json",
                8,
                Duration.ofMinutes(15),
                20,
                new BggProperties.Client(
                        URI.create("https://example.com"),
                        "apikey",
                        "test",
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        0,
                        Duration.ZERO));
    }
}
