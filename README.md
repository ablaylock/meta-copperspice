# meta-copperspice

Yocto layer for [CopperSpice](https://www.copperspice.com/) 2.1.0.

Targets Yocto **wrynose (6.0 LTS)** only
(`LAYERSERIES_COMPAT_copperspice = "wrynose"`). For the old kirkstone-era
layer see the `kirkstone` branch.

Verified machines: `qemuarm` (32-bit), `qemuarm64`, `qemux86-64` — build,
host/target tool split, and GUI boot in QEMU checked on all three.

## What you get

- `copperspice` — the libraries, cross-compiled correctly: build-time
  `.ui`/`.qrc`/`.ts` processing runs **host** tools from
  `copperspice-native`, while target images get only target libraries.
  Target-arch tools are packaged separately as `copperspice-tools` (for
  on-device development) and are never installed by default.
- `copperspice.bbclass` — inherit it in your application recipe and
  `find_package(CopperSpice)` + `COPPERSPICE_RESOURCES()` just work when
  cross compiling.
- `cs-hello` — minimal GUI demo and reference consumer (see
  `recipes-copperspice/cs-hello/files/CMakeLists.txt`).
- `kitchensink` — the official CopperSpice demo app.
- `copperspice-demo-image` — sato-based image with both demos.
- `copperspice-demo-image-weston` — `core-image-weston` plus both demos,
  used to verify the Wayland platform plugin on QEMU.
- kas configs for reproducible builds on the three QEMU machines, plus
  `kas/wayland-only.yml` (drops `x11` from `DISTRO_FEATURES` and adds
  `glvnd`, for a pure-Wayland world) and `kas/gates.yml` (passwordless
  root login, for QEMU GUI verification).

## Build configuration

`copperspice` exposes upstream's `WITH_*` options as `PACKAGECONFIG` knobs.
The default set reproduces what this layer has always built:

```
PACKAGECONFIG ??= "gui network x11 multimedia opengl svg sql xmlpatterns openssl cups glib"
```

(`x11` and `wayland` are pulled in only when the matching `DISTRO_FEATURE`
is set. poky carries both by default, so the default build is a **coexist**
build: both platform plugins land in one `copperspice` build, with xcb
remaining CopperSpice's runtime default. See "Runtime platform selection"
below.)

| Knob | Controls | Off sheds |
|---|---|---|
| `gui` | CsGui, platform plugins | the whole GUI stack; needs `x11` and/or `wayland` |
| `network` | CsNetwork | sockets, SSL-backed network access |
| `multimedia` | CsMultimedia (GStreamer backend) | audio/video playback |
| `opengl` | the CsOpenGL add-on library | just that add-on (see below) |
| `svg` | SVG image format plugin | `.svg` loading |
| `sql` | CsSql core + the bundled SQLite driver | SQL access entirely |
| `xmlpatterns` | CsXmlPatterns (XQuery/XPath) | that module only |
| `vulkan` | CsVulkan | needs the `vulkan` distro feature |
| `webkit` | CsWebKit | see verification note below |
| `psql`, `mysql`, `odbc` | extra CsSql driver plugins | those drivers |
| `openssl` | TLS support | see runtime note below |
| `cups` | printing support | CUPS printer driver |
| `glib` | glib event-loop integration | glib mainloop hookup |
| `pulseaudio` | CsMultimedia audio output backend (libpulse) | no audio playback (CsMultimedia still does everything else) |
| `x11` | XCB/X11 platform plugin | one of the two platform plugins |
| `wayland` | Wayland platform plugin (`CsGuiWayland`, EGL client-buffer integration, bradient decoration); deps `wayland wayland-native libxkbcommon virtual/egl`; needs the `wayland` distro feature | the other platform plugin; on by default when the distro has `wayland` (poky does) |

`gui`, `network`, `opengl`, `svg`, `sql`, `xmlpatterns`, `vulkan`, and
`webkit` map straight to upstream `WITH_*` switches. `psql`, `mysql`, and
`odbc` are extra CsSql driver plugins layered on top of `sql`. `openssl`,
`cups`, `pulseaudio`, `glib`, and `x11` have no upstream switch at all —
upstream only offers `find_package()` autodetection for them, so the OFF
side of each knob explicitly forbids that `find_package()` call; otherwise
whatever happened to be staged in the sysroot (or not) would silently
decide the feature, breaking reproducibility.

Selecting a knob without its prerequisites fails at parse time, naming
what's missing, e.g.:

```
copperspice: PACKAGECONFIG 'multimedia' also requires: glib
```

The full rule set: `multimedia` needs `gui network opengl glib` (GStreamer
is glib-based); `opengl` and `svg` need `gui`; `xmlpatterns` needs
`network`; `webkit` needs `gui network`; `psql`/`mysql`/`odbc` need `sql`;
and `gui` needs a platform plugin — `x11`, `wayland`, or both.

**OpenGL:** the `opengl` knob only controls the optional CsOpenGL add-on
library. CsGui itself always compiles its `QOpenGL*` classes and links
against desktop GL, so any `gui` build needs the `opengl` `DISTRO_FEATURE`
regardless of whether the `opengl` knob is enabled. Which GL library that
is depends on the platform: on distros with the `x11` distro feature it's
classic `virtual/libgl` (GLX-based); on x11-less distros it's `libglvnd`
instead, and the recipe additionally requires the `glvnd` distro feature
for `gui` builds there (`REQUIRED_DISTRO_FEATURES`, so `features_check`
skips the recipe cleanly if it's missing rather than failing deep in the
build) — legacy Mesa's classic `libGL` doesn't exist in an x11-less
world, so desktop GL has to come from GLVND's `libOpenGL` over EGL.

**Audio:** PulseAudio (via the `pulseaudio` knob) is CopperSpice's only
audio output backend at the CS level; ALSA support is dead code upstream
and this layer does not build against it.

`psql`, `mysql`, and `odbc` need their client libraries from
meta-openembedded's `meta-oe` layer, which this layer does not otherwise
require — add `meta-oe` to `bblayers.conf` before enabling them (see
`kas/audit.yml` for a pinned example). `vulkan` needs the `vulkan`
`DISTRO_FEATURE` (for `vulkan-loader`). `openssl` is a runtime dlopen
dependency — CsNetwork loads `libssl`/`libcrypto` at runtime rather than
linking them, so the knob only adds RDEPENDS.

Every knob combination in `scripts/knob-audit.sh` is build-verified on
`qemuarm64`, except `webkit`: `WITH_WEBKIT=YES` fails to compile against
oe-core's default `-Werror=format-security` hardening (upstream CS WebKit
overrides its own warning flags), so `webkit` is provided as upstream
offers it, not verified by this layer.

