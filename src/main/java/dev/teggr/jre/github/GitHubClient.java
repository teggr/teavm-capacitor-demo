package dev.teggr.jre.github;

import dev.teggr.jre.interop.Fetch;
import dev.teggr.jre.interop.Json;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArrayReader;

/** Fetches and validates a user's repositories from the public GitHub REST API. */
public final class GitHubClient {
    public interface Listener {
        void onSuccess(List<Repository> repositories);

        void onFailure(String message);
    }

    /** Raw JSON shape of one repository in the API response. */
    private interface RepoJson extends JSObject {
        @JSProperty
        String getName();

        @JSProperty
        String getDescription();

        @JSProperty
        String getLanguage();

        @JSProperty("stargazers_count")
        int getStars();

        @JSProperty("updated_at")
        String getUpdatedAt();
    }

    private GitHubClient() {
    }

    public static void fetchRepositories(String username, Listener listener) {
        String user = username == null ? "" : username.trim();
        if (user.isEmpty()) {
            listener.onFailure("Enter a GitHub username first.");
            return;
        }
        String url = "https://api.github.com/users/" + Fetch.encodeURIComponent(user)
                + "/repos?sort=updated&per_page=10";
        Fetch.getText(url, "application/vnd.github+json", new Fetch.Callback() {
            @Override
            public void onResponse(int status, String body, Map<String, String> headers) {
                String error = ResponseClassifier.errorFor(
                        status, user, headers.get("x-ratelimit-remaining"), headers.get("retry-after"));
                if (error != null) {
                    listener.onFailure(error);
                    return;
                }
                List<Repository> repositories = parse(body);
                if (repositories == null) {
                    listener.onFailure(ResponseClassifier.INVALID_DATA);
                } else {
                    listener.onSuccess(repositories);
                }
            }

            @Override
            public void onNetworkError(String message) {
                listener.onFailure(ResponseClassifier.networkError(message));
            }
        }, "x-ratelimit-remaining", "retry-after");
    }

    /** Returns null when the payload is not an array of repository objects. */
    private static List<Repository> parse(String body) {
        JSObject root = Json.parse(body);
        if (root == null || !Json.isArray(root)) {
            return null;
        }
        JSArrayReader<RepoJson> items = (JSArrayReader<RepoJson>) root;
        List<Repository> result = new ArrayList<>();
        for (int i = 0; i < items.getLength(); i++) {
            RepoJson item = items.get(i);
            if (!Json.isObject(item)
                    || !"string".equals(Json.typeOfProperty(item, "name"))
                    || !"number".equals(Json.typeOfProperty(item, "stargazers_count"))) {
                return null;
            }
            String description = "string".equals(Json.typeOfProperty(item, "description"))
                    ? item.getDescription() : null;
            String language = "string".equals(Json.typeOfProperty(item, "language"))
                    ? item.getLanguage() : null;
            String updated = "string".equals(Json.typeOfProperty(item, "updated_at"))
                    ? item.getUpdatedAt() : "";
            result.add(new Repository(item.getName(), description, language, item.getStars(), updated));
        }
        return result;
    }
}
