# ProjectLibre on JDK 26

ProjectLibre 1.9.8 building and running on OpenJDK 26, with readable text on
Linux HiDPI screens. Upstream targets JDK 21 and forces settings that look
blurry on modern displays. This branch fixes both.

## Requirements

- OpenJDK 26 (`java-26-openjdk` on Arch/CachyOS, or any 26 build)
- Apache Ant 1.10.x

## Build and run

```bash
cd projectlibre_build
ant dist        # dist/projectlibre.jar + dist/lib/
ant test        # offline regression checks
ant dir         # runnable bundle under packages/projectlibre-1.9.8/
./packages/projectlibre-1.9.8/projectlibre.sh
```

`ant all` builds the portable bundle only. Run `ant deb`, `ant rpm`, or
`ant jpackage-dmg` explicitly on the matching packaging platform.

Do not run the script in `resources/` directly. It is a template that the
`dir` target copies next to the jars.

## What is different from upstream

- Toolchain moved from 21 to 26. See CHANGELOG for the full list.
- Two JDK 26 startup crashes fixed (`SecurityManager`, `java.applet`).
- Linux uses the system look and feel with subpixel font smoothing instead
  of forced Metal, and the timeline header paints with LCD hints.
- Launchers and packages require Java 26. Cached Java paths are checked again
  after an upgrade.
- Project saves use atomic replacement, and export paths keep the directory
  selected by the user.
- ZIP extraction, XML readers, update checks, and project deserialization have
  explicit safety limits and trust boundaries.
- `ant test` runs offline regression checks without contacting a server.

## Upstream credit

All application code is the work of the ProjectLibre team. This repo tracks
their SourceForge git at `https://git.code.sf.net/p/projectlibre/code`.
`master` here mirrors upstream untouched. The JDK 26 work lives on the
`jdk26` branch, based on upstream `master@0530be2` (v1.9.8).

## License

Same as upstream: CPAL 1.0, see `projectlibre_build/license/`.
