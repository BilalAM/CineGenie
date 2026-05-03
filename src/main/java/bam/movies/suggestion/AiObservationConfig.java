package bam.movies.suggestion;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiObservationConfig {

    private static final Logger log = LoggerFactory.getLogger("ai-tokens");

    @Bean
    ObservationRegistryCustomizer<ObservationRegistry> aiTokenLogger() {
        return registry -> registry.observationConfig().observationHandler(
                new ObservationHandler<Observation.Context>() {
                    @Override
                    public void onStop(Observation.Context ctx) {
                        log.info("Obs [{}] keys={}", ctx.getName(), ctx.getAllKeyValues());
                    }

                    @Override
                    public boolean supportsContext(Observation.Context context) {
                        return true;
                    }
                });
    }
}
