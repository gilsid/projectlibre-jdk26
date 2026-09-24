# Changelog

## jdk26 — 2026-09-23

Base: upstream `master@0530be2` (v1.9.8).

### Toolchain 21 to 26

- `javac` in `projectlibre_build`, `projectlibre_contrib`, `projectlibre_core`,
  `projectlibre_exchange`: `source/target="21"` changed to `release="26"`.
- Eclipse compiler prefs in all six modules and IntelliJ `misc.xml` set to 26.
- `build.properties`: `jvm_name=openjdk-26.jdk`, mac bundle path points at
  `openjdk-26.jdk`, Windows bundle path at `jdk-26`.
- `resources/win/make.ps1` expects `jdk-26`.
- Debian `control` and `control-build` depend on `default-jre|openjdk-26-jre|…`
  and `default-jdk|openjdk-26-jdk|…` instead of the Java 6/7-only packages.

### JDK 26 runtime fixes

- `StartupFactory` no longer calls `System.setSecurityManager(null)` bare.
  That call throws on JDK 24 and later, so it sits in try/catch now.
- `BasicPopupPanelUI` dropped `java.applet.Applet`, which the JDK removed.
  The popup-ancestor check tests `Window` only. Applets have been dead for
  years, so nothing of value was lost.

### Readable text on Linux HiDPI

- `com.projectlibre1.main.Main` and `pm.graphic.gantt.Main` set
  `awt.useSystemAAFontSettings=on` and `swing.aatext=true` before Swing
  starts. The shell and batch launchers pass the same flags.
- `LafManagerImpl` tries the system look and feel first on Linux, then
  Nimbus, then Metal. Forcing Metal was the main source of blocky text.
- `TimeScaleComponent` paints the timeline header with the desktop font
  hints, LCD subpixel smoothing, and fractional metrics.

## Audit follow-up — 2026-09-24

- Build `javac` tasks now pin UTF-8, fail on compiler errors, and keep Ant's
  runtime out of the application classpath. `ant test` runs offline regression
  checks.
- Linux launchers and package metadata now require Java 26, revalidate cached
  runtimes, preserve the selected export directory, and exit non-zero when no
  compatible runtime is available.
- Project saves use checked, atomic file replacement. PDF/PNG export handles
  cancellation, extension selection, stream closure, and multi-page output.
- Update checks use HTTPS with timeouts and a bounded response. Remote Groovy
  formulas are no longer compiled or executed.
- ZIP and embedded archive extraction now enforce path containment and size
  limits. XML readers disable DTDs and external entities. Java project data
  deserialization uses an allowlist filter.
- Fixed the Windows platform check, first-run preference key, and spreadsheet
  paste column offset.
- Fixed multi-project save filename reuse, EDT confirmation deadlocks, import
  recovery result propagation, case-sensitive file routing, leveling-delay
  export, and strict date/money conversion.
- Project and export files are now replaced atomically. PDF/PNG export keeps an
  existing target intact when rendering fails.
- Project ID and resource-pool registries no longer retain closed project
  graphs through strong references.
- Packaging now rejects the incompatible ProGuard path clearly, includes report
  runtime dependencies, and keeps legacy platform targets out of `all`.
- Replaced finalizer-based cleanup with explicit lifecycle methods and corrected
  reflective calls that produced JDK compiler warnings.
