# Java Repository Explorer (TeaVM + Capacitor)

A small proof of concept: **Java** owns the application behaviour, **TeaVM** compiles it to JavaScript, and **Capacitor** packages it as an **Android** app. The app lists a GitHub user's repositories (public REST API, no auth) and shows device info obtained from the official Capacitor **Device** plugin.

> **Status: written but not yet built or run.** The authoring environment had no Android SDK (`dl.google.com` was unreachable) and no builds were executed, so nothing below has been verified end to end. See [Verification status](#verification-status).

## Architecture

```
Java source ──► Maven + TeaVM ──► target/web/js/app.js ─┐
src/main/webapp (index.html, style.css) ────────────────┴► target/web ──► Capacitor (webDir) ──► Android WebView
```

| Layer | Responsibility |
|---|---|
| Java (`src/main/java/dev/teggr/jre`) | All behaviour: input handling, GitHub API orchestration, validation, error messages, DOM rendering, device-info display |
| TeaVM | Compiles Java bytecode to a classic script (`js/app.js`); provides JSO/DOM/Promise bindings |
| Browser APIs | DOM, `fetch`, `JSON`, `Promise` (reached via TeaVM JSO) |
| Capacitor | Native Android shell, WebView, `window.Capacitor` bridge, Device plugin |

Packages:

- `dev.teggr.jre.interop` – the thin boundary: `Fetch` (fetch binding), `Json` (safe parse/type checks), `Capacitor` (generic `invoke(plugin, method)` returning a `JSPromise`). The only JavaScript is in short `@JSBody` snippets here.
- `dev.teggr.jre.device.DeviceInfo` – plugin-specific parsing of `Device.getInfo()`, separate from the generic bridge.
- `dev.teggr.jre.github` – API client, response validation, and `ResponseClassifier` (pure Java, unit-tested).
- `src/main/webapp` – static HTML/CSS shell. The only handwritten JS is `onload="main()"`.
- `native/` – Capacitor packaging (`package.json`, `capacitor.config.json`); `native/android` is generated.

`Capacitor.invoke` uses `window.Capacitor.Plugins[name]` (falling back to `registerPlugin`), which the native Android bridge injects, so no JS bundler is needed. Outside a native app it reports that the bridge is absent.

## Prerequisites

- JDK 21+ (Maven build targets release 17; Capacitor 8's Android build wants a recent JDK)
- Node.js 22+ and npm
- Android Studio or the Android SDK (`ANDROID_HOME` set), plus an emulator or device with USB debugging
- Internet access (Maven Central, npm, GitHub API)

Pinned: TeaVM 0.16.0, Capacitor 8.6.0, `@capacitor/device` 8.0.3, Maven 3.9.11 (via wrapper).

## Build and run

Use the Maven wrapper (`mvnw.cmd` on Windows).

```sh
# 1. Java -> JavaScript (also copies index.html/style.css into target/web)
./mvnw clean package

# 2. Capacitor / Android
cd native
npm install                  # commit the generated package-lock.json
npx cap add android          # first time only; creates native/android
npx cap sync android         # copies ../target/web and plugins into the Android project
npx cap run android          # build, install and launch on a device/emulator
# or: npx cap open android   # open in Android Studio
```

Repeat step 1 and `npx cap sync android` after every Java change.

To try the UI in a browser first (no native bridge, so the runtime section says so):

```sh
cd target/web && python3 -m http.server 8080   # then open http://localhost:8080
```

## Troubleshooting

- **Blank page / `main is not defined`**: check that `target/web/js/app.js` exists. If your TeaVM output does not expose a global `main`, adjust `jsModuleType` in `pom.xml` or the bootstrap in `index.html`.
- **Runtime info says the plugin is unavailable**: run `npx cap sync android` after `npm install` so the Device plugin is registered natively.
- **Debugging**: open `chrome://inspect` with the device attached to inspect the WebView console; use `adb logcat | grep -i capacitor` for native logs. Source maps are generated for the Java sources.
- **Rate limit message**: unauthenticated GitHub API allows ~60 requests/hour per IP.
- **`ANDROID_HOME` / SDK not found**: install the SDK and set `ANDROID_HOME`, or create `native/android/local.properties` with `sdk.dir=...`.

## Verification status

Not run: Maven build, TeaVM compile, unit tests, `npm install`, `cap sync`, Gradle build, device/emulator testing. Remaining to do on a machine with the Android SDK: run the commands above, commit `native/package-lock.json` and the generated `native/android` project, and confirm the Device plugin call and a live GitHub request on the emulator.

Known unverified points: TeaVM 0.16 `jsModuleType=NONE` output and the global `main`; whether `Capacitor.Plugins.Device` is populated natively without importing the plugin's JS module (a `registerPlugin` fallback is included); the code has never been compiled, so lambda typing against TeaVM's `JSPromise`/`JSMapping` signatures may need small fixes.
