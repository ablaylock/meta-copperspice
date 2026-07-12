#!/bin/sh
# Copyright (c) 2026 Allen Blaylock
# SPDX-License-Identifier: MIT
# Collect cs-svg-repro dump results from a booted QEMU machine.
#   usage: svg-repro-collect.sh <machine> <results-root> [iterations]
# Assumes: image booted with "runqemu ... slirp" (ssh on localhost:2222)
# and built with kas gates.yml (passwordless root login).
set -u

[ $# -ge 2 ] || { echo "usage: $0 <machine> <results-root> [iterations]" >&2; exit 2; }
MACHINE="$1"; ROOT="$2"; ITER="${3:-3}"
# ssh takes the port as lowercase -p, scp as uppercase -P
COMMON="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"
SSH="ssh -p 2222 $COMMON"

$SSH root@localhost true || { echo "FAIL: ssh unreachable on localhost:2222" >&2; exit 1; }

# weston images: discover the compositor socket; sato image: X on :0
ENVSTR=$($SSH root@localhost '
  RT=$(ls -d /run/user/* 2>/dev/null | head -n1)
  WL=$(ls "$RT"/wayland-* 2>/dev/null | grep -v "\.lock$" | head -n1)
  if [ -n "$WL" ]; then
    echo "XDG_RUNTIME_DIR=$RT WAYLAND_DISPLAY=$(basename "$WL") CS_GUI_PLATFORM_NAME=wayland"
  else
    echo "DISPLAY=:0"
  fi')
echo "target env: $ENVSTR"

$SSH root@localhost \
  "rm -rf /tmp/svgdump && $ENVSTR cs-svg-repro --dump /tmp/svgdump --iterations $ITER" \
  || { echo "FAIL: cs-svg-repro --dump on target" >&2; exit 1; }

rm -rf "$ROOT/$MACHINE"
mkdir -p "$ROOT/$MACHINE"
scp -P 2222 $COMMON "root@localhost:/tmp/svgdump/*" "$ROOT/$MACHINE/" \
  || { echo "FAIL: scp" >&2; exit 1; }

GOT=$(ls "$ROOT/$MACHINE" | wc -l)
WANT=$((6 * 3 * ITER))
[ "$GOT" -eq "$WANT" ] || { echo "FAIL: expected $WANT files, got $GOT" >&2; exit 1; }
echo "PASS: $GOT files in $ROOT/$MACHINE"
