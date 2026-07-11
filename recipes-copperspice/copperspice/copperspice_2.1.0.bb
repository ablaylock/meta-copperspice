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
           file://0007-cmake-allow-building-cs_wayland_scanner-standalone.patch \
           file://0008-cmake-demote-the-X11-stack-from-REQUIRED-to-RECOMMEN.patch \
           file://0009-cmake-locate-EGL-without-FindOpenGL-for-the-Wayland-.patch \
           "
SRC_URI[sha256sum] = "377844cd3b9199f763411e8c7705f00a50b5d6f695541ad378597ee2355319e2"

inherit cmake pkgconfig features_check

# Computed from the selected PACKAGECONFIG. Any GUI build needs libGL:
# CsGui compiles its QOpenGL* classes unconditionally
# (src/gui/CMakeLists.txt includes opengl/opengl.cmake with no condition)
# and upstream configure hard-requires OpenGL when WITH_GUI is on. The
# 'opengl' knob below only controls the separate CsOpenGL add-on library.

# gui without any platform knob cannot build. Requiring x11 here makes
# features_check (whose anonymous python runs before this recipe's) skip
# the recipe cleanly on distros lacking it, instead of the parse-time
# platform check below aborting the whole parse; on distros that do have
# x11, explicit misuse still reaches the platform check and its clearer
# message.
def cs_platform_fallback(d):
    pc = (d.getVar('PACKAGECONFIG') or '').split()
    if 'gui' in pc and 'x11' not in pc and 'wayland' not in pc:
        return 'x11'
    return ''

REQUIRED_DISTRO_FEATURES:class-target = " \
    ${@bb.utils.contains('PACKAGECONFIG', 'gui', 'opengl', '', d)} \
    ${@cs_platform_fallback(d)} \
    ${@bb.utils.contains('PACKAGECONFIG', 'x11', 'x11', '', d)} \
    ${@bb.utils.contains('PACKAGECONFIG', 'wayland', 'wayland', '', d)} \
    ${@bb.utils.contains('PACKAGECONFIG', 'vulkan', 'vulkan', '', d)} \
"

DEPENDS:class-target = "copperspice-native virtual/libiconv zlib"
DEPENDS:class-native = "glib-2.0-native"
DEPENDS:class-nativesdk = "nativesdk-glib-2.0"

# The default knob set reproduces the feature set this layer has always
# built: GUI toolkit on X11 with multimedia, OpenGL, SVG, SQL(sqlite),
# XmlPatterns, TLS, CUPS printing, and glib event-loop integration.
# On distros with wayland in DISTRO_FEATURES (poky default) the wayland
# platform plugin is built too; xcb remains the runtime default.
PACKAGECONFIG ??= " \
    gui network \
    ${@bb.utils.filter('DISTRO_FEATURES', 'x11 wayland', d)} \
    multimedia opengl svg sql xmlpatterns openssl cups glib \
"
PACKAGECONFIG:class-native = ""
PACKAGECONFIG:class-nativesdk = ""

# Component switches (upstream WITH_* options). Interdependencies are
# validated at parse time below, mirroring upstream's configure rules.
PACKAGECONFIG[gui]         = "-DWITH_GUI=YES,-DWITH_GUI=NO -DCMAKE_DISABLE_FIND_PACKAGE_JPEG=TRUE,fontconfig freetype jpeg virtual/libgl"
PACKAGECONFIG[network]     = "-DWITH_NETWORK=YES,-DWITH_NETWORK=NO"
PACKAGECONFIG[opengl]      = "-DWITH_OPENGL=YES,-DWITH_OPENGL=NO"
PACKAGECONFIG[multimedia]  = "-DWITH_MULTIMEDIA=YES,-DWITH_MULTIMEDIA=NO,gstreamer1.0 gstreamer1.0-plugins-base"
PACKAGECONFIG[svg]         = "-DWITH_SVG=YES,-DWITH_SVG=NO"
PACKAGECONFIG[sql]         = "-DWITH_SQL=YES,-DWITH_SQL=NO -DCMAKE_DISABLE_FIND_PACKAGE_SQLite3=TRUE,sqlite3"
PACKAGECONFIG[xmlpatterns] = "-DWITH_XMLPATTERNS=YES,-DWITH_XMLPATTERNS=NO"
# vulkan-loader requires the 'vulkan' distro feature (enforced above)
PACKAGECONFIG[vulkan]      = "-DWITH_VULKAN=YES,-DWITH_VULKAN=NO,vulkan-loader vulkan-headers"
PACKAGECONFIG[webkit]      = "-DWITH_WEBKIT=YES,-DWITH_WEBKIT=NO,libxml2"

