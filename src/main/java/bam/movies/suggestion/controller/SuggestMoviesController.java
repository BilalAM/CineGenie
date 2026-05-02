package bam.movies.suggestion.controller;


import bam.movies.suggestion.ai.RecommendationResult;
import bam.movies.suggestion.ai.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SuggestMoviesController {
    private final SearchService searchService;

    public SuggestMoviesController(SearchService searchService) {
        this.searchService = searchService;
    }


    @GetMapping("/askMe")
    public RecommendationResult recommendMe(@RequestParam String query) {
        return searchService.askForRecommendations(query);
    }

    @GetMapping("/recommended")
    public RecommendationResult popularOnes() {
        return searchService.fetchRandomPopularMovies("Here are some popular movies from the archives");
    }
}
