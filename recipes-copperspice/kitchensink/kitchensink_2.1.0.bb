SUMMARY = "CopperSpice KitchenSink demo application"
DESCRIPTION = "Official CopperSpice demo exercising most library components \
(widgets, SQL, SVG, XML patterns, network) and the uic/rcc/lrelease build \
tools (30+ .ui forms, resources, translations)."
HOMEPAGE = "https://github.com/copperspice/kitchensink"
LICENSE = "BSD-2-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=ff94566f728b63bc7cfa537805eacf36"

SRC_URI = " \
    git://github.com/copperspice/kitchensink.git;protocol=https;branch=master \
    file://0001-cmake-link-OpenGL-explicitly-on-Linux-BSD.patch \
    file://kitchensink.desktop \
"
# tag ks-2.1.0
SRCREV = "bcdfe7ef3d51b3be3c5528965805e4eeab52d2d9"

inherit copperspice features_check

REQUIRED_DISTRO_FEATURES = "x11"

# Upstream's install step builds a portable-app layout and copies the
# CopperSpice libraries next to the binary (cs_copy_library). Skip it and
# install the binary+resources sibling layout the app expects.
do_install() {
    install -d ${D}${libdir}/kitchensink/resources
    # CMAKE_RUNTIME_OUTPUT_DIRECTORY=bin plus OUTPUT_NAME=kitchensink on
    # Linux place the binary at ${B}/bin/kitchensink; install it under the
    # upstream target name KitchenSink used by the symlink and desktop entry
    install -m 0755 ${B}/bin/kitchensink ${D}${libdir}/kitchensink/KitchenSink
    install -m 0644 ${S}/resources/sampleMenu.xml ${D}${libdir}/kitchensink/resources/
    install -m 0644 ${S}/resources/ks.png ${D}${libdir}/kitchensink/resources/

    # translations produced by lrelease during the build; upstream sets
    # TS_OUTPUT_DIR to ${S}/resources, overriding the build folder
    for qm in $(find ${B} ${S}/resources -name "*.qm"); do
        install -m 0644 $qm ${D}${libdir}/kitchensink/resources/
    done

    install -d ${D}${bindir}
    ln -s ${libdir}/kitchensink/KitchenSink ${D}${bindir}/kitchensink

    install -d ${D}${datadir}/applications
    install -m 0644 ${UNPACKDIR}/kitchensink.desktop ${D}${datadir}/applications/
}

FILES:${PN} += "${libdir}/kitchensink"

# $ORIGIN rpath set by upstream CMake is expected for its resource lookup
INSANE_SKIP:${PN} += "useless-rpaths"
