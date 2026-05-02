package bam.movies.enricher.tmdb;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(TmdbApiConfigurationProperies.class)
public class TmdbRemoteApiConfiguration {

    @Bean
    public TmdbRemoteApi tmdbRemoteApi(TmdbApiConfigurationProperies tmdbApiConfigurationProperies) {

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(
                        RestClientAdapter.create(
                                RestClient.builder()
                                        .baseUrl(tmdbApiConfigurationProperies.baseUrl())
                                        .defaultHeader("accept", "application/json")
                                        .defaultHeader("Authorization","Bearer " + tmdbApiConfigurationProperies.apiToken())
                                        .build()
                        )
                )
                .build();
        return factory.createClient(TmdbRemoteApi.class);
    }
}
