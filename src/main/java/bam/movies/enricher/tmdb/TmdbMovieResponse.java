package bam.movies.enricher.tmdb;

import java.util.List;

public record TmdbMovieResponse(List<TmdbMovieDetails> results) {
}
