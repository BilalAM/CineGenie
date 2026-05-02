package bam.movies.enricher;

import bam.movies.entity.Movie;
import bam.movies.entity.MovieRepository;
import bam.movies.enricher.tmdb.TmdbApiConfigurationProperies;
import bam.movies.enricher.tmdb.TmdbMovieDetails;
import bam.movies.enricher.tmdb.TmdbMovieResponse;
import bam.movies.enricher.tmdb.TmdbRemoteApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
public class MovieEnricher {
    private final TmdbApiConfigurationProperies tmdbApiConfigurationProperies;
    private final TmdbRemoteApi tmdbRemoteApi;
    private final MovieRepository movieRepository;
    private final Logger log = LoggerFactory.getLogger(MovieEnricher.class);

    public MovieEnricher(TmdbRemoteApi tmdbRemoteApi,
                         MovieRepository movieRepository,
                         TmdbApiConfigurationProperies tmdbApiConfigurationProperies) {
        this.tmdbRemoteApi = tmdbRemoteApi;
        this.movieRepository = movieRepository;
        this.tmdbApiConfigurationProperies = tmdbApiConfigurationProperies;
    }

    public void enrichMovies() {
        Slice<Movie> movies;
        do {
            movies = movieRepository.findAllByStatus(Movie.Status.GUESSED, PageRequest.of(0, 10));
            movies.forEach(movie -> {
                log.atInfo()
                        .log("Enriching movie {}", movie.getTitle());
                final TmdbMovieResponse tmdbMovieResponse = tmdbRemoteApi.fetchMovie(movie.getTitle());
                if (!tmdbMovieResponse.results().isEmpty()) {
                    final TmdbMovieDetails movieDetails = tmdbRemoteApi.fetchMovieComplete(tmdbMovieResponse.results().get(0).id(),
                            "keywords,credits");
                    if (movieDetails != null) {
                        doEnrichment(movie, movieDetails);
                        log.atInfo()
                                .log("Enriched movie {} ({})", movie.getTitle(), movie.getYear());
                    }
                } else {
                    log.atWarn()
                            .log("Failed to enrich movie {}", movie.getTitle());
                    markAsFailed(movie);
                }
            });
        }
        while (!movies.isEmpty());
    }

    private void markAsFailed(Movie movie) {
        movie.markStatus(Movie.Status.FAILED);
        movieRepository.save(movie);
    }

    private void doEnrichment(final Movie movie, final TmdbMovieDetails movieDetails) {
        movie.setKeywords(movieDetails.keywords().keywords().stream().map(TmdbMovieDetails.Keyword::name).toList());
        movie.setGenres(movieDetails.genres().stream().map(TmdbMovieDetails.Genre::name).toList());
        movie.setTmdbId((long) movieDetails.id());
        movie.setImdbId(movieDetails.imdbId());
        if (movieDetails.credits() != null) {
            movie.setTopCast(
                    movieDetails.credits().cast().stream().filter(s -> s.popularity() > 3.5).map(TmdbMovieDetails.Cast::name).toList()
            );
        }
        movie.setTotalVotes(movieDetails.voteCount());
        movie.setTmdbRating(movieDetails.voteAverage());
        movie.setRuntime(movieDetails.runtime());
        movie.markStatus(Movie.Status.ENRICHED);
        movie.setPlot(movieDetails.overview());
        movie.setPosterPath("https://image.tmdb.org/t/p/w500" + movieDetails.posterPath());
        movieRepository.save(movie);
    }
}
