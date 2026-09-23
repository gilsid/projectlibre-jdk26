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
