package de.agiehl.bgstats.statistics;

import de.agiehl.bgstats.config.BggProperties;
import de.agiehl.bgstats.domain.Game;
import de.agiehl.bgstats.domain.Participant;
import de.agiehl.bgstats.domain.PlayCatalog;
import de.agiehl.bgstats.domain.PlayRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;

@Service
public class StatisticsService {

    private static final Comparator<GameStatistics> BY_TOTAL_TIME = Comparator
            .comparingLong(GameStatistics::totalMinutes)
            .reversed()
            .thenComparing(statistics -> statistics.game().name(), String.CASE_INSENSITIVE_ORDER);

    private final int playThreshold;

    public StatisticsService(BggProperties properties) {
        this.playThreshold = properties.playThreshold();
    }

    public OverviewStatistics overview(PlayCatalog catalog) {
        String ownerKey = ownerKey(catalog);
        List<GameStatistics> games = aggregateGames(catalog.plays(), play -> true, ownerKey).stream()
                .filter(game -> game.plays() > playThreshold)
                .toList();
        return new OverviewStatistics(
                games,
                sumPlays(catalog.plays()),
                sumMinutes(catalog.plays()),
                catalog.plays().stream().mapToLong(play -> play.winsFor(ownerKey)).sum(),
                firstDate(catalog.plays()),
                lastDate(catalog.plays()),
                playThreshold);
    }

