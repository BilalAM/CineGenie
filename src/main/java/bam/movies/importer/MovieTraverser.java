package bam.movies.importer;

import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Service
public class MovieTraverser {
    private static final List<String> SUPPORTED_FORMATS = List.of(".mkv", ".mp4", ".avi");
    private static final String MOVIE_DIRECTORY = "/Volumes/One Touch/_data";

    public List<Path> fetchMoviePaths(String fromDirectory) {
        try (Stream<Path> path = Files.walk(Paths.get(MOVIE_DIRECTORY))) {
            return path
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedFormat)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isSupportedFormat(final Path moviePath) {
        final String name = moviePath.getFileName().toString();
        return SUPPORTED_FORMATS.contains(name.toLowerCase().substring(name.lastIndexOf(".")));
    }
}
