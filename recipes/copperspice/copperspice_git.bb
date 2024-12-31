# Recipe created by recipetool
# This is the basis of a recipe and may need further editing in order to be fully functional.
# (Feel free to remove these comments when editing.)

SUMMARY = "A library of functions for manipulating MNG format files."
# WARNING: the following LICENSE and LIC_FILES_CHKSUM values are best guesses - it is
# your responsibility to verify that the values are complete and correct.
#
# The following license files were not able to be identified and are
# represented as "Unknown" below, you will need to check them yourself:
#   license/LGPL_EXCEPTION.txt
#   license/LICENSE.FDL
#   src/3rdparty/easing/legal.doc
#   src/3rdparty/fonts/COPYING.Helvetica
#   src/3rdparty/fonts/COPYING.Utopia
#   src/3rdparty/fonts/COPYRIGHT.BH
#   src/3rdparty/fonts/COPYRIGHT.Charter
#   src/3rdparty/fonts/COPYRIGHT.Courier
#   src/3rdparty/fonts/COPYRIGHT.DejaVu
#   src/3rdparty/fonts/COPYRIGHT.IBM
#   src/3rdparty/fonts/COPYRIGHT.Unifont
#   src/3rdparty/fonts/COPYRIGHT.Vera
#   src/3rdparty/freetype/LICENSE.TXT
#   src/3rdparty/harfbuzz/COPYING
#   src/3rdparty/javascriptcore/JavaScriptCore/COPYING.LIB
#   src/3rdparty/javascriptcore/JavaScriptCore/pcre/COPYING
#   src/3rdparty/libmng/LICENSE
#   src/3rdparty/libpng/LICENSE
#   src/3rdparty/libtiff/COPYRIGHT
#   src/3rdparty/webkit/Source/JavaScriptCore/COPYING.LIB
#   src/3rdparty/webkit/Source/WebCore/LICENSE-APPLE
#   src/3rdparty/webkit/Source/WebCore/LICENSE-LGPL-2
#   src/3rdparty/webkit/Source/WebKit/LICENSE
#   src/plugins/platforms/cocoa/APPLE_LICENSE.TXT
#
# NOTE: multiple licenses have been detected; they have been separated with &
# in the LICENSE value for now since it is a reasonable assumption that all
# of the licenses apply. If instead there is a choice between the multiple
# licenses then you should change the value to separate the licenses with |
# instead of &. If there is any doubt, check the accompanying documentation
# to determine which situation is applicable.
LICENSE = "LGPL-2.1-only & Unknown"
LIC_FILES_CHKSUM = "file://license/LGPL_EXCEPTION.txt;md5=411080a56ff917a5a1aa08c98acae354 \
                    file://license/LICENSE.FDL;md5=6d9f2a9af4c8b8c3c769f6cc1b6aaf7e \
                    file://license/LICENSE.LGPL;md5=fbc093901857fcd118f065f900982c24 \
                    file://src/3rdparty/easing/legal.doc;md5=6e7cda30f8708293c66c8b758e37b377 \
                    file://src/3rdparty/fonts/COPYING.Helvetica;md5=5dfa0fdf45473b4ca0acf37d854df10e \
                    file://src/3rdparty/fonts/COPYING.Utopia;md5=fa13e704b7241f60ef9105cc041b9732 \
                    file://src/3rdparty/fonts/COPYRIGHT.BH;md5=dc7b6cafe5fa08e7deae428dc82fc62e \
                    file://src/3rdparty/fonts/COPYRIGHT.Charter;md5=fdce7a1e5844bdf133c752a722220cf5 \
                    file://src/3rdparty/fonts/COPYRIGHT.Courier;md5=fdce7a1e5844bdf133c752a722220cf5 \
                    file://src/3rdparty/fonts/COPYRIGHT.DejaVu;md5=60ed8cfab589ec1db22b584ef1e95a70 \
                    file://src/3rdparty/fonts/COPYRIGHT.IBM;md5=2dc3bb5f6df5a00597ee00f55597dd99 \
                    file://src/3rdparty/fonts/COPYRIGHT.Unifont;md5=c4c0d4c67a214d866c782e3b379a866e \
                    file://src/3rdparty/fonts/COPYRIGHT.Vera;md5=27d7484b1e18d0ee4ce538644a3f04be \
                    file://src/3rdparty/freetype/LICENSE.TXT;md5=a5927784d823d443c6cae55701d01553 \
                    file://src/3rdparty/harfbuzz/COPYING;md5=b98429b8e8e3c2a67cfef01e99e4893d \
                    file://src/3rdparty/javascriptcore/JavaScriptCore/COPYING.LIB;md5=d0c6d6397a5d84286dda758da57bd691 \
                    file://src/3rdparty/javascriptcore/JavaScriptCore/pcre/COPYING;md5=ba6e74c45bfcf7d22c6e6bee3bf29467 \
                    file://src/3rdparty/libmng/LICENSE;md5=32becdb8930f90eab219a8021130ec09 \
                    file://src/3rdparty/libpng/LICENSE;md5=b9b75399b72e4a8656cf3a6ddfc86d9a \
                    file://src/3rdparty/libtiff/COPYRIGHT;md5=34da3db46fab7501992f9615d7e158cf \
                    file://src/3rdparty/webkit/Source/JavaScriptCore/COPYING.LIB;md5=d0c6d6397a5d84286dda758da57bd691 \
                    file://src/3rdparty/webkit/Source/WebCore/LICENSE-APPLE;md5=4646f90082c40bcf298c285f8bab0b12 \
                    file://src/3rdparty/webkit/Source/WebCore/LICENSE-LGPL-2;md5=36357ffde2b64ae177b2494445b79d21 \
                    file://src/3rdparty/webkit/Source/WebCore/LICENSE-LGPL-2.1;md5=a778a33ef338abbaf8b8a7c36b6eec80 \
                    file://src/3rdparty/webkit/Source/WebKit/LICENSE;md5=4646f90082c40bcf298c285f8bab0b12 \
                    file://src/plugins/platforms/cocoa/APPLE_LICENSE.TXT;md5=0373af2da51306a8878d02fd4aefcdfe"

