package bam.movies.suggestion.ai;

import bam.movies.suggestion.MovieSearchService;
import bam.movies.suggestion.SearchFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("LoggingSimilarMessage")
@Component
public class MovieSearchTool {

    private final MovieSearchService movieSearchService;
    private final Logger log = LoggerFactory.getLogger(MovieSearchTool.class);


    public MovieSearchTool(MovieSearchService movieSearchService) {
        this.movieSearchService = movieSearchService;
    }

    @Tool(description = """
             Search the user's personal movie library by genre, rating, title, theme, and ranking direction.
             Returns a list of candidate movies that the assistant can choose from.
             All parameters are optional — omit any you don't need.
            """)
    public List<MovieToolResult> searchMovies(
            @ToolParam(required = false, description = """
                     List of TMDB genre names to include. Valid values: Action, Adventure, Animation,
                     Comedy, Crime, Documentary, Drama, Family, Fantasy, History, Horror, Music,
                     Mystery, Romance, Science Fiction, TV Movie, Thriller, War, Western.
                     Match against the movie's `genres` field.
                    """)
            List<String> genres,

            @ToolParam(required = false, description = """
                     Genre matching mode. Use "ANY" (default) when the user is open to any of the
                     listed genres, or "ALL" when the user wants every listed genre to be present
                     (e.g. "comedy AND horror" → ALL).
                    """)
            SearchFilters.GenreMode genreMode,

            @ToolParam(required = false, description = """
                     Genres to filter OUT. Movies tagged with any of these will be excluded.
                     Use ONLY for genre negations like "not horror", "no fantasy", "without romance".
                     DO NOT use this for non-genre concepts like "not famous" or "not popular" —
                     use sortMode for those instead.
                    """)
            List<String> excludeGenres,

            @ToolParam(required = false, description = """
                     Minimum TMDB rating, on a 0–10 scale. Filters out movies rated below this value.
                     Use 7.5 for "highly rated", 8.0 for "great" or "masterpiece".
                     Do NOT set this to 0 — it's a no-op and just disables the filter.
                    """)
            Double minRating,

            @ToolParam(required = false, description = """
                     Substring of a movie title (case-insensitive). Use ONLY when the user names a
                     specific movie or franchise (e.g. "matrix", "lord of the rings", "saltburn").
                     Provide the most distinguishing words, lowercase. Do NOT combine with genres.
                    """)
            String titleLike,

            @ToolParam(required = false, description = """
                     Theme/topic the movie should be about. Matches substring across the movie's
                     plot, keywords, and tagline (case-insensitive). Use for queries like
                     "movies about dreams", "involving robots", "set in space", "with time travel".
                     Pass the ROOT form of a single noun: "dream" (not "dreams"), "robot" (not "robots").
                     Substring match means "dream" finds both "dream" and "dreams" in text.
                     Do NOT combine with titleLike (different intents).
                    """)
            String topic,

            @ToolParam(required = false, description = """
                     Ranking direction for the result set. Default is POPULAR_FIRST. Options:
                     • POPULAR_FIRST  — most well-known movies first (default)
                     • OBSCURE_FIRST  — least popular first; use for "not famous", "obscure",
                                        "underrated", "hidden gem"
                     • HIGHEST_RATED  — highest tmdbRating first; use for "best", "great",
                                        "masterpiece" (use this OR minRating, not both)
                     • LOWEST_RATED   — lowest tmdbRating first (rating=0 entries are excluded);
                                        use for "bad", "trashy", "guilty pleasure", "so bad it's good"
                    """)
            SearchFilters.SortMode sortMode,

            @ToolParam(required = false, description = """
                     Maximum number of candidate movies to return. Default 15. Lower only if the
                     user asks for a small explicit number.
                    """)
            Integer limit
    ) {
        final SearchFilters filters = new SearchFilters(
                genres, genreMode, excludeGenres, minRating, titleLike, topic, sortMode, limit
        );
        log.info("Tool {} called with [  {}  ]", Thread.currentThread().getStackTrace()[1].getMethodName(), filters);
        final List<MovieToolResult> results = movieSearchService.search(filters).stream()
                .map(MovieToolResult::from)
                .collect(Collectors.toList());
        log.info("Returning {} movies to LLM", results.size());
        return results;
    }

    @Tool(description = """
             Find movies in the user's library that are similar to a given reference title.
             Use this whenever the user asks for movies LIKE another movie — e.g.
             "movies like Interstellar", "Inception-style films", "something similar to Memento",
             "Saltburn type movies", "in the vein of The Prestige".

             The tool resolves the reference movie by title, then returns up to 10 OTHER
             movies that share genres with it (the reference movie itself is excluded).
             Returns an empty list if the reference movie is NOT in the user's archive.

             Do NOT pair this with search_movies for the same query — this tool returns
             the complete candidate set on its own.
            """)
    public List<MovieToolResult> findSimilarMovies(@ToolParam(required = true, description = """
             The reference movie's title. Pass only the most distinguishing words, lowercase.
             Examples: "interstellar", "memento", "lord of the rings", "saltburn".
             Strip articles and noise: "the prestige" → "prestige" is fine; "interstellar 2014" → "interstellar".
            """) String title) {
        log.info("Tool {} called called with [  {}  ]", Thread.currentThread().getStackTrace()[1].getMethodName(), title);
        final MovieToolResult firstMatching = movieSearchService.search(
                        new SearchFilters(
                                null, null, null, null, title, null, null, null
                        )
                )
                .stream()
                .map(MovieToolResult::from)
                .findFirst()
                .orElse(null);
        if (firstMatching == null) {
            return Collections.emptyList();
        }
        final SearchFilters filters = new SearchFilters(
                firstMatching.genres(),
                SearchFilters.GenreMode.ANY,
                null,
                null,
                null,
                null,
                SearchFilters.SortMode.POPULAR_FIRST,
                10
        );
        return movieSearchService.search(filters)
                .stream()
                // we do not want to return the movie that the user originally mentioned as what they want similar for.
                .filter(movie -> !movie.getTitle().toLowerCase().contains(title.toLowerCase()))
                .map(MovieToolResult::from).toList();
    }

}
