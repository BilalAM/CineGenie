package bam.movies.enricher.tmdb;


import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(accept = "application/json")
public interface TmdbRemoteApi {

    @GetExchange("/search/movie")
    TmdbMovieResponse fetchMovie(@RequestParam("query") String movieName);

    @GetExchange("/movie/{movieId}")
    TmdbMovieDetails fetchMovieComplete(@PathVariable int movieId,
                                                   @RequestParam(value = "append_to_response") String appendToResponse);


}
