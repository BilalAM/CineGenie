package bam.movies.suggestion.controller;

import bam.movies.entity.Movie;

import java.util.List;

public record MovieView(
        Long id,
        String title,
        Integer year,
        Double tmdbRating,
        Integer runtime,
        String plot,
        String tagline,
        String posterUrl,
        List<String> genres,
        List<String> topCast,
        String reason
) {

    public static MovieView from(Movie movie, String reason) {
        return new MovieView(
                movie.getId(),
                movie.getTitle(),
                movie.getYear(),
                movie.getTmdbRating(),
                movie.getRuntime(),
                movie.getPlot(),
                movie.getTagline(),
                movie.getPosterPath(),
                movie.getGenres(),
                movie.getTopCast(),
                reason
        );
    }
}
