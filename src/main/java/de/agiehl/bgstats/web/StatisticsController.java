package de.agiehl.bgstats.web;

import de.agiehl.bgstats.domain.PlayCatalog;
import de.agiehl.bgstats.service.CatalogLoadingState;
import de.agiehl.bgstats.service.CatalogLoadingStatus;
import de.agiehl.bgstats.service.CatalogStartupLoader;
import de.agiehl.bgstats.service.PlayCatalogService;
import de.agiehl.bgstats.statistics.FilterCriteria;
import de.agiehl.bgstats.statistics.LocationSummary;
import de.agiehl.bgstats.statistics.ParticipantOption;
import de.agiehl.bgstats.statistics.StatisticsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class StatisticsController {

    private final PlayCatalogService catalogService;
    private final CatalogStartupLoader catalogStartupLoader;
    private final StatisticsService statisticsService;

    public StatisticsController(
            PlayCatalogService catalogService,
            CatalogStartupLoader catalogStartupLoader,
            StatisticsService statisticsService) {
        this.catalogService = catalogService;
        this.catalogStartupLoader = catalogStartupLoader;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/")
    String overview(Model model) {
        CatalogLoadingStatus loadingStatus = catalogStartupLoader.status();
        if (loadingStatus.state() != CatalogLoadingState.READY) {
            model.addAttribute("loadingStatus", loadingStatus);
            return "loading";
        }
        PlayCatalog catalog = catalogService.getCatalog();
        addCommon(model, catalog, "games");
        model.addAttribute("statistics", statisticsService.overview(catalog));
        return "overview";
    }

    @GetMapping("/years")
    String years(@RequestParam(required = false) Integer year, Model model) {
        if (!catalogStartupLoader.isReady()) {
            return "redirect:/";
        }
        PlayCatalog catalog = catalogService.getCatalog();
        List<Integer> years = statisticsService.availableYears(catalog);
        Integer selectedYear = year != null && years.contains(year) ? year : years.stream().findFirst().orElse(null);
        addCommon(model, catalog, "years");
        model.addAttribute("years", years);
        model.addAttribute("selectedYear", selectedYear);
        if (selectedYear != null) {
            model.addAttribute("statistics", statisticsService.year(catalog, selectedYear));
        }
        return "years";
    }

    @GetMapping("/users")
    String users(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) String user,
            Model model) {
        if (!catalogStartupLoader.isReady()) {
            return "redirect:/";
        }
        PlayCatalog catalog = catalogService.getCatalog();
        List<ParticipantOption> allParticipants = statisticsService.participants(catalog, null);
        List<ParticipantOption> matches = statisticsService.participants(catalog, q);
        String selectedKey = selectParticipant(catalog, q, user, allParticipants, matches);
        addCommon(model, catalog, "users");
        model.addAttribute("query", q);
        model.addAttribute("participants", matches);
        model.addAttribute("selectedKey", selectedKey);
        if (selectedKey != null) {
            model.addAttribute("statistics", statisticsService.participant(catalog, selectedKey));
        }
        return "users";
    }

    @GetMapping("/locations")
    String locations(@RequestParam(required = false) String location, Model model) {
        if (!catalogStartupLoader.isReady()) {
            return "redirect:/";
        }
        PlayCatalog catalog = catalogService.getCatalog();
        List<LocationSummary> locations = statisticsService.locations(catalog);
        String selectedLocation = locations.stream()
                .map(LocationSummary::name)
                .filter(name -> location != null && name.equalsIgnoreCase(location))
                .findFirst()
                .orElseGet(() -> locations.stream().map(LocationSummary::name).findFirst().orElse(null));
        addCommon(model, catalog, "locations");
        model.addAttribute("locations", locations);
        model.addAttribute("selectedLocation", selectedLocation);
        if (selectedLocation != null) {
            model.addAttribute("statistics", statisticsService.location(catalog, selectedLocation));
        }
        return "locations";
    }

    @GetMapping("/custom")
    String custom(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String player,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer game,
            @RequestParam(required = false) Double minRating,
            Model model) {
        if (!catalogStartupLoader.isReady()) {
            return "redirect:/";
        }
        PlayCatalog catalog = catalogService.getCatalog();
        FilterCriteria filters = new FilterCriteria(year, category, player, location, game, minRating);
        addCommon(model, catalog, "custom");
        model.addAttribute("options", statisticsService.filterOptions(catalog));
        model.addAttribute("statistics", statisticsService.custom(catalog, filters));
        return "custom";
    }

    @PostMapping("/refresh")
    String refresh(RedirectAttributes redirectAttributes) {
        if (!catalogStartupLoader.isReady()) {
            return "redirect:/";
        }
        catalogService.reload();
        redirectAttributes.addFlashAttribute("successMessage", "Die Daten wurden aktualisiert.");
        return "redirect:/";
    }

    @GetMapping("/api/catalog/status")
    @ResponseBody
    CatalogLoadingStatus catalogStatus() {
        return catalogStartupLoader.status();
    }

    @PostMapping("/loading/retry")
    String retryLoading() {
        catalogStartupLoader.startLoading();
        return "redirect:/";
    }

    private String selectParticipant(
            PlayCatalog catalog,
            String query,
            String requestedKey,
            List<ParticipantOption> allParticipants,
            List<ParticipantOption> matches) {
        if (requestedKey != null && allParticipants.stream().anyMatch(option -> option.key().equals(requestedKey))) {
            return requestedKey;
        }
        if (query != null && !query.isBlank()) {
            return matches.size() == 1 ? matches.getFirst().key() : null;
        }
        String ownerKey = "u:" + catalog.username().trim().toLowerCase(java.util.Locale.ROOT);
        return allParticipants.stream()
                .filter(option -> option.key().equals(ownerKey))
                .map(ParticipantOption::key)
                .findFirst()
                .orElseGet(() -> allParticipants.stream().map(ParticipantOption::key).findFirst().orElse(null));
    }

    private void addCommon(Model model, PlayCatalog catalog, String activePage) {
        model.addAttribute("username", catalog.username());
        model.addAttribute("loadedAt", catalog.loadedAt());
        model.addAttribute("activePage", activePage);
    }
}
