package bam.movies.importer;

import bam.movies.entity.Movie;
import bam.movies.entity.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class MovieImporter {

    private static final Logger log = LoggerFactory.getLogger(MovieImporter.class);

    private final MovieTraverser movieTraverser;
    private final GuessItRunner guessItRunner;
    private final MovieRepository movieRepository;

    public MovieImporter(MovieTraverser movieTraverser,
                         GuessItRunner guessItRunner,
                         MovieRepository movieRepository) {
        this.movieTraverser = movieTraverser;
        this.guessItRunner = guessItRunner;
        this.movieRepository = movieRepository;
    }

    public void importAll() {
        List<Path> paths = movieTraverser.fetchMoviePaths(null);
        log.info("Found {} candidate files", paths.size());

        int imported = 0;
        int skipped = 0;
        int failed = 0;

        for (Path path : paths) {
            String filePath = path.toString();

            if (movieRepository.findByFilePath(filePath).isPresent()) {
                skipped++;
                continue;
            }

            try {
                GuessItRunner.GuessedMovie guessed = guessItRunner.guessMovie(path);
                Movie movie = new Movie(
                        filePath,
                        guessed.title(),
                        parseYear(guessed.year()),
                        guessed.type(),
                        Movie.Status.GUESSED
                );
                movieRepository.save(movie);
                imported++;
                log.info("Imported: {} ({})", guessed.title(), guessed.year());
            } catch (Exception e) {
                failed++;
                log.error("Failed to import {}: {}", path, e.getMessage());
            }
        }

        log.info("Import done. imported={}, skipped={}, failed={}", imported, skipped, failed);
    }

    private static Integer parseYear(String year) {
        if (year == null || year.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(year);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
