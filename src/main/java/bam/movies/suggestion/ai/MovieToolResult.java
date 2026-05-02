package bam.movies.suggestion.ai;

import bam.movies.entity.Movie;

import java.util.List;

public record MovieToolResult(
        Long id,
        String title,
        Integer year,
        List<String> genres,
        Double tmdbRating,
        Integer totalVotes,
        String plot,
        List<String> keywords,
        List<String> topCast
) {
    private static final int PLOT_MAX_CHARS = 200;
    private static final int KEYWORDS_MAX = 10;
    private static final int CAST_MAX = 5;

    public static MovieToolResult from(Movie movie) {
        return new MovieToolResult(
                movie.getId(),
                movie.getTitle(),
                movie.getYear(),
                movie.getGenres(),
                movie.getTmdbRating(),
                movie.getTotalVotes(),
                truncate(movie.getPlot(), PLOT_MAX_CHARS),
                limit(movie.getKeywords(), KEYWORDS_MAX),
                limit(movie.getTopCast(), CAST_MAX)
        );
    }

    private static String truncate(String text, int max) {
        if (text == null) return null;
        return text.length() > max ? text.substring(0, max) + "…" : text;
    }

    private static <T> List<T> limit(List<T> list, int max) {
        if (list == null) return null;
        return list.size() > max ? list.subList(0, max) : list;
    }
}
