package bam;

import bam.movies.enricher.MovieEnricher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SuggestMoviesApplication implements CommandLineRunner {

    private final MovieEnricher enrichMovies;
    public SuggestMoviesApplication(MovieEnricher enrichMovies) {
        this.enrichMovies = enrichMovies;
    }

    public static void main(String[] args) {
        SpringApplication.run(SuggestMoviesApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        //enrichMovies.enrichMovies();
    }
}
