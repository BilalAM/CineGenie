package bam.movies.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    Optional<Movie> findByFilePath(String filePath);

    Page<Movie> findAllByStatus(Movie.Status status, Pageable pageable);
}
