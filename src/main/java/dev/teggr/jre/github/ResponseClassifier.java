package dev.teggr.jre.github;

/** Maps HTTP outcomes to user-facing messages. Pure Java, so it is unit-testable on the JVM. */
public final class ResponseClassifier {
    private ResponseClassifier() {
    }

    /** Returns null for a success status, otherwise a message for the user. */
    public static String errorFor(int status, String username, String rateLimitRemaining, String retryAfter) {
        if (status >= 200 && status < 300) {
            return null;
        }
        if (status == 404) {
            return "No GitHub user named \"" + username + "\" was found.";
        }
        if (status == 429 || (status == 403 && "0".equals(rateLimitRemaining))) {
            String hint = retryAfter != null ? " Retry in about " + retryAfter + " seconds." : " Try again later.";
            return "GitHub's unauthenticated rate limit has been reached." + hint;
        }
        if (status == 403) {
            return "GitHub refused the request (HTTP 403).";
        }
        return "GitHub returned an unexpected response (HTTP " + status + ").";
    }

    public static String networkError(String detail) {
        return "Network error: " + detail + " Check your connection.";
    }

    public static final String INVALID_DATA = "GitHub returned data in an unexpected format.";
}
