package bam.movies.suggestion.ai;

import bam.movies.suggestion.controller.MovieView;

import java.util.List;

public record RecommendationResult(
        String message,
        List<MovieView> picks
) {
}
