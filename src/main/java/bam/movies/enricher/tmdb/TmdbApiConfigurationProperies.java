package bam.movies.enricher.tmdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tmdb")
public record TmdbApiConfigurationProperies(String baseUrl, String apiToken) {
}