    public List<Integer> availableYears(PlayCatalog catalog) {
        return catalog.plays().stream()
                .map(play -> play.date().getYear())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    public YearStatistics year(PlayCatalog catalog, int year) {
        List<PlayRecord> plays = catalog.plays().stream()
                .filter(play -> play.date().getYear() == year)
                .toList();
        String ownerKey = ownerKey(catalog);
        List<GameStatistics> games = aggregateGames(plays, play -> true, ownerKey);
        long totalPlays = sumPlays(plays);
        long totalMinutes = sumMinutes(plays);
        String favoriteGame = games.stream()
                .sorted(Comparator.comparingLong(GameStatistics::plays).reversed()
                        .thenComparing(game -> game.game().name(), String.CASE_INSENSITIVE_ORDER))
                .findFirst()
                .map(game -> game.game().name())
                .orElse("–");
        String busiestMonth = busiestMonth(plays);
        return new YearStatistics(
                year,
                totalPlays,
                games.size(),
                totalMinutes,
                plays.stream().mapToLong(play -> play.winsFor(ownerKey)).sum(),
                plays.stream().map(PlayRecord::date).distinct().count(),
                plays.stream().map(PlayRecord::location).distinct().count(),
                totalPlays == 0 ? 0 : Math.round((double) totalMinutes / totalPlays),
                favoriteGame,
                busiestMonth,
                firstDate(plays),
                lastDate(plays),
                monthlyActivity(plays),
                games);
    }

    public FilterOptions filterOptions(PlayCatalog catalog) {
        String ownerKey = ownerKey(catalog);
        List<Game> games = catalog.plays().stream()
                .map(PlayRecord::game)
                .collect(java.util.stream.Collectors.toMap(
                        Game::id,
                        game -> game,
                        (first, second) -> first))
                .values().stream()
                .sorted(Comparator.comparing(Game::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<String> categories = games.stream()
                .flatMap(game -> game.categories().stream())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<String> locations = catalog.plays().stream()
                .map(PlayRecord::location)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<ParticipantOption> participants = participants(catalog, null).stream()
                .filter(participant -> !participant.key().equals(ownerKey))
                .toList();
        return new FilterOptions(availableYears(catalog), categories, participants, locations, games);
    }

    public CustomStatistics custom(PlayCatalog catalog, FilterCriteria filters) {
        List<PlayRecord> plays = catalog.plays().stream().filter(filters::matches).toList();
        String ownerKey = ownerKey(catalog);
        List<GameStatistics> games = aggregateGames(plays, play -> true, ownerKey);
        long totalPlays = sumPlays(plays);
        long wins = plays.stream().mapToLong(play -> play.winsFor(ownerKey)).sum();
        Double averageRating = averageRating(games);
        return new CustomStatistics(
                filters,
                totalPlays,
                games.size(),
                sumMinutes(plays),
                wins,
                totalPlays == 0 ? 0 : wins * 100.0 / totalPlays,
                plays.stream().map(PlayRecord::date).distinct().count(),
                totalPlays == 0 ? 0 : Math.round((double) sumMinutes(plays) / totalPlays),
                averageRating,
                favoriteGame(games),
                firstDate(plays),
                lastDate(plays),
                games,
                categoryStatistics(plays),
                filters.year() == null ? yearlyActivity(plays) : monthlyActivity(plays));
    }

    public List<ParticipantOption> participants(PlayCatalog catalog, String query) {
        Map<String, ParticipantAccumulator> accumulators = new HashMap<>();
        for (PlayRecord play : catalog.plays()) {
            Set<String> counted = new HashSet<>();
            for (Participant participant : play.participants()) {
                if (counted.add(participant.key())) {
                    accumulators.computeIfAbsent(participant.key(), key -> new ParticipantAccumulator(participant))
                            .plays += play.quantity();
                }
            }
        }
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return accumulators.values().stream()
                .map(ParticipantAccumulator::toOption)
                .filter(participant -> normalizedQuery.isBlank()
                        || participant.displayName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || participant.username() != null
                        && participant.username().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(Comparator.comparingLong(ParticipantOption::plays).reversed()
                        .thenComparing(ParticipantOption::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public ParticipantStatistics participant(PlayCatalog catalog, String participantKey) {
        ParticipantOption option = participants(catalog, null).stream()
                .filter(participant -> participant.key().equals(participantKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Die ausgewählte Person wurde nicht gefunden."));
        List<PlayRecord> plays = catalog.plays().stream()
                .filter(play -> play.includes(participantKey))
                .toList();
        List<GameStatistics> games = aggregateGames(plays, play -> true, participantKey);
        long totalPlays = sumPlays(plays);
        long wins = plays.stream().mapToLong(play -> play.winsFor(participantKey)).sum();
        return new ParticipantStatistics(
                option,
                totalPlays,
                games.size(),
                sumMinutes(plays),
                wins,
                totalPlays == 0 ? 0 : wins * 100.0 / totalPlays,
                plays.stream().map(PlayRecord::location).distinct().count(),
                games.stream().sorted(Comparator.comparingLong(GameStatistics::plays).reversed()
                                .thenComparing(game -> game.game().name(), String.CASE_INSENSITIVE_ORDER))
                        .findFirst()
                        .map(game -> game.game().name()).orElse("–"),
                firstDate(plays),
                lastDate(plays),
                favoriteCoPlayers(plays, participantKey),
                games);
    }

    public List<LocationSummary> locations(PlayCatalog catalog) {
        Map<String, List<PlayRecord>> grouped = new LinkedHashMap<>();
        catalog.plays().forEach(play -> grouped.computeIfAbsent(play.location(), key -> new ArrayList<>()).add(play));
        return grouped.entrySet().stream()
                .map(entry -> toLocationSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(LocationSummary::totalMinutes).reversed()
                        .thenComparing(LocationSummary::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public LocationStatistics location(PlayCatalog catalog, String location) {
        List<PlayRecord> plays = catalog.plays().stream()
                .filter(play -> play.location().equalsIgnoreCase(location))
                .toList();
        if (plays.isEmpty()) {
            throw new IllegalArgumentException("Der ausgewählte Ort wurde nicht gefunden.");
        }
        List<GameStatistics> games = aggregateGames(plays, play -> true, ownerKey(catalog));
        return new LocationStatistics(
                plays.getFirst().location(),
                sumPlays(plays),
                games.size(),
                sumMinutes(plays),
                uniqueParticipants(plays),
                plays.stream().map(PlayRecord::date).distinct().count(),
                games.stream().sorted(Comparator.comparingLong(GameStatistics::plays).reversed()
                                .thenComparing(game -> game.game().name(), String.CASE_INSENSITIVE_ORDER))
                        .findFirst()
                        .map(game -> game.game().name()).orElse("–"),
                firstDate(plays),
                lastDate(plays),
                games);
    }

    private List<GameStatistics> aggregateGames(
            Collection<PlayRecord> plays,
            Predicate<PlayRecord> predicate,
            String winnerKey) {
        Map<Integer, GameAccumulator> games = new HashMap<>();
        plays.stream().filter(predicate).forEach(play -> games
                .computeIfAbsent(play.game().id(), id -> new GameAccumulator(play.game()))
                .accept(play, winnerKey));
        return games.values().stream()
                .map(GameAccumulator::toStatistics)
                .sorted(BY_TOTAL_TIME)
                .toList();
    }

    private List<CoPlayerStatistics> favoriteCoPlayers(List<PlayRecord> plays, String selectedKey) {
        Map<String, ParticipantAccumulator> players = new HashMap<>();
        for (PlayRecord play : plays) {
            Set<String> counted = new HashSet<>();
            play.participants().stream()
                    .filter(participant -> !participant.key().equals(selectedKey))
                    .filter(participant -> counted.add(participant.key()))
                    .forEach(participant -> players
                            .computeIfAbsent(participant.key(), key -> new ParticipantAccumulator(participant))
                            .plays += play.quantity());
        }
        return players.values().stream()
                .map(player -> new CoPlayerStatistics(
                        player.participant.displayName(),
                        player.participant.username(),
                        player.plays))
                .sorted(Comparator.comparingLong(CoPlayerStatistics::plays).reversed()
                        .thenComparing(CoPlayerStatistics::displayName, String.CASE_INSENSITIVE_ORDER))
                .limit(5)
                .toList();
    }

    private LocationSummary toLocationSummary(String name, List<PlayRecord> plays) {
        return new LocationSummary(
                name,
                sumPlays(plays),
                sumMinutes(plays),
                plays.stream().map(play -> play.game().id()).distinct().count(),
                uniqueParticipants(plays),
                firstDate(plays),
                lastDate(plays));
    }

    private long uniqueParticipants(List<PlayRecord> plays) {
        return plays.stream().flatMap(play -> play.participants().stream()).map(Participant::key).distinct().count();
    }

    private String busiestMonth(List<PlayRecord> plays) {
        return plays.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        play -> play.date().getMonth(),
                        java.util.stream.Collectors.summingLong(PlayRecord::quantity)))
                .entrySet().stream()
                .sorted(Map.Entry.<Month, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .findFirst()
                .map(Map.Entry::getKey)
                .map(this::germanMonth)
                .orElse("–");
    }

    private List<ActivityStatistics> monthlyActivity(List<PlayRecord> plays) {
        Map<Month, Long> playCounts = plays.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        play -> play.date().getMonth(),
                        java.util.stream.Collectors.summingLong(PlayRecord::quantity)));
        Map<Month, Long> minuteCounts = plays.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        play -> play.date().getMonth(),
                        java.util.stream.Collectors.summingLong(PlayRecord::totalMinutes)));
        long maximum = playCounts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        return IntStream.rangeClosed(1, 12)
                .mapToObj(Month::of)
                .map(month -> new ActivityStatistics(
                        germanMonth(month),
                        playCounts.getOrDefault(month, 0L),
                        minuteCounts.getOrDefault(month, 0L),
                        percentage(playCounts.getOrDefault(month, 0L), maximum)))
                .toList();
    }

    private List<ActivityStatistics> yearlyActivity(List<PlayRecord> plays) {
        Map<Integer, Long> playCounts = plays.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        play -> play.date().getYear(),
                        java.util.stream.Collectors.summingLong(PlayRecord::quantity)));
        Map<Integer, Long> minuteCounts = plays.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        play -> play.date().getYear(),
                        java.util.stream.Collectors.summingLong(PlayRecord::totalMinutes)));
        long maximum = playCounts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        return playCounts.keySet().stream()
                .sorted()
                .map(year -> new ActivityStatistics(
                        year.toString(),
                        playCounts.get(year),
                        minuteCounts.getOrDefault(year, 0L),
                        percentage(playCounts.get(year), maximum)))
                .toList();
    }

    private List<CategoryStatistics> categoryStatistics(List<PlayRecord> plays) {
        Map<String, Long> counts = new HashMap<>();
        for (PlayRecord play : plays) {
            play.game().categories().forEach(category -> counts.merge(category, (long) play.quantity(), Long::sum));
        }
        long maximum = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
                .limit(8)
                .map(entry -> new CategoryStatistics(
                        entry.getKey(),
                        entry.getValue(),
                        percentage(entry.getValue(), maximum)))
                .toList();
    }

    private Double averageRating(List<GameStatistics> games) {
        var average = games.stream()
                .map(GameStatistics::game)
                .map(Game::averageRating)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average();
        return average.isPresent() ? average.getAsDouble() : null;
    }

    private String favoriteGame(List<GameStatistics> games) {
        return games.stream()
                .sorted(Comparator.comparingLong(GameStatistics::plays).reversed()
                        .thenComparing(game -> game.game().name(), String.CASE_INSENSITIVE_ORDER))
                .findFirst()
                .map(game -> game.game().name())
                .orElse("–");
    }

    private double percentage(long value, long maximum) {
        return maximum == 0 ? 0 : value * 100.0 / maximum;
    }

    private String germanMonth(Month month) {
        String name = month.getDisplayName(TextStyle.FULL, Locale.GERMAN);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private String ownerKey(PlayCatalog catalog) {
        return "u:" + catalog.username().trim().toLowerCase(Locale.ROOT);
    }

    private long sumPlays(Collection<PlayRecord> plays) {
        return plays.stream().mapToLong(PlayRecord::quantity).sum();
    }

    private long sumMinutes(Collection<PlayRecord> plays) {
        return plays.stream().mapToLong(PlayRecord::totalMinutes).sum();
    }

    private LocalDate firstDate(Collection<PlayRecord> plays) {
        return plays.stream().map(PlayRecord::date).min(LocalDate::compareTo).orElse(null);
    }

    private LocalDate lastDate(Collection<PlayRecord> plays) {
        return plays.stream().map(PlayRecord::date).max(LocalDate::compareTo).orElse(null);
    }

    private static final class ParticipantAccumulator {

        private final Participant participant;
        private long plays;

        private ParticipantAccumulator(Participant participant) {
            this.participant = participant;
        }

        private ParticipantOption toOption() {
            String username = participant.username();
            if (username != null && username.isBlank()) {
                username = null;
            }
            return new ParticipantOption(participant.key(), participant.displayName(), username, plays);
        }
    }

    private static final class GameAccumulator {

        private final Game game;
        private final Set<String> players = new HashSet<>();
        private final Map<String, Long> locations = new HashMap<>();
        private long plays;
        private long totalMinutes;
        private long wins;
        private LocalDate firstPlay;
        private LocalDate lastPlay;

        private GameAccumulator(Game game) {
            this.game = game;
        }

        private void accept(PlayRecord play, String winnerKey) {
            plays += play.quantity();
            totalMinutes += play.totalMinutes();
            wins += play.winsFor(winnerKey);
            firstPlay = firstPlay == null || play.date().isBefore(firstPlay) ? play.date() : firstPlay;
            lastPlay = lastPlay == null || play.date().isAfter(lastPlay) ? play.date() : lastPlay;
            play.participants().stream().map(Participant::key).forEach(players::add);
            locations.merge(play.location(), (long) play.quantity(), Long::sum);
        }

        private GameStatistics toStatistics() {
            String favoriteLocation = locations.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                            .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse("–");
            return new GameStatistics(
                    game,
                    plays,
                    totalMinutes,
                    firstPlay,
                    lastPlay,
                    wins,
                    players.size(),
                    favoriteLocation);
        }
    }
}
