SUMMARY = "SVG rendering corruption repro for the KitchenSink SVG view"
DESCRIPTION = "Loads the six KitchenSink SVGs through the same \
QSvgRenderer/QImage/QTextEdit text-object pipeline KitchenSink uses \
(including its uninitialized buffer). GUI mode mirrors the demo; \
--dump mode writes raw/control/drawn PNGs per SVG for objective \
cross-arch comparison."
LICENSE = "BSD-2-Clause"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/BSD-2-Clause;md5=cb641bc04cda31daea161b1bc15da69f"

SRC_URI = " \
    file://CMakeLists.txt \
    file://main.cpp \
    file://svgrepro_render.h \
    file://svgrepro_render.cpp \
    file://svgrepro_window.h \
    file://svgrepro_window.cpp \
    file://svgtextobject.h \
    file://svgtextobject.cpp \
    file://resources.qrc \
    file://resources/pineapple.svg \
    file://resources/watermelon.svg \
    file://resources/cake1.svg \
    file://resources/cake2.svg \
    file://resources/cup_cake.svg \
    file://resources/ice_cream.svg \
    file://cs-svg-repro.desktop \
"

S = "${UNPACKDIR}"

inherit copperspice features_check

# the demo apps need a windowing platform - either one will do
ANY_OF_DISTRO_FEATURES = "x11 wayland"

do_install:append() {
    install -d ${D}${datadir}/applications
    install -m 0644 ${UNPACKDIR}/cs-svg-repro.desktop ${D}${datadir}/applications/
}
