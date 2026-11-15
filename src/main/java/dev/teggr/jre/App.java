package dev.teggr.jre;

import dev.teggr.jre.device.DeviceInfo;
import dev.teggr.jre.github.GitHubClient;
import dev.teggr.jre.github.Repository;
import dev.teggr.jre.interop.Capacitor;
import java.util.List;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

/** Java Repository Explorer: all behaviour lives here; index.html is only a static shell. */
public final class App {
    private final HTMLDocument document = Window.current().getDocument();
    private final HTMLInputElement usernameInput = (HTMLInputElement) element("username");
    private final HTMLButtonElement loadButton = (HTMLButtonElement) element("load");
    private final HTMLElement loading = element("loading");
    private final HTMLElement errorBox = element("error");
    private final HTMLElement errorMessage = element("error-message");
    private final HTMLElement empty = element("empty");
    private final HTMLElement list = element("repos");
    private int requestCounter;

    private static boolean started;

    public static void main(String[] args) {
        // index.html also calls main() on load, so guard against a double start
        if (started) {
            return;
        }
        started = true;
        new App().start();
    }

    private HTMLElement element(String id) {
        return Window.current().getDocument().getElementById(id);
    }

    private void start() {
        element("form").addEventListener("submit", event -> {
            event.preventDefault();
            loadRepositories();
        });
        element("retry").addEventListener("click", event -> loadRepositories());
        loadRuntimeInfo();
        loadRepositories();
    }

    private void loadRepositories() {
        int requestId = ++requestCounter;
        showLoading();
        GitHubClient.fetchRepositories(usernameInput.getValue(), new GitHubClient.Listener() {
            @Override
            public void onSuccess(List<Repository> repositories) {
                if (requestId == requestCounter) {
                    showRepositories(repositories);
                }
            }

            @Override
            public void onFailure(String message) {
                if (requestId == requestCounter) {
                    showError(message);
                }
            }
        });
    }

    private void showLoading() {
        loadButton.setDisabled(true);
        loading.setHidden(false);
        errorBox.setHidden(true);
        empty.setHidden(true);
        list.setHidden(true);
    }

    private void showError(String message) {
        loadButton.setDisabled(false);
        loading.setHidden(true);
        errorMessage.setTextContent(message);
        errorBox.setHidden(false);
    }

    private void showRepositories(List<Repository> repositories) {
        loadButton.setDisabled(false);
        loading.setHidden(true);
        list.setInnerHTML("");
        if (repositories.isEmpty()) {
            empty.setHidden(false);
            return;
        }
        for (Repository repository : repositories) {
            list.appendChild(renderRepository(repository));
        }
        list.setHidden(false);
    }

    private HTMLElement renderRepository(Repository repository) {
        HTMLElement item = create("li", null, null);
        item.appendChild(create("h3", "repo-name", repository.name));
        if (repository.description != null) {
            item.appendChild(create("p", "repo-description", repository.description));
        }
        HTMLElement meta = create("p", "repo-meta", null);
        if (repository.language != null) {
            meta.appendChild(create("span", "language", repository.language));
        }
        meta.appendChild(create("span", null, "\u2605 " + repository.stars));
        if (!repository.updated.isEmpty()) {
            meta.appendChild(create("span", null,
                    "Updated " + repository.updated.substring(0, Math.min(10, repository.updated.length()))));
        }
        item.appendChild(meta);
        return item;
    }

    private HTMLElement create(String tag, String className, String text) {
        HTMLElement created = document.createElement(tag);
        if (className != null) {
            created.setClassName(className);
        }
        if (text != null) {
            created.setTextContent(text);
        }
        return created;
    }

    private void loadRuntimeInfo() {
        HTMLElement status = element("runtime-status");
        if (!Capacitor.isNativePlatform()) {
            status.setTextContent("Running in a plain browser: Capacitor native bridge not present.");
            return;
        }
        DeviceInfo.load(info -> {
            status.setTextContent("Reported by the Capacitor Device plugin:");
            element("platform").setTextContent(info.platform);
            element("os-version").setTextContent(info.operatingSystem + " " + info.osVersion);
            element("model").setTextContent(info.model);
            element("runtime-details").setHidden(false);
        }, message -> status.setTextContent(message));
    }
}
