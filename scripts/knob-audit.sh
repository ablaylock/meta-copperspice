#!/bin/sh
# Per-knob PACKAGECONFIG audit for the copperspice recipe on qemuarm64
# (a fully cross target: host-tool leakage that qemux86-64 cannot see
# fails loudly here).
#
# Run from the workspace root (the directory holding meta-copperspice
# and build/):
#   meta-copperspice/scripts/knob-audit.sh <config> [<config>...]
#   meta-copperspice/scripts/knob-audit.sh all
#
# Per configuration: build copperspice with the overridden PACKAGECONFIG,
# then check (a) forbidden NEEDED sonames are absent from every installed
# .so, (b) required NEEDED sonames are present where expected, (c)
# expected files/dirs are absent, (d) expected files are present, (e)
# "Found <pkg>" lines are absent from log.do_configure for disabled
# detection knobs. BESTEFFORT configs record a build failure without
# failing the audit (upstream webkit/vulkan are not this layer's to fix).
set -u

[ $# -ge 1 ] || { echo "usage: $0 <config>...|all" >&2; exit 2; }

KAS_CFG="meta-copperspice/kas/qemuarm64.yml:meta-copperspice/kas/audit.yml"
GEN="meta-copperspice/kas/audit-gen"
WORK="build/tmp/work/cortexa57-poky-linux/copperspice/2.1.0"
OUT="build/knob-audit"
DEFAULT="gui network x11 multimedia opengl svg sql xmlpatterns openssl cups glib"

ALL="no-multimedia no-opengl no-svg no-sql no-xmlpatterns no-openssl \
no-cups no-glib no-network with-pulseaudio with-psql with-mysql \
with-odbc with-webkit with-vulkan min-gui headless"

mkdir -p "$OUT"
RESULTS="$OUT/results.tsv"

# configure(): set the per-config variables. PC is the PACKAGECONFIG
# override; the remaining fields are space-separated lists (empty = skip
# that check). REQUIRE_NEEDED entries are <lib-glob>:<soname> pairs.
configure() {
   PC="" FORBID="" ABSENT="" PRESENT="" GREP_ABSENT="" REQUIRE_NEEDED="" BESTEFFORT=0
   case "$1" in
   no-multimedia)
      PC="gui network x11 opengl svg sql xmlpatterns openssl cups glib"
      FORBID="libgstreamer-1.0.so.0"
      ABSENT="libCsMultimedia2.1.so copperspice/plugins/mediaservices copperspice/plugins/playlistformats" ;;
   no-opengl) # upstream rule: dropping opengl also drops multimedia
      PC="gui network x11 svg sql xmlpatterns openssl cups glib"
      ABSENT="libCsOpenGL2.1.so libCsMultimedia2.1.so"
      PRESENT="libCsGui2.1.so" ;;   # CsGui itself still links libGL - expected
   no-svg)
      PC="gui network x11 multimedia opengl sql xmlpatterns openssl cups glib"
      ABSENT="libCsSvg2.1.so"
      GREP_ABSENT="" ;;
   no-sql)
      PC="gui network x11 multimedia opengl svg xmlpatterns openssl cups glib"
      FORBID="libsqlite3.so.0"
      ABSENT="libCsSql2.1.so" ;;
   no-xmlpatterns)
      PC="gui network x11 multimedia opengl svg sql openssl cups glib"
      ABSENT="libCsXmlPatterns2.1.so" ;;
   no-openssl) # CsNetwork dlopens ssl, so the configure log is the check
      PC="gui network x11 multimedia opengl svg sql xmlpatterns cups glib"
      GREP_ABSENT="Found OpenSSL" ;;
   no-cups)
      PC="gui network x11 multimedia opengl svg sql xmlpatterns openssl glib"
      FORBID="libcups.so.2"
      ABSENT="copperspice/plugins/printerdrivers" ;;
   no-glib)
      PC="gui network x11 multimedia opengl svg sql xmlpatterns openssl cups"
      FORBID="libglib-2.0.so.0 libgobject-2.0.so.0" ;;
   no-network) # network off forces multimedia+xmlpatterns off too
      PC="gui x11 svg sql openssl cups glib"
      ABSENT="libCsNetwork2.1.so libCsXmlPatterns2.1.so libCsMultimedia2.1.so" ;;
   with-pulseaudio)
      PC="$DEFAULT pulseaudio"
      REQUIRE_NEEDED="libCsMultimedia2.1.so:libpulse.so.0" ;;
   with-psql)
      PC="$DEFAULT psql"
      PRESENT="copperspice/plugins/sqldrivers"
      REQUIRE_NEEDED="copperspice/plugins/sqldrivers/CsSql*.so:libpq.so.5" ;;
   with-mysql)
      PC="$DEFAULT mysql"
      PRESENT="copperspice/plugins/sqldrivers" ;;
   with-odbc)
      PC="$DEFAULT odbc"
      PRESENT="copperspice/plugins/sqldrivers"
      REQUIRE_NEEDED="copperspice/plugins/sqldrivers/CsSql*.so:libodbc.so.2" ;;
   with-webkit)
      PC="$DEFAULT webkit"
      PRESENT="libCsWebKit2.1.so"
      BESTEFFORT=1 ;;
   with-vulkan)
      PC="$DEFAULT vulkan"
      PRESENT="libCsVulkan2.1.so"
      BESTEFFORT=1 ;;
   min-gui)
      PC="gui network x11"
      FORBID="libgstreamer-1.0.so.0 libsqlite3.so.0 libcups.so.2 libglib-2.0.so.0 libgobject-2.0.so.0"
      ABSENT="libCsMultimedia2.1.so libCsOpenGL2.1.so libCsSvg2.1.so libCsSql2.1.so libCsXmlPatterns2.1.so copperspice/plugins/printerdrivers"
      PRESENT="libCsGui2.1.so copperspice/plugins/platforms" ;;
   headless)
      PC=""
      FORBID="libGL.so.1 libX11.so.6 libfontconfig.so.1 libfreetype.so.6"
      ABSENT="libCsGui2.1.so copperspice/plugins"
      PRESENT="libCsCore2.1.so libCsXml2.1.so" ;;
   *) echo "unknown config: $1" >&2; exit 2 ;;
   esac
}

