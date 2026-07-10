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
- kas configs for reproducible builds on the three QEMU machines.

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
recipes have no dependency on kas. The distro must provide the `x11` and
`opengl` `DISTRO_FEATURES` (poky's defaults do); without them the
`copperspice` recipe is skipped and the demo apps fail to resolve their
dependency on it.

## The host/target tool split

CopperSpice generates code at build time with its own tools (`uic`, `rcc`,
`lrelease`, `lconvert`, `lupdate`). In a cross build those tools are target
binaries and cannot run on the build machine, so this layer builds them
twice:

- `copperspice-native` builds a tools-only configuration (CsCore + CsXml,
  everything else off) and stages the five tools for the build host.
- The target `copperspice` build points its own tool invocations at the
  native `uic`/`rcc`/`lrelease` (the only tools CopperSpice runs while
  building itself), and consumer recipes point all five, via the
  `CS_TOOL_UIC`, `CS_TOOL_RCC`, `CS_TOOL_LRELEASE`, `CS_TOOL_LCONVERT`,
  `CS_TOOL_LUPDATE` CMake cache variables. These variables are added by carried patches 0001 (CopperSpice's
  own build) and 0002 (the exported `CopperSpiceConfig.cmake`, so any
  `find_package(CopperSpice)` project gets them); they are no-ops when
  unset, leaving native builds and upstream behavior unchanged.

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
  `lupdate`/`linguist`, only useful for development ON the device. Never
  installed in images; the verify script asserts this.
- **Plugin layout:** upstream installs plugins flat in `${libdir}`, but at
  runtime QFactoryLoader only searches `<librarypath>/<category>/`, so a
  flat layout means no GUI app can start ("platform plugin was not found",
  key `xcb`). The recipe instead installs plugins under
  `${libdir}/copperspice/plugins/<category>/` (platforms, imageformats,
  xcbglintegrations, ...) and exports `CS_PLUGIN_PATH` via
  `/etc/profile.d/copperspice.sh` (the X session sources `/etc/profile`).
- 32-bit ARM builds use `DEBUG_LEVELFLAG:arm = "-g1"`: with full `-g` the
  unstripped `libCsGui2.1.so` exceeds the 4 GiB ELF32 file-offset limit
  and the linker emits a structurally broken library.
- CopperSpice wants roughly 4 GB RAM per compile thread; the recipe pins
  `PARALLEL_MAKE = "-j 6"`. On smaller hosts lower it further.
- Upstream publishes no tarball checksums; the recipe pins a sha256
  computed from the tarball at recipe-creation time (2026-07-08).

## Carried patches

Six patches in `recipes-copperspice/copperspice/files/`, all
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

**Note for maintainers:** the CopperSpice sources use CRLF line endings.
Any patch touching them must reproduce CRLF byte-exact or `do_patch`
fails with "different line endings". Never edit the patch files with
tools that normalize line endings; regenerate with git and verify with
`file` (should report "with CRLF") before committing.

`kitchensink` carries one additional patch (link OpenGL explicitly on
Linux/BSD), tagged for its own upstream.

## SDK

`bitbake copperspice-demo-image -c populate_sdk` produces an SDK whose
host sysroot contains the CopperSpice libraries AND the five host tools
(`TOOLCHAIN_HOST_TASK` adds both `nativesdk-copperspice` and
`nativesdk-copperspice-tools` — the tools package split applies to
nativesdk too, so listing only the former would ship libraries with no
code generators).

SDK consumers building CopperSpice applications with CMake must pass the
five tool overrides. Verified invocation (qemuarm64 SDK):

```
. <sdk>/environment-setup-cortexa57-poky-linux
N=$OECORE_NATIVE_SYSROOT/usr/bin
cmake -G Ninja <src> \
    -DCS_TOOL_UIC=$N/uic \
    -DCS_TOOL_RCC=$N/rcc \
    -DCS_TOOL_LRELEASE=$N/lrelease \
    -DCS_TOOL_LCONVERT=$N/lconvert \
    -DCS_TOOL_LUPDATE=$N/lupdate
```

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
- **"platform plugin was not found" (key "xcb"):** the app cannot see the
  plugin directory. Check `/etc/profile.d/copperspice.sh` is present and
  the session sourced it (`echo $CS_PLUGIN_PATH`).

## Updating the pins

The kas configs pin exact upstream commits for reproducibility. To move
to newer wrynose point releases, update the `commit:` values in
`kas/base.yml` deliberately (`git ls-remote <url> <branch>`), rebuild,
and commit the bump on its own.
