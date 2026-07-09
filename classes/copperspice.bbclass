# Inherit this class in recipes that build CopperSpice applications.
# It wires the cross-compile tool split: libraries come from the target
# copperspice build, while uic/rcc/lrelease/lconvert/lupdate come from
# copperspice-native (the CS_TOOL_* variables are honored by the patched
# CopperSpiceConfig.cmake).

inherit cmake pkgconfig

DEPENDS:append:class-target = " copperspice copperspice-native"
DEPENDS:append:class-nativesdk = " nativesdk-copperspice copperspice-native"

EXTRA_OECMAKE:append = " \
    -DCS_TOOL_UIC=${STAGING_BINDIR_NATIVE}/uic \
    -DCS_TOOL_RCC=${STAGING_BINDIR_NATIVE}/rcc \
    -DCS_TOOL_LRELEASE=${STAGING_BINDIR_NATIVE}/lrelease \
    -DCS_TOOL_LCONVERT=${STAGING_BINDIR_NATIVE}/lconvert \
    -DCS_TOOL_LUPDATE=${STAGING_BINDIR_NATIVE}/lupdate \
"
