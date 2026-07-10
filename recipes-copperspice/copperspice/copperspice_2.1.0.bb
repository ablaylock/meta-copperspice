SUMMARY = "CopperSpice C++ cross-platform GUI library"
DESCRIPTION = "CopperSpice is a set of C++ libraries used to develop \
cross-platform GUI applications. It began as a fork of Qt 4.8 and was \
rewritten to use modern C++ (currently C++20) instead of moc-generated code."
HOMEPAGE = "https://www.copperspice.com"
BUGTRACKER = "https://github.com/copperspice/copperspice/issues"

LICENSE = "LGPL-2.1-only"
LIC_FILES_CHKSUM = " \
    file://license/LICENSE.LGPL;md5=7266a93b753b03bc5f00522e65722b79 \
    file://license/LGPL_EXCEPTION.txt;md5=f983e0c26cbfd50b8721a4f058fb152c \
    file://license/LICENSE.FDL;md5=92a9c8f80b75e6d5fcf0416c1ad9e667 \
"

# The release tarball unpacks bare (no top-level directory); subdir= gives it
# one so the default S = "${UNPACKDIR}/${BP}" works.
# Upstream publishes no checksums for its tarballs. This sha256 was computed
# from the tarball downloaded from upstream on 2026-07-08.
SRC_URI = "https://download.copperspice.com/copperspice/source/copperspice-${PV}.tar.bz2;subdir=${BP} \
           file://0001-cmake-support-prebuilt-host-tools-in-the-CopperSpice.patch \
           file://0002-cmake-support-prebuilt-host-tools-in-the-exported-co.patch \
           file://0003-cmake-only-request-the-OpenGL-EGL-component-when-Way.patch \
           file://0004-cmake-build-the-NEON-drawhelpers-on-64-bit-ARM.patch \
           file://0005-cmake-do-not-export-sysroot-paths-for-private-depend.patch \
           file://0006-core-detect-32-bit-ARM-via-the-canonical-__ARM_ARCH-.patch \
           "
SRC_URI[sha256sum] = "377844cd3b9199f763411e8c7705f00a50b5d6f695541ad378597ee2355319e2"

inherit cmake pkgconfig features_check

REQUIRED_DISTRO_FEATURES:class-target = "x11 opengl"

DEPENDS:class-target = " \
    copperspice-native \
    alsa-lib \
    cups \
    fontconfig \
    freetype \
    glib-2.0 \
    gstreamer1.0 \
    gstreamer1.0-plugins-base \
    jpeg \
    libx11 \
    libxcb \
    libxcursor \
    libxi \
    libxinerama \
    libxkbcommon \
    libxml2 \
    openssl \
    sqlite3 \
    virtual/libgl \
    virtual/libiconv \
    xcb-util \
    xcb-util-image \
    xcb-util-keysyms \
    xcb-util-renderutil \
    xcb-util-wm \
    zlib \
"
DEPENDS:class-native = "glib-2.0-native"
DEPENDS:class-nativesdk = "nativesdk-glib-2.0"

# Yocto's default -fvisibility-inlines-hidden breaks CsGui linking
# (confirmed: https://forum.copperspice.com/viewtopic.php?t=4121)
CXXFLAGS:remove = "-fvisibility-inlines-hidden"

# CopperSpice compile units need roughly 4 GB of RAM per thread; cap the
# make-level parallelism for this recipe (applies to all class variants)
PARALLEL_MAKE = "-j 10"

# With full -g, libCsGui2.1.so exceeds 4 GiB and a 32-bit ELF cannot
# represent file offsets past that, so the linker emits a structurally
# broken library that downstream links reject with "file too short".
# Line-tables-only debug info keeps 32-bit ARM comfortably inside the
# format limit (ELF64 targets are unaffected and keep full -g).
DEBUG_LEVELFLAG:arm = "-g1"

# Target build enables everything KitchenSink links against. WebKit stays
# off (KitchenSink's CsWebKit use is disabled upstream); no Vulkan in the
# QEMU images.
# The wayland platform plugin would need cs_wayland_scanner at build
# time (not provided by copperspice-native) and wayland libs can leak
# into the sysroot transitively via mesa - disable detection so the
# plugin state is deterministic.
EXTRA_OECMAKE:class-target = " \
    -DWITH_WEBKIT=NO \
    -DWITH_VULKAN=NO \
    -DCMAKE_DISABLE_FIND_PACKAGE_Wayland=TRUE \
"

