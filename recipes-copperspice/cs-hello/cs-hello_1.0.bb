SUMMARY = "Minimal CopperSpice GUI demo application"
DESCRIPTION = "One dialog built from a .ui form (uic) showing a greeting \
loaded from a .qrc resource (rcc). Smoke test for the CopperSpice \
cross-compile tool split, and a reference consumer of copperspice.bbclass."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://CMakeLists.txt \
    file://main.cpp \
    file://hello.ui \
    file://resources.qrc \
    file://greeting.txt \
    file://cs-hello.desktop \
"

S = "${UNPACKDIR}"

inherit copperspice features_check

# the demo apps need a windowing platform - either one will do
ANY_OF_DISTRO_FEATURES = "x11 wayland"

do_install:append() {
    install -d ${D}${datadir}/applications
    install -m 0644 ${UNPACKDIR}/cs-hello.desktop ${D}${datadir}/applications/
}