record() { printf '%s\t%s\t%s\n' "$1" "$2" "$3" | tee -a "$RESULTS"; }

run_one() {
   name="$1"; configure "$name"
   cat > "$GEN/$name.yml" <<EOF
header:
  version: 14
local_conf_header:
  knob-audit-override: |
    PACKAGECONFIG:pn-copperspice = "$PC"
EOF
   kas shell "$KAS_CFG:$GEN/$name.yml" -c 'bitbake copperspice' \
      > "$OUT/$name.build.log" 2>&1
   if [ $? -ne 0 ]; then
      if [ "$BESTEFFORT" = 1 ]; then
         record "$name" "BESTEFFORT-FAIL" "build failed; see $OUT/$name.build.log"
         return 0
      fi
      record "$name" "FAIL" "build failed; see $OUT/$name.build.log"
      return 1
   fi

   bad=""
   lib="$WORK/image/usr/lib"

   for so in $FORBID; do
      hits=$(find "$lib" -name "*.so*" -type f \
                -exec sh -c 'readelf -d "$1" 2>/dev/null | grep -q "\[$2\]" && echo "$1"' _ {} "$so" \;)
      [ -z "$hits" ] || bad="$bad forbidden-NEEDED:$so($hits)"
   done

   for pair in $REQUIRE_NEEDED; do
      glob="${pair%%:*}"; so="${pair##*:}"
      found=0
      for f in "$lib"/$glob; do
         [ -f "$f" ] && readelf -d "$f" 2>/dev/null | grep -q "\[$so\]" && found=1
      done
      [ $found -eq 1 ] || bad="$bad missing-NEEDED:$glob:$so"
   done

   for p in $ABSENT; do
      [ ! -e "$lib/$p" ] || bad="$bad unexpectedly-present:$p"
   done
   for p in $PRESENT; do
      ok=0; for f in "$lib"/$p; do [ -e "$f" ] && ok=1; done
      [ $ok -eq 1 ] || bad="$bad missing:$p"
   done

   if [ -n "$GREP_ABSENT" ]; then
      log=$(ls "$WORK"/temp/log.do_configure 2>/dev/null | head -n1)
      if [ -n "$log" ] && grep -q "$GREP_ABSENT" "$log"; then
         bad="$bad configure-log-shows:$GREP_ABSENT"
      fi
   fi

   if [ -z "$bad" ]; then
      record "$name" "PASS" "PACKAGECONFIG=\"$PC\""
   else
      record "$name" "FAIL" "$bad"
      return 1
   fi
}

[ "$1" = all ] && set -- $ALL
fail=0
for cfg in "$@"; do run_one "$cfg" || fail=1; done
exit $fail