# When cross compiling, CopperSpice's own build runs uic/rcc/lrelease to
# process its .ui/.qrc/.ts files - use the host tools from
# copperspice-native (the CS_TOOL_* variables are added by our patches).
# cs_wayland_scanner is not needed: no wayland libs are in DEPENDS, so the
# wayland platform plugin is never enabled.
EXTRA_OECMAKE:class-target += " \
    -DCS_TOOL_UIC=${STAGING_BINDIR_NATIVE}/uic \
    -DCS_TOOL_RCC=${STAGING_BINDIR_NATIVE}/rcc \
    -DCS_TOOL_LRELEASE=${STAGING_BINDIR_NATIVE}/lrelease \
"

# native/nativesdk builds exist to provide the build tools (uic, rcc,
# lrelease, lconvert, lupdate); those need only CsCore and CsXml
CS_FEATURES_OFF = " \
    -DWITH_GUI=NO \
    -DWITH_MULTIMEDIA=NO \
    -DWITH_NETWORK=NO \
    -DWITH_OPENGL=NO \
    -DWITH_SQL=NO \
    -DWITH_SVG=NO \
    -DWITH_VULKAN=NO \
    -DWITH_WEBKIT=NO \
    -DWITH_XMLPATTERNS=NO \
"
EXTRA_OECMAKE:class-native = "${CS_FEATURES_OFF}"
EXTRA_OECMAKE:class-nativesdk = "${CS_FEATURES_OFF}"

BBCLASSEXTEND = "native nativesdk"

# rcc records the absolute path of every input file as a comment in the
# generated qrc_*.cpp, which then lands in the copperspice-src debug
# sources and trips the buildpaths QA check. The paths only occur in
# generated comments (verified: no occurrences in code or resource
# data), so neutralizing them changes nothing but the comment text.
do_compile:append() {
    find ${B} -name "qrc_*.cpp" -exec sed -i -e "s|${TMPDIR}|/buildpaths-scrubbed|g" {} +
}

# CopperSpice deliberately ships unversioned libraries: the ABI is encoded
# in the file name (libCsCore2.1.so) instead of an SONAME suffix. The .so
# files are therefore runtime libraries, not dev symlinks.
SOLIBS = ".so"
FILES_SOLIBSDEV = ""

PACKAGES =+ "${PN}-tools"

# Target-arch uic/rcc/lrelease/lconvert/lupdate/linguist: only useful for
# doing development ON the device. Host builds use copperspice-native;
# images must not pull this in.
FILES:${PN}-tools = "${bindir}"

# CopperSpice plugins (CsGuiXcb2.1.so, CsImageFormatsSvg2.1.so, etc.) are
# installed flat in ${libdir} using bare names with no "lib" prefix - a
# deliberate upstream convention distinguishing plugins from the main
# libraries (libCs*2.1.so). At runtime, however, QFactoryLoader only
# searches <librarypath>/<category>/ (e.g. .../platforms/CsGuiXcb2.1.so);
# a flat ${libdir} is never searched, so no GUI app can start ("platform
# plugin was not found", key "xcb"). Upstream expects each application to
# bundle plugins beside its executable; for a system-wide install we
# instead arrange the categorized layout under ${libdir}/copperspice and
# publish it through CS_PLUGIN_PATH (honored by
# QCoreApplication::libraryPaths), set for login shells and the X session
# alike via /etc/profile.d (Xsession sources /etc/profile).
do_install:append:class-target() {
    for plugin in ${D}${libdir}/Cs*.so; do
        [ -e "$plugin" ] || continue
        case "$(basename $plugin)" in
            CsGuiXcb_Glx*)       category=xcbglintegrations ;;
            CsGuiXcb*)           category=platforms ;;
            CsImageFormats*)     category=imageformats ;;
            CsMultimedia_m3u*)   category=playlistformats ;;
            CsMultimedia_gst_*)  category=mediaservices ;;
            CsPrinterDriver*)    category=printerdrivers ;;
            *) bbfatal "unclassified CopperSpice plugin: $plugin" ;;
        esac
        install -d ${D}${libdir}/copperspice/plugins/$category
        mv "$plugin" ${D}${libdir}/copperspice/plugins/$category/
    done

    install -d ${D}${sysconfdir}/profile.d
    echo "export CS_PLUGIN_PATH=${libdir}/copperspice/plugins" \
        > ${D}${sysconfdir}/profile.d/copperspice.sh
}

FILES:${PN} += "${libdir}/copperspice/plugins ${sysconfdir}/profile.d/copperspice.sh"

# nativesdk keeps the flat upstream layout (its plugins ship as-installed;
# the SDK host tools do not load GUI plugins)
FILES:${PN}:append:class-nativesdk = " ${libdir}/Cs*.so"
