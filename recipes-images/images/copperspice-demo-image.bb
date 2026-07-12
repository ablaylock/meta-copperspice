require recipes-sato/images/core-image-sato.bb

DESCRIPTION = "core-image-sato plus the CopperSpice demo applications, \
used to verify CopperSpice on the QEMU machines"

IMAGE_INSTALL += "cs-hello kitchensink cs-svg-repro"

# SDKs generated from this image ship the CopperSpice host libraries AND
# the host tools: the ${PN}-tools split (which keeps uic/rcc out of target
# images) applies to the nativesdk variant too, so the tools must be
# pulled in explicitly or the SDK gets libraries with no code generators.
TOOLCHAIN_HOST_TASK:append = " nativesdk-copperspice nativesdk-copperspice-tools"
