package de.agiehl.bgstats.integration;

public class CatalogFileException extends RuntimeException {

    public CatalogFileException(String message) {
        super(message);
    }

    public CatalogFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