To override the defaults, set `PACKAGECONFIG:pn-copperspice` in
`local.conf`, or a kas `local_conf_header` fragment. Known-good minimal
configs: `headless` (`PACKAGECONFIG = ""`, CsCore+CsXml only) and
`min-gui` (`PACKAGECONFIG = "gui network x11"`). `scripts/knob-audit.sh`
only accepts its 19 predefined config names (see the `ALL` list at the
top of the script, or pass `all` to run every one) — including
`with-wayland` (coexist: both platform plugins built, xcb still the
runtime default) and `wayland-only` (`x11` off, `wayland` on: no xcb/X11
libraries, `CS_GUI_PLATFORM_NAME=wayland` exported); to validate a custom
selection, add a case for it to the script's `configure()` function and
run that.

## Runtime platform selection

- **Coexist builds** (both `x11` and `wayland` knobs on, poky's default):
  xcb is CopperSpice's runtime default. Opt an individual app into Wayland
  with `-platform wayland` on its command line, or by setting
  `CS_GUI_PLATFORM_NAME=wayland` in its environment.
- **Wayland-only builds** (`x11` knob off, `wayland` on — no xcb plugin
  exists in the image at all): the recipe appends
  `export CS_GUI_PLATFORM_NAME=wayland` to `/etc/profile.d/copperspice.sh`
  automatically, so login sessions get working GUI apps with no manual
  configuration. Without this override every app would abort looking for
  xcb, upstream's hardcoded default platform.

## Quick start (kas)

Work from a workspace directory ABOVE this repo — builds, downloads, and
sstate live there, never inside the layer:

```
mkdir cs-yocto && cd cs-yocto
git clone <this-repo> meta-copperspice
pipx install kas

kas build meta-copperspice/kas/qemuarm64.yml
kas shell meta-copperspice/kas/qemuarm64.yml -c \
    "runqemu qemuarm64 copperspice-demo-image"
```

The kas configs make the image emit a plain `ext4` (in addition to
wrynose's default `ext4.zst`) and set `QB_DEFAULT_FSTYPE = "ext4"`, so
`runqemu` boots it directly — no snapshot mode needed.

In the Sato launcher run **CS Hello** (should show "Hello from CopperSpice
2.1.0 on Yocto wrynose") and **KitchenSink**.

Without a local display, append `publicvnc` to the runqemu command and
connect a VNC viewer to port 5900. Do NOT use the `gl` runqemu option over
X11 forwarding or VNC — it requires a local GL-capable display.

All three machines share `downloads/` and `sstate-cache/`, and one `build/`
directory serves all of them (set `KAS_BUILD_DIR` for separate trees).

Verify the host/target tool split for a machine (inside kas shell, from
the build directory):

```
kas shell meta-copperspice/kas/qemuarm64.yml -c \
    "../meta-copperspice/scripts/verify-tool-split.sh qemuarm64"
```

## Using the layer without kas

Add the layer path to `BBLAYERS` in an existing wrynose build
(openembedded-core + bitbake 2.18 + meta-poky) and add
`copperspice-demo-image` or the individual recipes to your image. The
recipes have no dependency on kas. The distro must provide the `opengl`
`DISTRO_FEATURE` plus at least one of `x11` or `wayland` (poky's defaults
provide `opengl` and both platform features, giving a coexist build); on
an x11-less distro (`wayland` only) the `glvnd` distro feature is also
required. Without a satisfied platform/GL combination the `copperspice`
recipe is skipped (`REQUIRED_DISTRO_FEATURES` + `features_check`) and the
demo apps fail to resolve their dependency on it.

## The host/target tool split

CopperSpice generates code at build time with its own tools (`uic`, `rcc`,
`lrelease`, `lconvert`, `lupdate`), and, for Wayland builds,
`cs_wayland_scanner` (protocol marshalling code generator) — six tools in
total. In a cross build those tools are target binaries and cannot run on
the build machine, so this layer builds them twice:

- `copperspice-native` builds a tools-only configuration (CsCore + CsXml,
  everything else off, `-DWITH_WAYLAND_SCANNER=YES`) and stages all six
  tools for the build host.
- The target `copperspice` build points its own tool invocations at the
  native `uic`/`rcc`/`lrelease` (and, when the `wayland` knob is on,
  `cs_wayland_scanner` — the only tools CopperSpice runs while building
  itself), and consumer recipes point all five non-scanner tools, via the
  `CS_TOOL_UIC`, `CS_TOOL_RCC`, `CS_TOOL_LRELEASE`, `CS_TOOL_LCONVERT`,
  `CS_TOOL_LUPDATE` CMake cache variables (a `CS_TOOL_CS_WAYLAND_SCANNER`
  variable also exists for projects that process their own Wayland
  protocol XML). These variables are added by carried patches 0001
  (CopperSpice's own build) and 0002 (the exported
  `CopperSpiceConfig.cmake`, so any `find_package(CopperSpice)` project
  gets them); they are no-ops when unset, leaving native builds and
  upstream behavior unchanged.

Application recipes only need:

```
inherit copperspice
```

The class pulls in `copperspice` + `copperspice-native` and passes all five
`CS_TOOL_*` variables. `cs-hello` is the reference consumer.

## Packaging notes

- `copperspice` — runtime libraries and plugins. CopperSpice ships
  unversioned `.so` files on purpose (ABI is in the name,
  e.g. `libCsCore2.1.so`), so the recipe treats `.so` as runtime, not dev.
- `copperspice-dev` — headers, CMake package, pkgconfig.
- `copperspice-tools` — target-arch `uic`/`rcc`/`lrelease`/`lconvert`/
  `lupdate`/`linguist` (plus `cs_wayland_scanner` on wayland builds),
  only useful for development ON the device. Never installed in images;
  the verify script asserts this.
- **Plugin layout:** upstream installs plugins flat in `${libdir}`, but at
  runtime QFactoryLoader only searches `<librarypath>/<category>/`, so a
  flat layout means no GUI app can start ("platform plugin was not found",
  key `xcb`). The recipe instead installs plugins under
  `${libdir}/copperspice/plugins/<category>/` (platforms, imageformats,
  xcbglintegrations, ...) and exports `CS_PLUGIN_PATH` via
  `/etc/profile.d/copperspice.sh` (the X session sources `/etc/profile`).
  All three Wayland sub-plugins (`CsGuiWayland2.1.so`, the EGL
  client-buffer integration `CsGuiWayland_Egl2.1.so`, and the bradient
  decoration `CsGuiWayland_bradient2.1.so`) land in the `platforms`
  category alongside `CsGuiXcb2.1.so` — every Wayland `QFactoryLoader`
  looks under `/platforms`, unlike XCB's separate `xcbglintegrations`.
  `libCsWaylandClient2.1.so` is a regular (non-plugin) library and stays
  directly in `${libdir}`.
- 32-bit ARM builds use `DEBUG_LEVELFLAG:arm = "-g1"`: with full `-g` the
  unstripped `libCsGui2.1.so` exceeds the 4 GiB ELF32 file-offset limit
  and the linker emits a structurally broken library.
- CopperSpice wants roughly 4 GB RAM per compile thread; the recipe pins
  `PARALLEL_MAKE = "-j 10"`. On smaller hosts lower it further.
- Upstream publishes no tarball checksums; the recipe pins a sha256
  computed from the tarball at recipe-creation time (2026-07-08).

## Carried patches

Eleven patches in `recipes-copperspice/copperspice/files/`, all
`Upstream-Status: Pending`:

- `0001-cmake-support-prebuilt-host-tools-in-the-CopperSpice.patch` —
  honor `CS_TOOL_*` in CopperSpice's own resource build steps so cross
  builds run host tools.
- `0002-cmake-support-prebuilt-host-tools-in-the-exported-co.patch` —
  honor the same `CS_TOOL_*` variables in the exported
  `CopperSpiceConfig.cmake`: when they are set, the generated
  `CopperSpiceBinaryTargets.cmake` (whose existence checks fail in a cross
  sysroot) is skipped and `CopperSpice::uic` etc. IMPORTED targets are
  created directly from the given host tools.
- `0003-cmake-only-request-the-OpenGL-EGL-component-when-Way.patch` —
  the unconditional EGL component request breaks FindOpenGL on legacy
  (non-GLVND) Mesa, leaving CsGui unlinkable; EGL is only needed by the
  Wayland plugin.
- `0004-cmake-build-the-NEON-drawhelpers-on-64-bit-ARM.patch` — the NEON
  drawhelper sources were only compiled when `CMAKE_SYSTEM_PROCESSOR`
  matches "arm", which "aarch64" does not, causing undefined references
  (`qt_memfill32`) when linking CsGui.
- `0005-cmake-do-not-export-sysroot-paths-for-private-depend.patch` —
  zlib/OpenGL/GStreamer are implementation details but were linked PUBLIC,
  baking absolute cross-sysroot paths into the exported CMake package.
- `0006-core-detect-32-bit-ARM-via-the-canonical-__ARM_ARCH-.patch` —
  qglobal.h only recognized `__ARM_ARCH_7__` exactly; ARMv7-A compilers
  define `__ARM_ARCH_7A__`, so 32-bit ARM builds died on
  `#error "Unsupported system architecture"`. Check the numeric
  `__ARM_ARCH` first.
- `0007-cmake-allow-building-cs_wayland_scanner-standalone.patch` — adds
  `WITH_WAYLAND_SCANNER` (default OFF) so `cs_wayland_scanner` can be
  built on its own, letting `copperspice-native` produce a host scanner
  without enabling GUI or Wayland support.
- `0008-cmake-demote-the-X11-stack-from-REQUIRED-to-RECOMMEN.patch` —
  upstream hard-requires the X11/XCB stack even when only the Wayland
  plugin is wanted; demoting it to RECOMMENDED lets `configure` proceed
  on x11-less distros.
- `0009-cmake-locate-EGL-without-FindOpenGL-for-the-Wayland-.patch` —
  the unconditional `OpenGL::EGL` component request (via `FindOpenGL`)
  is GLVND-poisoned on legacy Mesa; locate EGL directly with
  `find_library(NAMES GL OpenGL)` instead, which also covers GLVND
  sysroots that have no classic `libGL`.
- `0010-gui-build-QWindowsStyle-for-Wayland-platforms.patch` — CsGui's
  style factory only compiled `QWindowsStyle` for X11; it's also the
  base class of `QStyleSheetStyle`, so Wayland builds need it too.
- `0011-cmake-link-the-GLVND-OpenGL-dispatch-library-when-li.patch` —
  append GLVND's `libOpenGL` to `OPENGL_LIBRARIES` when classic `libGL`
  is absent, so linking against desktop GL works on GLVND-only sysroots.

**Note for maintainers:** the CopperSpice sources use CRLF line endings.
Any patch touching them must reproduce CRLF byte-exact or `do_patch`
fails with "different line endings". Never edit the patch files with
tools that normalize line endings; regenerate with git and verify with
`file` (should report "with CRLF") before committing.

`kitchensink` carries one additional patch (link OpenGL explicitly on
Linux/BSD), tagged for its own upstream.

## SDK

`bitbake copperspice-demo-image -c populate_sdk` produces an SDK whose
host sysroot contains the CopperSpice libraries AND all six host tools,
including `cs_wayland_scanner` (`TOOLCHAIN_HOST_TASK` adds both
`nativesdk-copperspice` and `nativesdk-copperspice-tools` — the tools
package split applies to nativesdk too, so listing only the former would
ship libraries with no code generators).

SDK consumers building CopperSpice applications with CMake must pass the
tool overrides. Verified invocation (qemuarm64 SDK):

```
. <sdk>/environment-setup-cortexa57-poky-linux
N=$OECORE_NATIVE_SYSROOT/usr/bin
cmake -G Ninja <src> \
    -DCS_TOOL_UIC=$N/uic \
    -DCS_TOOL_RCC=$N/rcc \
    -DCS_TOOL_LRELEASE=$N/lrelease \
    -DCS_TOOL_LCONVERT=$N/lconvert \
    -DCS_TOOL_LUPDATE=$N/lupdate \
    -DCS_TOOL_CS_WAYLAND_SCANNER=$N/cs_wayland_scanner
```

(`CS_TOOL_CS_WAYLAND_SCANNER` only matters to a consumer that processes
its own Wayland protocol XML with CopperSpice's scanner; KitchenSink and
CS Hello don't, but the variable is harmless to pass unconditionally.
Like the other five tools, `cs_wayland_scanner` also gets a target-arch
build: upstream builds the scanner as part of the Wayland platform
plugin, so any target build with the `wayland` knob installs it to
`${bindir}`, where it is packaged into `copperspice-tools` — never in
images, like the rest of the target tools. The target build never runs
that copy; its codegen uses the copperspice-native scanner via
`CS_TOOL_CS_WAYLAND_SCANNER`.)

## Troubleshooting

- **bitbake lock / stale server:** only one bitbake server may run per
  build directory. If a command hangs on the lock, another kas/bitbake
  session (including a running `runqemu` started via `kas shell`) still
  holds it — finish or stop that session first. A crashed session can
  leave a stale server; it times out on its own, or remove
  `build/bitbake.lock` once you are sure nothing is running.
- **runqemu shows no window:** ensure `DISPLAY` is set (WSL2 needs WSLg or
  an X server). Over ssh use `ssh -Y` with an X server on your side
  (XQuartz on macOS), or use `publicvnc` and tunnel it:
  `ssh -L 5901:localhost:5900`, then point a VNC viewer at
  `localhost:5901`. macOS Screen Sharing rejects no-auth VNC servers —
  use TigerVNC or similar. Do not pass `gl` in either setup.
- **"platform plugin was not found" (key "xcb" or "wayland"):** the app
  cannot see the plugin directory. Check `/etc/profile.d/copperspice.sh`
  is present and the session sourced it (`echo $CS_PLUGIN_PATH`). On a
  wayland-only build the key is always "xcb" unless
  `CS_GUI_PLATFORM_NAME=wayland` is set (`echo $CS_GUI_PLATFORM_NAME`) —
  the recipe sets it automatically in `/etc/profile.d/copperspice.sh`,
  so an unset value there usually means the session didn't source it.

## Updating the pins

The kas configs pin exact upstream commits for reproducibility. To move
to newer wrynose point releases, update the `commit:` values in
`kas/base.yml` deliberately (`git ls-remote <url> <branch>`), rebuild,
and commit the bump on its own.
