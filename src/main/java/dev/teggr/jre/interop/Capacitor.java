package dev.teggr.jre.interop;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

/**
 * Generic, plugin-agnostic access to Capacitor's JavaScript plugin API
 * ({@code window.Capacitor}, injected by the native Android bridge).
 * Plugin-specific result types live outside this class.
 */
public final class Capacitor {
    private Capacitor() {
    }

    @JSBody(script = "return !!(window.Capacitor && window.Capacitor.isNativePlatform && window.Capacitor.isNativePlatform());")
    public static native boolean isNativePlatform();

    @JSBody(params = {"plugin", "method"}, script = ""
            + "var c = window.Capacitor;"
            + "if (!c) return false;"
            + "var p = (c.Plugins && c.Plugins[plugin]) || (c.registerPlugin && c.registerPlugin(plugin));"
            + "return !!p && typeof p[method] === 'function';")
    public static native boolean hasPluginMethod(String plugin, String method);

    @JSBody(params = {"plugin", "method"}, script = ""
            + "var c = window.Capacitor;"
            + "var p = (c.Plugins && c.Plugins[plugin]) || c.registerPlugin(plugin);"
            + "return p[method]();")
    private static native <T extends JSObject> JSPromise<T> call(String plugin, String method);

    /** Invokes {@code Capacitor.Plugins[plugin][method]()}; the promise rejects if the plugin is unavailable. */
    public static <T extends JSObject> JSPromise<T> invoke(String plugin, String method) {
        if (!hasPluginMethod(plugin, method)) {
            return JSPromise.reject("Capacitor plugin " + plugin + "." + method + " is not available");
        }
        return call(plugin, method);
    }
}
