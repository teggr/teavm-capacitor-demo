package dev.teggr.jre.github;

/** Immutable repository summary shown in the list. */
public final class Repository {
    public final String name;
    public final String description;
    public final String language;
    public final int stars;
    public final String updated;

    public Repository(String name, String description, String language, int stars, String updated) {
        this.name = name;
        this.description = description;
        this.language = language;
        this.stars = stars;
        this.updated = updated;
    }
}
