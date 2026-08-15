package de.agiehl.bgstats.web;

import de.agiehl.bgg.exception.BggClientException;
import de.agiehl.bgstats.integration.CatalogFileException;
import de.agiehl.bgstats.service.MissingConfigurationException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingConfigurationException.class)
    String missingConfiguration(MissingConfigurationException exception, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        model.addAttribute("title", "Konfiguration fehlt");
        model.addAttribute("message", exception.getMessage());
        model.addAttribute("configurationError", true);
        return "error";
    }

    @ExceptionHandler(BggClientException.class)
    String bggError(BggClientException exception, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_GATEWAY.value());
        model.addAttribute("title", "BoardGameGeek ist gerade nicht erreichbar");
        model.addAttribute("message", "Die Daten konnten nicht von BoardGameGeek geladen werden. Bitte später erneut versuchen.");
        return "error";
    }

    @ExceptionHandler(CatalogFileException.class)
    String catalogFileError(CatalogFileException exception, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        model.addAttribute("title", "JSON-Datendatei nicht verfügbar");
        model.addAttribute("message", exception.getMessage());
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    String invalidSelection(IllegalArgumentException exception, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        model.addAttribute("title", "Auswahl nicht gefunden");
        model.addAttribute("message", exception.getMessage());
        return "error";
    }
}
