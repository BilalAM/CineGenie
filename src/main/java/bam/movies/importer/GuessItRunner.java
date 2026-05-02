package bam.movies.importer;

import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;

@Service
public class GuessItRunner {
    private static final int TIMEOUT_FOR_COMMAND = 10;
    private static final String GUESSIT_COMMAND = "guessit";
    private final ObjectMapper mapper = new ObjectMapper();

    public GuessedMovie guessMovie(Path movie) {
        try {
            Process process = new ProcessBuilder()
                    .redirectErrorStream(false)
                    .command(List.of(GUESSIT_COMMAND, "-j", movie.toString()))
                    .start();
            if (!process.waitFor(TIMEOUT_FOR_COMMAND, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new RuntimeException("Timeout happened for running 'guessit " + movie + "' command");
            }
            if (process.exitValue() != 0) {
                throw new RuntimeException("Error happened for running 'guessit " + movie + "' command");
            }
            JsonNode rawOutput = mapper.readTree(process.getInputStream());
            return new GuessedMovie(
                    textOrNull(rawOutput, "title"),
                    textOrNull(rawOutput, "year"),
                    textOrNull(rawOutput, "type"),
                    movie
            );
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private String textOrNull(JsonNode node, String key) {
        return node.hasNonNull(key) ? node.get(key).asString() : null;
    }

    public record GuessedMovie(String title, String year, String type, Path movie) {
    }
}
