require recipes-sato/images/core-image-sato.bb

DESCRIPTION = "core-image-sato plus the CopperSpice demo applications, \
used to verify CopperSpice on the QEMU machines"

IMAGE_INSTALL += "cs-hello kitchensink"

IMAGE_FEATURES += "ssh-server-dropbear"

# SDKs generated from this image ship the CopperSpice host tools
TOOLCHAIN_HOST_TASK:append = " nativesdk-copperspice"
