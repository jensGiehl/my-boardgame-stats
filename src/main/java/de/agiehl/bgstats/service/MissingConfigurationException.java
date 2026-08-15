package de.agiehl.bgstats.service;

public class MissingConfigurationException extends RuntimeException {

    public MissingConfigurationException(String message) {
        super(message);
    }
}
