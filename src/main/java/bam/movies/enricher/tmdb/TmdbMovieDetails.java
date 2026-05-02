package bam.movies.enricher.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TmdbMovieDetails(
        int id,
        String title,
        String overview,
        List<Genre> genres,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("vote_average") double voteAverage,
        @JsonProperty("vote_count") int voteCount,
        @JsonProperty("poster_path") String posterPath,
        Integer runtime,
        @JsonProperty("tagline") String tagline,
        Keywords keywords,
        @JsonProperty("imdb_id") String imdbId,
        @JsonProperty("credits") Credits credits
) {
    public record Genre(int id, String name) {
    }

    public record Keyword(int id, String name) {
    }

    public record Keywords(List<Keyword> keywords) {

    }

    public record Credits(List<Cast> cast) {
    }

    public record Cast(String name, double popularity) {
    }
}
