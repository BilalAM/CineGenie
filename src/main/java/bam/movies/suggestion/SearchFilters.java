package bam.movies.suggestion;

import java.util.List;

public record SearchFilters(
        List<String> genres,
        GenreMode genreMode,
        List<String> excludeGenres,
        Double minRating,
        String titleLike,
        String topic,
        SortMode sortMode,
        Integer limit
) {
    public enum GenreMode {
        ANY, ALL
    }

    public enum SortMode {
        POPULAR_FIRST, OBSCURE_FIRST, HIGHEST_RATED, LOWEST_RATED
    }

}
