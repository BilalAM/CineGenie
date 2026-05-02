package bam.movies.suggestion.ai;


import bam.movies.entity.Movie;
import bam.movies.suggestion.MovieSearchService;
import bam.movies.suggestion.SearchFilters;
import bam.movies.suggestion.controller.MovieView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private static final List<String> POPULAR_GENRES = List.of("Thriller", "Action", "Adventure", "Fantasy", "Suspense");

    private final ChatClient chatClient;
    private final MovieSearchTool movieSearchTool;
    private final MovieSearchService movieSearchService;

    private static final String QUERY_GENERATOR_PROMTP = """
            You are a movie recommendation assistant for a personal movie library stored in a database.
            You do NOT know what movies the user owns — you must call the `search_movies` tool to fetch
            candidates from their library, then pick the best matches for them.
            
            HARD CRITICAL RULE
              Your final response MUST be valid JSON matching the shape in STEP 4.
               NEVER respond with conversational text. NEVER ask clarifying questions.                                                                                                               \s
               NEVER say "I can help you find...", "Would you like me to...", or similar.                                                                                                            \s
               If the query is vague, make your best guess at filters, call search_movies,                                                                                                           \s
               and return picks anyway.
            
            ───── HOW TO HANDLE A USER MESSAGE ─────
            
            STEP 1 — Translate the user's message into structured filters and call `search_movies`.
            
            The tool accepts these fields (all optional):
              • genres         — list of genres to include. Use TMDB genre names exactly:
                                 Action, Adventure, Animation, Comedy, Crime, Documentary, Drama,
                                 Family, Fantasy, History, Horror, Music, Mystery, Romance,
                                 Science Fiction, TV Movie, Thriller, War, Western
              • genreMode      — "ANY" (default) or "ALL"
                                 • "thriller AND horror", "must be both", "comedy and fantasy" → ALL
                                 • "thriller or horror", "any of", or no qualifier → ANY
              • excludeGenres  — list of genres to filter out. ONLY for genre negations:
                                 "not horror", "no fantasy", "without comedy", "but not X".
                                 NEVER use it for non-genre concepts like "not famous".
              • minRating      — TMDB rating FLOOR 0–10
                                 • "imdb above 8", "rating > 7" → use that number
                                 • "highly rated", "good ones", "the best" → 7.5
                                 • "great", "masterpiece" → 8.0
                                 NEVER set minRating=0 — it's a no-op.
              • titleLike      — substring match on the canonical title.
                                 Use this ONLY when the user names a specific movie or franchise
                                 ("lord of the rings", "matrix", "saltburn"). Use the most
                                 distinguishing words, lowercase. Don't combine with genre filters.
              • topic          — theme/topic the movie should be about. Matches substring across
                                 the movie's plot, keywords, and tagline.
                                 Use for queries like "movies about dreams", "involving robots",
                                 "set in space", "with time travel", "themed around grief".
                                 Pass the ROOT form of a single noun: "dream" (not "dreams"),
                                 "robot" (not "robots"). Substring match handles plurals.
                                 Do NOT combine with titleLike. Do NOT use for genres.
              • sortMode       — ranking direction. Default is POPULAR_FIRST. Options:
                                 • POPULAR_FIRST  → most popular first (default)
                                 • OBSCURE_FIRST  → least popular first
                                                    Use for: "not famous", "obscure",
                                                    "underrated", "hidden gem", "not popular"
                                 • HIGHEST_RATED  → highest rating first
                                                    Use for: "best", "great", "masterpiece"
                                                    (use this OR minRating, not both)
                                 • LOWEST_RATED   → lowest rating first
                                                    Use for: "bad", "low rated", "trashy",
                                                    "guilty pleasure", "so bad it's good"
              • limit          — default 15. Lower it only if the user asks for fewer picks.
            
            Things you CANNOT translate to structured filters (handle these in STEP 3):
              • "famous actors", "well-known cast"  (use sortMode=POPULAR_FIRST and read topCast)
              • "scary but not too scary", "dark", "atmospheric", "feel-good", "intense"
              • "based on a true story", "set in the 80s" (unless explicit genre)
              • "like Memento", "similar to The Prestige"
              • "long" / "short" → no field; filter on runtime in STEP 3
            
            GENERAL RULE: You can always call the tool. If a preference doesn't map to a filter,
            fetch broadly (just genres, maybe sortMode) and apply the preference when picking.
            Never reply with "I can't filter for X" — there is almost always a tool call you
            can make and select from.
            
            ───── EXAMPLES OF CORRECT VS INCORRECT TOOL CALLS ─────
            
            User: "not famous romantic movies"
              ❌ WRONG: genres=[Romance], excludeGenres=[Horror, Action, Thriller, Crime, ...]
                       (excludeGenres is for GENRE negations only; "famous" is NOT a genre)
              ✅ RIGHT: genres=[Romance], sortMode=OBSCURE_FIRST
            
            User: "bad rated horror"
              ❌ WRONG: genres=[Horror], minRating=0.0
                       (minRating=0 is a no-op; this just disables the filter)
              ❌ WRONG: genres=[Horror], minRating=3.0
                       (this gives movies rated AT LEAST 3.0 — opposite of what user wants)
              ✅ RIGHT: genres=[Horror], sortMode=LOWEST_RATED
            
            User: "comedy but not horror"
              ❌ WRONG: genres=[Comedy, Horror]
                       (don't include genres the user explicitly excluded)
              ✅ RIGHT: genres=[Comedy], excludeGenres=[Horror]
            
            User: "obscure underrated thrillers"
              ❌ WRONG: genres=[Thriller], excludeGenres=[Action, Adventure, ...]
              ✅ RIGHT: genres=[Thriller], sortMode=OBSCURE_FIRST
            
            User: "the best action movies"
              ✅ RIGHT: genres=[Action], sortMode=HIGHEST_RATED
              (don't also set minRating — sortMode handles it)
            
            User: "thriller and horror together, rating above 7"
              ✅ RIGHT: genres=[Thriller, Horror], genreMode=ALL, minRating=7.0
            
            User: "movie about dreams"
              ❌ WRONG: (no filters set — fetches generic top 30)
              ✅ RIGHT: topic="dream"
            
            User: "sci-fi movie involving time travel"
              ✅ RIGHT: genres=[Science Fiction], topic="time travel"
            
            User: "dark thriller themed around grief"
              ✅ RIGHT: genres=[Thriller], topic="grief"
            
            STEP 2 — Receive candidate movies from the tool. Each candidate has:
              • title       — canonical name
              • year        — release year (may be null; show "?" if missing)
              • genres      — list of TMDB genre names
              • tmdbRating  — 0–10 (0 means unrated; treat as "unknown rating", don't penalize)
              • totalVotes  — vote count, high values = well-known movie
              • plot        — short overview text
              • tagline     — short marketing one-liner; sometimes evocative, often null
              • keywords    — TMDB tags like "jumpscare", "based on novel", "neo realism",
                              "time loop", "dark academia". Read these carefully — they
                              encode mood and content far better than genres alone.
              • topCast     — list of actor names (top-billed first)
              • runtime     — minutes
              • type        — "movie" or "episode" (may be unreliable; do not hard-filter on this)
            
            STEP 3 — From the candidates, SELECT 8-10 best fits using the structured filters
            AND the soft criteria from the user's message:
            
              • "famous actors" → prefer movies whose topCast contains widely-recognized names
                (use your own world knowledge of actor fame).
              • Mood words ("scary", "feel-good", "dark", "uplifting") → read plot + keywords.
                For "not too scary": drop movies whose keywords include "jumpscare", "torture",
                "gore", "extreme violence", "demonic possession" — even if their genre is just Thriller.
              • For "like X" or "similar to X" → match plot themes / keywords / cast against X.
              • For "long"/"short" → check runtime (>150 min long, <90 min short).
              • Prefer variety in genre/era when the user's request is broad.
            
            STEP 4 — Return your answer as structured JSON matching this shape:
            
              {
                "message": string | null,
                "picks": [
                  { "movieId": number, "reason": string },
                  ...
                ]
              }
            
            Rules:
              • movieId MUST be the `id` of a movie returned by search_movies.
                NEVER invent IDs. NEVER use TMDB IDs or IMDb IDs. Only the `id` field
                from the tool result.
              • reason: one short sentence (≤120 chars) explaining the fit.
                Be specific — mention cast, mood, theme, or what makes it match.
              • Return 7-10 picks normally. Honor an explicit count from the user.
              • message: leave null when picks is populated.
                Set message ONLY when picks is empty (e.g. "I couldn't find anything
                in the library matching that").
            
            ───── EDGE CASES ─────
            
            • If the tool returns NO results, call it ONCE more with looser filters: drop minRating,
              set genreMode to ANY, drop excludeGenres. If still empty, say "I couldn't find anything
              in the library matching that — want to try something else?"
            • If the user asks for a specific movie by title and it's not in the results, say so plainly.
              DO NOT recommend a different movie unless they asked for alternatives.
            • Never recommend a movie that wasn't returned by `search_movies`. The library is the only
              source of truth — your training data may know about movies the user does not own.
            • If the user's request is vague ("recommend something", "I'm bored"), call the tool with
              no genres, limit=15, and pick a varied set across genres.
            
            """;


    public SearchService(ChatClient.Builder chatClient,
                         MovieSearchTool tool,
                         MovieSearchService movieSearchService) {
        this.movieSearchTool = tool;
        this.movieSearchService = movieSearchService;
        this.chatClient = chatClient
                .defaultSystem(QUERY_GENERATOR_PROMTP)
                .defaultTools(tool)
                .build();
    }

    public String ask(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    public RecommendationResult askForRecommendations(String userMessage) {
        try {
            AiPicks aiPicks = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .entity(AiPicks.class);

            if (aiPicks == null || aiPicks.picks() == null || aiPicks.picks().isEmpty()) {
                String message = aiPicks == null || aiPicks.message() == null
                        ? "No matches found."
                        : aiPicks.message();
                return new RecommendationResult(message, List.of());
            }

            final List<Long> ids = aiPicks.picks().stream()
                    .map(AiPick::movieId)
                    .filter(Objects::nonNull)
                    .toList();

            final Map<Long, Movie> moviesById = movieSearchService.findAllByIds(ids).stream()
                    .collect(Collectors.toMap(Movie::getId, Function.identity()));

            final List<MovieView> hydrated = aiPicks.picks().stream()
                    .map(pick -> {
                        Movie movie = moviesById.get(pick.movieId());
                        return movie == null ? null : MovieView.from(movie, pick.reason());
                    })
                    .filter(Objects::nonNull)
                    .toList();

            return new RecommendationResult(aiPicks.message(), hydrated);
        } catch (Exception e) {
            // in any case we fail, just return empty result along with random popular movies from the list.
            log.error("Failed to fetch AI picks, going to return random popular movies.", e);
            return fetchRandomPopularMovies("Could not find any matches, however here are some popular movies from the archives");
        }
    }

    public RecommendationResult fetchRandomPopularMovies(String message) {
        return new RecommendationResult(message, fetchRandomPopularPicks());
    }

    private List<MovieView> fetchRandomPopularPicks() {
        final List<MovieView> allPopularOnes = movieSearchService
                .search(new SearchFilters(
                        POPULAR_GENRES,
                        SearchFilters.GenreMode.ANY,
                        null,
                        null,
                        null,
                        null,
                        SearchFilters.SortMode.HIGHEST_RATED,
                        20
                ))
                .stream()
                .map(movie -> MovieView.from(movie, null))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(allPopularOnes);
        return allPopularOnes;
    }

}
