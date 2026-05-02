package bam.movies.suggestion;

import bam.movies.entity.Movie;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieSearchRepository extends JpaRepository<Movie, Long> {

    @Query("""
                SELECT DISTINCT m FROM Movie m
                WHERE m.status = :status
                  AND m.guessedType = 'movie'
                  AND (:tmdbRating IS NULL OR m.tmdbRating >= :tmdbRating)
                  AND (:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
            """)
    List<Movie> searchByScalarFilters(
            @Param("status") Movie.Status status,
            @Param("tmdbRating") Double tmdbRating,
            @Param("title") String title,
            Sort sort
    );
}
