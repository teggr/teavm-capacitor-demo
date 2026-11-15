package dev.teggr.jre.interop;

import java.util.HashMap;
import java.util.Map;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSString;
import org.teavm.jso.core.JSUndefined;

/**
 * Minimal binding to the browser Fetch API (TeaVM's JSO APIs don't ship one).
 * The only JavaScript here is the one-line {@code fetch} call in {@link #request}.
 */
public final class Fetch {
    private Fetch() {
    }

    public interface Response extends JSObject {
        @JSProperty
        int getStatus();

        JSPromise<JSString> text();
    }

    public interface Callback {
        void onResponse(int status, String body, Map<String, String> headers);

        void onNetworkError(String message);
    }

    @JSBody(params = {"url", "accept"},
            script = "return fetch(url, { headers: { 'Accept': accept } });")
    private static native JSPromise<Response> request(String url, String accept);

    @JSBody(params = {"response", "name"},
            script = "return response.headers.get(name);")
    private static native String header(Response response, String name);

    @JSBody(params = "value", script = "return encodeURIComponent(value);")
    public static native String encodeURIComponent(String value);

    /** Performs a GET and reports status, body and the requested response headers (null when absent). */
    public static void getText(String url, String accept, Callback callback, String... headerNames) {
        request(url, accept).then(
                response -> {
                    int status = response.getStatus();
                    Map<String, String> headers = new HashMap<>();
                    for (String name : headerNames) {
                        headers.put(name, header(response, name));
                    }
                    return response.text().then(
                            text -> {
                                callback.onResponse(status, text.stringValue(), headers);
                                return JSUndefined.instance();
                            },
                            error -> {
                                callback.onNetworkError("Could not read the response body.");
                                return null;
                            });
                },
                error -> {
                    callback.onNetworkError("The request could not be sent.");
                    return null;
                });
    }
}