# SQL driver plugins. The client libraries live in meta-openembedded's
# meta-oe layer, which this layer does not otherwise require; enabling
# one of these knobs requires meta-oe in bblayers (see README).
PACKAGECONFIG[psql]        = "-DWITH_PSQL_PLUGIN=YES,-DWITH_PSQL_PLUGIN=NO -DCMAKE_DISABLE_FIND_PACKAGE_PostgreSQL=TRUE,postgresql"
PACKAGECONFIG[mysql]       = "-DWITH_MYSQL_PLUGIN=YES,-DWITH_MYSQL_PLUGIN=NO -DCMAKE_DISABLE_FIND_PACKAGE_MySQL=TRUE,mariadb"
PACKAGECONFIG[odbc]        = "-DWITH_ODBC_PLUGIN=YES,-DWITH_ODBC_PLUGIN=NO -DCMAKE_DISABLE_FIND_PACKAGE_ODBC=TRUE,unixodbc"

# Detection-only features: upstream has no WITH_* switch, so the OFF side
# must forbid find_package() or the feature state would depend on what
# happens to be staged in the sysroot (e.g. wayland leaking in via mesa).
# CsNetwork dlopens libssl/libcrypto at runtime instead of linking them,
# hence the RDEPENDS entries on the openssl knob.
PACKAGECONFIG[openssl]     = ",-DCMAKE_DISABLE_FIND_PACKAGE_OpenSSL=TRUE,openssl,libssl libcrypto"
PACKAGECONFIG[cups]        = ",-DCMAKE_DISABLE_FIND_PACKAGE_Cups=TRUE,cups"
PACKAGECONFIG[pulseaudio]  = ",-DCMAKE_DISABLE_FIND_PACKAGE_PulseAudio=TRUE,pulseaudio"
PACKAGECONFIG[glib]        = ",-DCMAKE_DISABLE_FIND_PACKAGE_GLib2=TRUE -DCMAKE_DISABLE_FIND_PACKAGE_GObject2=TRUE,glib-2.0"
# XKBCommon (unlike XKBCommon_X11) serves both X11 and Wayland, so
# libxkbcommon rides each platform knob rather than the disable list.
PACKAGECONFIG[x11]         = ",-DCMAKE_DISABLE_FIND_PACKAGE_XCB=TRUE -DCMAKE_DISABLE_FIND_PACKAGE_X11=TRUE -DCMAKE_DISABLE_FIND_PACKAGE_XKBCommon_X11=TRUE -DCMAKE_DISABLE_FIND_PACKAGE_X11_XCB=TRUE,libice libsm libx11 libxcb libxcursor libxi libxinerama libxkbcommon xcb-util xcb-util-image xcb-util-keysyms xcb-util-renderutil xcb-util-wm"
# Wayland platform plugin. Protocol marshalling code is generated at
# build time on the host: cs_wayland_scanner comes from
# copperspice-native (patch 0007 + the CS_TOOL hook in patch 0001), the
# C protocol stubs from the standard wayland-scanner (wayland-native).
# CS bundles its protocol XML, so wayland-protocols is not needed.
# virtual/egl: upstream only builds the plugin when TARGET OpenGL::EGL
# exists. The OFF side keeps wayland detection deterministic (mesa can
# leak libwayland into the sysroot).
PACKAGECONFIG[wayland]     = "-DCS_TOOL_CS_WAYLAND_SCANNER=${STAGING_BINDIR_NATIVE}/cs_wayland_scanner,-DCMAKE_DISABLE_FIND_PACKAGE_Wayland=TRUE,wayland wayland-native libxkbcommon virtual/egl"

# ALSA is dead code in CS 2.1.0: find_package(ALSA) runs but the result
# is consumed nowhere in the source tree (the only CS-level audio backend
# is PulseAudio). Forbid detection so configure output cannot depend on
# alsa-lib leaking into the sysroot via other recipes.
EXTRA_OECMAKE:class-target = " -DCMAKE_DISABLE_FIND_PACKAGE_ALSA=TRUE"

