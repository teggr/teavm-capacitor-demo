# Java Repository Explorer (TeaVM + Capacitor)

A small proof of concept: **Java** owns the application behaviour, **TeaVM** compiles it to JavaScript, and **Capacitor** packages it as an **Android** app. The app lists a GitHub user's repositories (public REST API, no auth) and shows device info obtained from the official Capacitor **Device** plugin.

> **Status: verified in a browser and an Android emulator.** Java/TeaVM compilation, unit tests, Android packaging, the native Device plugin and live GitHub requests have passed. See [Verification status](#verification-status).

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
- `native/` – Capacitor packaging (`package.json`, `package-lock.json`, `capacitor.config.json`) and the generated Android project. Build outputs and copied web assets are ignored.

`Capacitor.invoke` uses `window.Capacitor.Plugins[name]` (falling back to `registerPlugin`), which the native Android bridge injects, so no JS bundler is needed. Outside a native app it reports that the bridge is absent.

## Prerequisites

- JDK 21 for Android (Maven targets release 17). The generated Gradle 8.14.3 wrapper cannot run on JDK 25; use JDK 21, such as Android Studio's bundled JDK.
- Node.js 22+ and npm
- Android Studio or the Android SDK (`ANDROID_HOME` set), Android SDK Platform 36 and Build-Tools 35.0.0, plus an emulator or device with USB debugging. Gradle can download missing SDK components when their licenses have already been accepted.
- Internet access (Maven Central, npm, GitHub API)

Pinned: TeaVM 0.16.0, Capacitor 8.6.0, `@capacitor/device` 8.0.3, Maven 3.9.11 (via wrapper).

## Build and run

Use the Maven wrapper (`mvnw.cmd` on Windows).

```sh
# 1. Java -> JavaScript (also copies index.html/style.css into target/web)
./mvnw clean package

# 2. Capacitor / Android
cd native
npm ci                       # restore the locked dependencies
npx cap sync android         # copies ../target/web and plugins into the Android project
npx cap run android          # build, install and launch on a device/emulator
# or: npx cap open android   # open in Android Studio
```

Repeat step 1 and `npx cap sync android` after every Java change.

The Android project is already included; do not run `npx cap add android` again.

On Windows, set the Android build's JDK and SDK in the same PowerShell session before running the commands:

```powershell
# Adjust these paths for your installation; the selected JDK must be version 21.
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\mvnw.cmd clean package
Set-Location native
npm ci
npx cap sync android
npx cap run android
```

To try the UI in a browser first (no native bridge, so the runtime section says so):

```sh
jwebserver -b 127.0.0.1 -p 8080 -d target/web   # then open http://localhost:8080
```

In PowerShell, use `-d .\target\web`. `jwebserver` is included with JDK 21.

## Troubleshooting

- **Blank page / `main is not defined`**: check that `target/web/js/app.js` exists and rebuild/sync the web assets. TeaVM 0.16.0 with `jsModuleType=NONE` was verified to expose the global `main` used by `index.html`.
- **Gradle `Unsupported class file major version 69`**: Gradle was launched with JDK 25. Set `JAVA_HOME` to JDK 21 before running Capacitor or Gradle.
- **`cap run` reports ADB unresponsive / device still authorizing**: on the tested emulator, the deployment helper's five-second ADB timeout expired even though the app launched. Wait for `adb devices` to show `device`, then build/install/launch directly. From `native` in PowerShell, with `JAVA_HOME` and `ANDROID_HOME` set:

  ```powershell
  npx cap sync android
  Set-Location android
  .\gradlew.bat --no-daemon assembleDebug
  adb -s emulator-5554 install -r .\app\build\outputs\apk\debug\app-debug.apk
  adb -s emulator-5554 shell am start -n dev.teggr.javarepoexplorer/.MainActivity
  ```

  Replace `emulator-5554` with the serial from `adb devices`.
- **Runtime info says the plugin is unavailable**: run `npx cap sync android` after `npm install` so the Device plugin is registered natively.
- **Debugging**: open `chrome://inspect` with the device attached to inspect the WebView console; use `adb logcat | grep -i capacitor` for native logs. Source maps are generated for the Java sources.
- **Rate limit message**: unauthenticated GitHub API allows ~60 requests/hour per IP.
- **`ANDROID_HOME` / SDK not found**: install the SDK and set `ANDROID_HOME`, or create `native/android/local.properties` with `sdk.dir=...`.

## Verification status

Verified on Windows on 2026-11-15:

- Maven/TeaVM compilation and all six JUnit tests passed. The existing `JSPromise` lambdas compiled without changes.
- The generated classic script exposed global `main`; the browser rendered ten repositories from a live GitHub request and correctly reported the absent native bridge.
- Browser smoke checks passed for blank input, loading, empty results, HTTP 404, rate limiting, malformed JSON/data, network/body-read failures, retry and safe text rendering. Error/empty payloads were simulated; the initial request and retry used the live GitHub API.
- npm dependencies were installed and locked; the Android project was generated and synchronized with `@capacitor/device`.
- The debug APK built with JDK 21, Android SDK Platform 36 and Build-Tools 35.0.0, then ran on the `Medium_Phone_API_35` emulator.
- `cap run android --target emulator-5554` built the APK but reported an ADB deployment timeout; direct ADB installation/launch was verified. See the fallback above.
- The emulator rendered ten live GitHub repositories. The native `Device.getInfo` call returned platform `android`, OS `android 15` and model `sdk_gphone64_x86_64`, all displayed in the UI without importing the plugin's JavaScript module.

Physical-device testing and a signed release build have not been performed.
