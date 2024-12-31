Test Yocto Layer for [CopperSpice](https://www.copperspice.com/).

Basic setup instructions
```
# Clone the base yocto project
git clone -b kirkstone https://github.com/yoctoproject/poky.git

# Clone this repo
git clone https://github.com/ablaylock/meta-copperspice.git

# Set up the build environment for yocto
cd poky; . oe-init-build-env build;

# Append this repo as a layer to the yocto build
vi build/conf/bblayers.conf # Add the full path to meta-copperspice to the BBLAYERS variable
```