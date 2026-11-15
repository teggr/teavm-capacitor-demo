package dev.teggr.jre.device;

import dev.teggr.jre.interop.Capacitor;
import java.util.function.Consumer;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSUndefined;

/** Plugin-specific wrapper for the official {@code @capacitor/device} plugin. */
public final class DeviceInfo {
    /** Shape of the object returned by {@code Device.getInfo()}. */
    interface Result extends JSObject {
        @JSProperty
        String getPlatform();

        @JSProperty
        String getOperatingSystem();

        @JSProperty
        String getOsVersion();

        @JSProperty
        String getModel();
    }

    public final String platform;
    public final String operatingSystem;
    public final String osVersion;
    public final String model;

    private DeviceInfo(Result r) {
        this.platform = r.getPlatform();
        this.operatingSystem = r.getOperatingSystem();
        this.osVersion = r.getOsVersion();
        this.model = r.getModel();
    }

    public static void load(Consumer<DeviceInfo> onSuccess, Consumer<String> onFailure) {
        Capacitor.<Result>invoke("Device", "getInfo").then(
                result -> {
                    onSuccess.accept(new DeviceInfo(result));
                    return JSUndefined.instance();
                },
                error -> {
                    onFailure.accept("Device plugin call failed.");
                    return null;
                });
    }
}
