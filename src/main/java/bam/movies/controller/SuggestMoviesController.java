package bam.movies.controller;


import bam.movies.suggestion.ai.RecommendationResult;
import bam.movies.suggestion.ai.SearchAiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SuggestMoviesController {
    private final SearchAiService searchAiService;

    public SuggestMoviesController(SearchAiService searchAiService) {
        this.searchAiService = searchAiService;
    }


    @GetMapping("/askMe")
    public RecommendationResult recommendMe(@RequestParam String query) {
        return searchAiService.askForRecommendations(query);
    }
}
