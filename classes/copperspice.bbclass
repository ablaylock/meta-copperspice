# Inherit this class in recipes that build CopperSpice applications.
# It wires the cross-compile tool split: libraries come from the target
# copperspice build, while uic/rcc/lrelease/lconvert/lupdate and
# cs_wayland_scanner come from copperspice-native (the CS_TOOL_*
# variables are honored by the patched CopperSpiceConfig.cmake).

inherit cmake pkgconfig

DEPENDS:append:class-target = " copperspice copperspice-native"
DEPENDS:append:class-nativesdk = " nativesdk-copperspice copperspice-native"
# class-native consumers are not supported: they would inherit the
# CS_TOOL_* settings below but no CopperSpice dependency

EXTRA_OECMAKE:append = " \
    -DCS_TOOL_UIC=${STAGING_BINDIR_NATIVE}/uic \
    -DCS_TOOL_RCC=${STAGING_BINDIR_NATIVE}/rcc \
    -DCS_TOOL_LRELEASE=${STAGING_BINDIR_NATIVE}/lrelease \
    -DCS_TOOL_LCONVERT=${STAGING_BINDIR_NATIVE}/lconvert \
    -DCS_TOOL_LUPDATE=${STAGING_BINDIR_NATIVE}/lupdate \
    -DCS_TOOL_CS_WAYLAND_SCANNER=${STAGING_BINDIR_NATIVE}/cs_wayland_scanner \
"

# rcc records the absolute path of every input file as a comment in its
# generated qrc_*.cpp; those files ship in ${PN}-src and trip the
# buildpaths QA check, so scrub the TMPDIR prefix after compiling
do_compile:append() {
    find ${B} -name 'qrc_*.cpp' -exec sed -i -e 's|${TMPDIR}|/buildpaths-scrubbed|g' {} +
}