SRC_URI = "git://github.com/copperspice/copperspice.git;protocol=https;branch=master"

# Modify these as desired
PV = "1.0.10+git${SRCPV}"
SRCREV = "036ae96b22649e0e17976d8c206876ae5e68030c"

S = "${WORKDIR}/git"

DEPENDS = "openssl fontconfig libxcb alsa-lib jpeg cups glib-2.0 libxml2 libx11 libxcursor libxi virtual/libgl zlib libxcb virtual/libiconv virtual/xserver xcb-util-keysyms xcb-util-renderutil libxinerama libxkbcommon xcb-util libx11-native libxcb-native libxi-native xcb-util-image xcb-util-wm copperspice-native"



inherit cmake pkgconfig

# Specify any options you want to pass to cmake using EXTRA_OECMAKE:
TARGET_CFLAGS = "${@ "-D__ARM_ARCH_5__=5" if d.getVar('TARGET_ARCH') == 'arm' else "" }"

EXTRA_OECMAKE = " \
    -DWITH_OPENGL=NO \
    -DWITH_SCRIPT=NO \
    -DWITH_VULKAN=NO \
    -DWITH_WEBKIT=NO \
    -DWITH_MULTIMEDIA=NO \
    -DWITH_XMLPATTERNS=NO \
    -DWITH_SQL=NO \
    -DWITH_SVG=NO \
    -DWITH_NETWORK=NO \
"
#    -DWITH_GUI=NO \
#"

# Allow skipping for now while debugging the build linking issues
INSANE_SKIP:${PN} += " ldflags"
#INSANE_SKIP_${PN} += " ldflags"
SOLIBS = ".so"
FILES_SOLIBSDEV = ""
