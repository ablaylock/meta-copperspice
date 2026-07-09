#!/bin/sh
# Verify the CopperSpice host/target tool split for one machine.
# Run inside 'kas shell' from the build directory:
#   ../meta-copperspice/scripts/verify-tool-split.sh <machine>
set -u

MACHINE="$1"
DEPLOY="tmp/deploy/images/${MACHINE}"
fail=0

# 1. target-arch tools must not be deployed to the image
MANIFEST=$(ls "${DEPLOY}"/copperspice-demo-image-"${MACHINE}"*.manifest 2>/dev/null | head -n1)
if [ -z "${MANIFEST}" ]; then
   echo "FAIL: no image manifest found in ${DEPLOY}"
   exit 1
fi

if grep -q "^copperspice-tools " "${MANIFEST}"; then
   echo "FAIL: copperspice-tools (target-arch tools) is in the image manifest"
   fail=1
else
   echo "PASS: image does not contain copperspice-tools"
fi

# 2. the native tools used during the build are host binaries
for t in uic rcc lrelease lconvert lupdate; do
   p=$(find tmp/work/*-linux/copperspice-native -path "*/sysroot-destdir/*/bin/$t" 2>/dev/null | head -n1)
   if [ -n "$p" ] && file "$p" | grep -q "x86-64"; then
      echo "PASS: native $t is a host binary"
   else
      echo "FAIL: native $t missing or not a host binary ($p)"
      fail=1
   fi
done

# 3. the packaged target tools are target binaries
pkgdir=$(find tmp/work -path "*/copperspice/*/packages-split/copperspice-tools/usr/bin" -type d 2>/dev/null | head -n1)
if [ -n "$pkgdir" ] && file "$pkgdir/uic" | grep -qv "x86-64"; then
   echo "PASS: packaged copperspice-tools/uic is a target binary"
elif [ "${MACHINE}" = "qemux86-64" ]; then
   echo "INFO: target == host arch on qemux86-64, arch check skipped"
else
   echo "FAIL: packaged target uic missing or wrong arch"
   fail=1
fi

[ $fail -eq 0 ] && echo "ALL CHECKS PASSED" || echo "CHECKS FAILED"
exit $fail
