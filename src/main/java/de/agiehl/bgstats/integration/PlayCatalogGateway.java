package de.agiehl.bgstats.integration;

import de.agiehl.bgstats.domain.PlayCatalog;

public interface PlayCatalogGateway {

    PlayCatalog load(String username);
}
