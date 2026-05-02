package bam.movies.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String filePath;

    private String title;

    private Integer guessedYear;

    private String guessedType;

    private Long tmdbId;

    private String imdbId;

    private Integer year;

    private Double tmdbRating;

    private Integer totalVotes;

    private Integer runtime;

    @Column(columnDefinition = "text")
    private String plot;

    private String tagline;

    private String posterPath;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> genres;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> keywords;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> topCast;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant enrichedAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected Movie() {
    }

    public Movie(String filePath,
                 String title,
                 Integer guessedYear,
                 String guessedType,
                 Status status) {
        this.filePath = filePath;
        this.title = title;
        this.guessedYear = guessedYear;
        this.guessedType = guessedType;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public enum Status {
        GUESSED, ENRICHED, FAILED
    }

    public void markStatus(Status status) {
        this.status = status;
        if (status == Status.ENRICHED) {
            this.enrichedAt = Instant.now();
        }
    }
    public Long getId() {
        return id;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getTitle() {
        return title;
    }

    public Integer getGuessedYear() {
        return guessedYear;
    }

    public String getGuessedType() {
        return guessedType;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Long tmdbId) {
        this.tmdbId = tmdbId;
    }

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Double getTmdbRating() {
        return tmdbRating;
    }

    public void setTmdbRating(Double tmdbRating) {
        this.tmdbRating = tmdbRating;
    }

    public Integer getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(Integer popularity) {
        this.totalVotes = popularity;
    }

    public Integer getRuntime() {
        return runtime;
    }

    public void setRuntime(Integer runtime) {
        this.runtime = runtime;
    }

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<String> getTopCast() {
        return topCast;
    }

    public void setTopCast(List<String> topCast) {
        this.topCast = topCast;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEnrichedAt() {
        return enrichedAt;
    }
}