# Mirror upstream's component dependency rules (top-level CMakeLists.txt
# "check components dependencies" block) so an invalid selection fails at
# parse time naming the knobs, instead of deep inside do_configure.
python __anonymous() {
    if d.getVar('CLASSOVERRIDE') != 'class-target':
        return

    pc = (d.getVar('PACKAGECONFIG') or "").split()

    rules = {
        'multimedia':  ['gui', 'network', 'opengl', 'glib'],
        'opengl':      ['gui'],
        'svg':         ['gui'],
        'xmlpatterns': ['network'],
        'webkit':      ['gui', 'network'],
        'psql':        ['sql'],
        'mysql':       ['sql'],
        'odbc':        ['sql'],
    }
    for knob, needs in sorted(rules.items()):
        missing = [n for n in needs if n not in pc]
        if knob in pc and missing:
            bb.fatal("copperspice: PACKAGECONFIG '%s' also requires: %s"
                     % (knob, ' '.join(missing)))

    if 'gui' in pc and 'x11' not in pc and 'wayland' not in pc:
        bb.fatal("copperspice: PACKAGECONFIG 'gui' needs a platform "
                 "plugin: add 'x11' or 'wayland'")
}

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

# When cross compiling, CopperSpice's own build runs uic/rcc/lrelease to
# process its .ui/.qrc/.ts files - use the host tools from
# copperspice-native (the CS_TOOL_* variables are added by our patches).
EXTRA_OECMAKE:class-target += " \
    -DCS_TOOL_UIC=${STAGING_BINDIR_NATIVE}/uic \
    -DCS_TOOL_RCC=${STAGING_BINDIR_NATIVE}/rcc \
    -DCS_TOOL_LRELEASE=${STAGING_BINDIR_NATIVE}/lrelease \
"

# native/nativesdk builds exist to provide the build tools (uic, rcc,
# lrelease, lconvert, lupdate, cs_wayland_scanner); those need only
# CsCore and CsXml
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
EXTRA_OECMAKE:class-native = "${CS_FEATURES_OFF} -DWITH_WAYLAND_SCANNER=YES"
EXTRA_OECMAKE:class-nativesdk = "${CS_FEATURES_OFF} -DWITH_WAYLAND_SCANNER=YES"

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
            # all wayland sub-plugins (generic platform, egl client-buffer
            # integration, bradient decoration) are loaded from the
            # "platforms" category - every wayland QFactoryLoader in
            # src/plugins/platforms/wayland/client uses "/platforms"
            CsGuiWayland*)       category=platforms ;;
            CsImageFormats*)     category=imageformats ;;
            CsMultimedia_m3u*)   category=playlistformats ;;
            CsMultimedia_gst_*)  category=mediaservices ;;
            CsPrinterDriver*)    category=printerdrivers ;;
            CsSql*)              category=sqldrivers ;;
            *) bbfatal "unclassified CopperSpice plugin: $plugin" ;;
        esac
        install -d ${D}${libdir}/copperspice/plugins/$category
        mv "$plugin" ${D}${libdir}/copperspice/plugins/$category/
    done

    install -d ${D}${sysconfdir}/profile.d
    echo "export CS_PLUGIN_PATH=${libdir}/copperspice/plugins" \
        > ${D}${sysconfdir}/profile.d/copperspice.sh
    # xcb is upstream's hardcoded default platform. On a wayland-only
    # build the default must be overridden system-wide or every GUI app
    # aborts looking for the missing xcb plugin.
    if ${@bb.utils.contains('PACKAGECONFIG', 'wayland', 'true', 'false', d)} && \
       ! ${@bb.utils.contains('PACKAGECONFIG', 'x11', 'true', 'false', d)}; then
        echo "export CS_GUI_PLATFORM_NAME=wayland" \
            >> ${D}${sysconfdir}/profile.d/copperspice.sh
    fi
}

FILES:${PN} += "${libdir}/copperspice/plugins ${sysconfdir}/profile.d/copperspice.sh"

# nativesdk keeps the flat upstream layout (its plugins ship as-installed;
# the SDK host tools do not load GUI plugins)
FILES:${PN}:append:class-nativesdk = " ${libdir}/Cs*.so"
