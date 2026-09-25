# Changelog

## Audit fixes — 2026-09-25

Independent review of the `jdk26` hardening. All items below were verified
against the code and covered by `ant test` where noted.

### Crashes on close, login, and locale (fixed)

- `GraphModel.setCache(null)` dereferenced the null cache, so every document
  close threw on the EDT and aborted the rest of `DocumentFrame.cleanUp`.
  `setCache` now detaches from the old cache and only attaches non-null
  caches; `cleanUp` isolates per-view failures and always closes the shared
  project caches.
- `ReferenceNodeModelCache.close()` was not idempotent and kept receiving
  events after close. It now latches a `closed` flag, clears the model and
  document, and stops event delivery.
- `StartupFactory` built the JNLP login URL with `URI.create`, whose
  `IllegalArgumentException` escaped the `MalformedURLException` catch and
  crashed login on a bad server URL. Same unchecked-escape fix in
  `LicenseDialog`.
- `LocaleDialog` crashed on a corrupt locale preference because `Locale.of`
  throws for ill-formed codes. It now falls back to English and the default
  country.
- `TimeScaleComponent` applied screen-only LCD hints to print/export graphics
  and called `Toolkit` on headless JVMs. Hints now apply to screen painting
  only, with a headless guard and a null clip fallback.

### Deserialization and archive limits (tightened)

- `SerializationFilter` now caps array lengths (undeclared multi-gigabyte arrays
  no longer reach allocation), drops `java.net.URL` (DNS exfil on
  deserialize) while allowing `java.awt.print` and `javax.print.attribute`
  value types used by saved print settings. Dynamic proxies stay undecided
  rather than rejected: array type descriptors (e.g. the `byte[]` magnitude
  inside `BigDecimal`, which `Money` extends) also arrive with a null class,
  and no `InvocationHandler` exists in the allowed packages, so rejecting
  null would break legitimate cost data for no practical gain.
- `PrintSettingsManager` applies the same allowlist filter as the other
  deserialization paths.
- `SerializeUtil` bounds decompressed ZIP payloads with a new
  `LimitedInputStream` and rejects archives without entries.
- P3/PRX and SureTrak/STX extraction now cap entry count and total output,
  matching the ZIP limits.
- ZIP extraction rejects duplicate entry names; the compression-ratio check
  uses exact long arithmetic; `FileHelper.createTempDir` no longer races via
  create-then-mkdir, and archive entries resolving to the destination itself
  are rejected.
- `UniversalProjectReader` directory sniffing reads the full peek buffer and
  no longer leaks a file descriptor; small files are matched instead of
  skipped.
- XML readers additionally enable secure processing and disable entity
  reference expansion.

### Update check (redirect downgrade closed)

- The update check no longer follows redirects automatically. Up to three
  `https`-only hops are followed manually, non-200 responses are ignored, and
  the version string must match a numeric dotted pattern before it is shown
  or persisted.

### Save, schedule, and identity edge cases (fixed)

- `MSPDISerializer` checks the temp-file size after the stream is closed
  rather than while it is still open.
- `CalendarDefinition.compare` guards empty calendars like `add` does.
- `HasUniqueIdImpl.renumber` no longer plants a dead weak reference when the
  referent was collected, and now implements `hashCode` to match `equals`.
- `ResourcePool` gains `disconnectOutlines`, called from
  `ResourcePoolFactory.removePool`, so pooled assignment models stop
  receiving events after the pool is dropped.
- `LafManagerImpl.getUnselectedBackgroundColor` falls back instead of
  returning null on look-and-feels without the key.
- `Main.getProjectLibreRunNumber` falls back to the legacy `runNumber` key.
- `FieldConverter.parseFully` reports a valid error offset when the parser
  leaves none.
- `LoginDialog` credential persistence uses try-with-resources and
  `valueOf` instead of deprecated constructors.
- `SpreadSheet.setModel` detaches the listener from the old model, and
  `DefaultFrameManager` clears combo items instead of child components.
- `GraphicManager` fixes the `BootstrapApplet.class` reflection name.

### Packaging and launchers (fixed)

- `resources/deb/rules` is a real makefile again (it contained literal `\n`
  sequences on one line).
- `projectlibre.nsi` requires JRE 26 with a live download URL and message.
- The WiX `>= "26"` string comparison (which accepted Java 9) is now a
  guarded numeric-shape check.
- Debian runtime dependency is `default-jre | openjdk-26-jre |
  openjdk-25-jre | openjdk-21-jre` so the package stays installable; the
  launcher still enforces 26 at runtime with a clear error.
- `projectlibre.bat` strips quoted `JAVA_HOME` and refuses runtimes below
  major 26, including `1.x` version strings.
- `make.ps1` parses the major version numerically and no longer mutates
  `$env:JAVA_HOME`.
- Shell launchers quote `dirname "$0"` and the JVM search globs so paths
  with spaces work.
- `ant tar` only passes GNU `--owner/--group` flags off macOS.
- `Text.badJavaVersion` names Java 26.

### Tests

- `ant test` now also covers: URL rejection, URI/print-type round-trip,
  oversized-array rejection, input-stream caps, temp-dir lifecycle, absolute
  and duplicate ZIP entries.

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
