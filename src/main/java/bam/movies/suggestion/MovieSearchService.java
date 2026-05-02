package bam.movies.suggestion;

import bam.movies.entity.Movie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class MovieSearchService {

    private static final int DEFAULT_LIMIT = 15;

    private final MovieSearchRepository repository;

    public MovieSearchService(MovieSearchRepository repository) {
        this.repository = repository;
    }

    public List<Movie> search(@Nullable final SearchFilters filters) {
        final Sort sort = buildSort(filters.sortMode());

        final List<Movie> rows = repository.searchByScalarFilters(
                Movie.Status.ENRICHED,
                filters.minRating(),
                filters.titleLike(),
                sort
        );

        int limit = filters.limit() == null ? DEFAULT_LIMIT : filters.limit();

        return rows.stream()
                .filter(movie -> matchesGenreFilters(movie, filters))
                .filter(movie -> matchesTopic(movie, filters.topic()))
                .filter(movie -> matchesSortMode(movie, filters.sortMode()))
                .limit(limit)
                .toList();
    }

    public List<Movie> findAllByIds(@NotNull final List<Long> ids) {
        return repository.findAllById(ids);
    }

    private Sort buildSort(final SearchFilters.SortMode mode) {
        final SearchFilters.SortMode resolved = mode == null
                ? SearchFilters.SortMode.POPULAR_FIRST
                : mode;
        return switch (resolved) {
            case POPULAR_FIRST -> Sort.by(Sort.Order.desc("totalVotes"), Sort.Order.desc("tmdbRating"));
            case OBSCURE_FIRST -> Sort.by(Sort.Order.asc("totalVotes"));
            case HIGHEST_RATED -> Sort.by(Sort.Order.desc("tmdbRating"), Sort.Order.desc("totalVotes"));
            case LOWEST_RATED -> Sort.by(Sort.Order.asc("tmdbRating"));
        };
    }

    private boolean matchesSortMode(@NotNull final Movie movie, @Nullable final SearchFilters.SortMode mode) {
        if (mode == SearchFilters.SortMode.LOWEST_RATED) {
            return movie.getTmdbRating() != null && movie.getTmdbRating() > 0;
        }
        return true;
    }

    private boolean matchesTopic(@NotNull final Movie movie, @Nullable final String topic) {
        if (topic == null || topic.isBlank()) {
            return true;
        }
        String needle = topic.toLowerCase();

        if (containsIgnoreCase(movie.getPlot(), needle)) {
            return true;
        }
        if (containsIgnoreCase(movie.getTagline(), needle)) {
            return true;
        }
        final List<String> keywords = movie.getKeywords();
        if (keywords != null) {
            for (String keyword : keywords) {
                if (containsIgnoreCase(keyword, needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(String haystack, String lowercaseNeedle) {
        return haystack != null && haystack.toLowerCase().contains(lowercaseNeedle);
    }

    private boolean matchesGenreFilters(Movie movie, SearchFilters filters) {
        final List<String> movieGenres = movie.getGenres() == null ? List.of() : movie.getGenres();
        final Set<String> have = Set.copyOf(movieGenres);

        if (filters.excludeGenres() != null) {
            for (String excluded : filters.excludeGenres()) {
                if (have.contains(excluded)) {
                    return false;
                }
            }
        }

        if (filters.genres() == null || filters.genres().isEmpty()) {
            return true;
        }

        final SearchFilters.GenreMode mode = filters.genreMode() == null
                ? SearchFilters.GenreMode.ANY
                : filters.genreMode();

        return switch (mode) {
            case ANY -> filters.genres().stream().anyMatch(have::contains);
            case ALL -> have.containsAll(filters.genres());
        };
    }
}
