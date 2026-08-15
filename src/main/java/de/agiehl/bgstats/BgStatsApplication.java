package de.agiehl.bgstats;

import de.agiehl.bgstats.config.BggProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BggProperties.class)
public class BgStatsApplication {

    static void main(String[] args) {
        SpringApplication.run(BgStatsApplication.class, args);
    }
}
