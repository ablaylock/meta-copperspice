require recipes-graphics/images/core-image-weston.bb

DESCRIPTION = "core-image-weston plus the CopperSpice demo applications, \
used to verify the CopperSpice wayland platform plugin on QEMU"

IMAGE_INSTALL += "cs-hello kitchensink"
