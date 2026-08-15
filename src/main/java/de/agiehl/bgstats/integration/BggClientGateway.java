package de.agiehl.bgstats.integration;

import de.agiehl.bgg.BggClient;
import de.agiehl.bgg.model.plays.Play;
import de.agiehl.bgg.model.plays.PlayPlayer;
import de.agiehl.bgg.model.common.DecimalValue;
import de.agiehl.bgg.model.common.IntValue;
import de.agiehl.bgg.model.common.Link;
import de.agiehl.bgg.model.thing.Thing;
import de.agiehl.bgg.model.thing.ThingRank;
import de.agiehl.bgg.model.thing.ThingRatings;
import de.agiehl.bgg.request.PlaysRequest;
import de.agiehl.bgg.request.ThingRequest;
import de.agiehl.bgstats.config.BggProperties;
import de.agiehl.bgstats.domain.Game;
import de.agiehl.bgstats.domain.Participant;
import de.agiehl.bgstats.domain.PlayCatalog;
import de.agiehl.bgstats.domain.PlayRecord;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class BggClientGateway implements PlayCatalogGateway {

    private final BggClient client;
    private final int coverBatchSize;

    public BggClientGateway(BggClient client, BggProperties properties) {
        this.client = client;
        this.coverBatchSize = properties.coverBatchSize();
    }

    @Override
    public PlayCatalog load(String username) {
        List<Play> sourcePlays = client.plays().allItems(
                PlaysRequest.builder().username(username).build());
        List<Play> relevantPlays = sourcePlays.stream()
                .filter(play -> play.getItem() != null && play.getItem().getObjectid() != null)
                .filter(play -> includesUser(play, username))
                .toList();
        Map<Integer, Thing> things = loadThings(relevantPlays);
        List<PlayRecord> records = relevantPlays.stream()
                .map(play -> toPlayRecord(play, things.get(play.getItem().getObjectid())))
                .toList();
        return new PlayCatalog(username, Instant.now(), records);
    }

    private boolean includesUser(Play play, String username) {
        if (play.getPlayers() == null) {
            return false;
        }
        String normalizedUsername = username.trim();
        return play.getPlayers().stream()
                .map(PlayPlayer::getUsername)
                .filter(playerUsername -> playerUsername != null && !playerUsername.isBlank())
                .map(String::trim)
                .anyMatch(normalizedUsername::equalsIgnoreCase);
    }

    private Map<Integer, Thing> loadThings(List<Play> plays) {
        Set<Integer> ids = new LinkedHashSet<>();
        plays.stream()
                .filter(play -> play.getItem() != null)
                .map(play -> play.getItem().getObjectid())
                .filter(id -> id != null && id > 0)
                .forEach(ids::add);

        List<Integer> orderedIds = new ArrayList<>(ids);
        Map<Integer, Thing> things = new HashMap<>();
        for (int start = 0; start < orderedIds.size(); start += coverBatchSize) {
            int end = Math.min(start + coverBatchSize, orderedIds.size());
            List<Integer> batch = orderedIds.subList(start, end);
            List<Thing> response = client.things()
                    .fetch(ThingRequest.builder().ids(batch).stats(true).build())
                    .getItems();
            if (response != null) {
                response.stream()
                        .filter(thing -> thing.getId() != null)
                        .forEach(thing -> things.put(thing.getId(), thing));
            }
        }
        return things;
    }

    private PlayRecord toPlayRecord(Play play, Thing thing) {
        String name = play.getItem().getName();
        if (thing != null && thing.getPrimaryName() != null && !thing.getPrimaryName().isBlank()) {
            name = thing.getPrimaryName();
        }
        String coverUrl = thing == null ? null : firstNonBlank(thing.getImage(), thing.getThumbnail());
        Game game = toGame(play.getItem().getObjectid(), name, coverUrl, thing);
        List<Participant> participants = play.getPlayers() == null
                ? List.of()
                : play.getPlayers().stream().map(this::toParticipant).toList();
        return new PlayRecord(
                play.getId() == null ? 0 : play.getId(),
                LocalDate.parse(play.getDate()),
                play.getQuantity() == null ? 1 : play.getQuantity(),
                play.getLength() == null ? 0 : play.getLength(),
                Boolean.TRUE.equals(play.getNowinstats()) || Boolean.TRUE.equals(play.getIncomplete()),
                play.getLocation(),
                game,
                participants);
    }

    private Game toGame(int id, String name, String coverUrl, Thing thing) {
        ThingRatings ratings = thing == null || thing.getStatistics() == null
                ? null
                : thing.getStatistics().getRatings();
        return new Game(
                id,
                name,
                coverUrl,
                thing == null ? null : value(thing.getYearpublished()),
                thing == null ? null : value(thing.getMinplayers()),
                thing == null ? null : value(thing.getMaxplayers()),
                thing == null ? null : value(thing.getPlayingtime()),
                thing == null ? null : value(thing.getMinage()),
                ratings == null ? null : value(ratings.getAverage()),
                ratings == null ? null : value(ratings.getAverageweight()),
                ratings == null ? null : boardGameRank(ratings.getRanks()),
                links(thing, "boardgamecategory"),
                links(thing, "boardgamemechanic"));
    }

    private List<String> links(Thing thing, String type) {
        if (thing == null || thing.getLinks() == null) {
            return List.of();
        }
        return thing.getLinks().stream()
                .filter(link -> type.equals(link.getType()))
                .map(Link::getValue)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private Integer boardGameRank(List<ThingRank> ranks) {
        if (ranks == null) {
            return null;
        }
        return ranks.stream()
                .filter(rank -> "boardgame".equals(rank.getName()))
                .map(ThingRank::asIntRank)
                .filter(rank -> rank != null && rank > 0)
                .findFirst()
                .orElse(null);
    }

    private Integer value(IntValue value) {
        return value == null ? null : value.getValue();
    }

    private Double value(DecimalValue value) {
        return value == null ? null : value.getValue();
    }

    private Participant toParticipant(PlayPlayer player) {
        return new Participant(player.getUsername(), player.getName(), Boolean.TRUE.equals(player.getWin()));
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
