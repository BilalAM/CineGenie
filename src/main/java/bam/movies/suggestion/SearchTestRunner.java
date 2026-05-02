package bam.movies.suggestion;

import bam.movies.entity.Movie;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchTestRunner implements CommandLineRunner {

    private final SearchService searchService;

    public SearchTestRunner(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public void run(String... args) {

        runQuery("movie that has case insensitive 'hobbit' in title",
                new SearchFilters(null, null, null, null, "hobbit", null, null, null));

        //SearchFilters[genres=null, genreMode=null, excludeGenres=null, minRating=null, titleLike=hobbit, topic=null, sortMode=null, limit=null]
    }

    private void runQuery(String label, SearchFilters filters) {
        System.out.println();
        System.out.println("--- " + label + " ---");
        List<Movie> results = searchService.search(filters);
        if (results.isEmpty()) {
            System.out.println("(no results)");
            return;
        }
        for (Movie movie : results) {
            double rating = movie.getTmdbRating() == null ? 0.0 : movie.getTmdbRating();
            String year = movie.getYear() == null ? "?" : movie.getYear().toString();
            System.out.printf("  [%.1f] %s (%s) - %s%n",
                    rating,
                    movie.getTitle(),
                    year,
                    movie.getGenres());
        }
    }

}
