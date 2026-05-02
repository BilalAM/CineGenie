package bam.movies.suggestion.ai;

import java.util.List;

public record AiPicks(
        String message,
        List<AiPick> picks
) {
}
