package dev.teggr.jre.interop;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/** Small JSON helpers; parsing failures yield null instead of a JS exception. */
public final class Json {
    private Json() {
    }

    @JSBody(params = "text", script = "try { return JSON.parse(text); } catch (e) { return null; }")
    public static native <T extends JSObject> T parse(String text);

    @JSBody(params = "value", script = "return Array.isArray(value);")
    public static native boolean isArray(JSObject value);

    @JSBody(params = {"object", "key"}, script = "return object === null ? 'null' : typeof object[key];")
    public static native String typeOfProperty(JSObject object, String key);

    @JSBody(params = "value", script = "return value !== null && typeof value === 'object';")
    public static native boolean isObject(JSObject value);
}
