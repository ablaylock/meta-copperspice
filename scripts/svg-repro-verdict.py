#!/usr/bin/env python3
# Copyright (c) 2026 Allen Blaylock
# SPDX-License-Identifier: MIT
"""Score cs-svg-repro dump results.

Layout: <results-root>/<machine>/<name>.<iter>.{raw,control,drawn}.png

Per machine x svg:
  raw-stable     raw PNG pixels identical across iterations
                 (False => uninitialized memory varies run to run)
  garbage-px     pixel count where control alpha == 0 but raw differs
                 from control, iteration 1 (nonzero => visible garbage
                 outside the artwork; the reported symptom)
  control-stable control identical across iterations (False => renderer
                 nondeterminism, Outcome B territory)

Cross-arch (per svg): control iteration 1 compared between machines.
Hash-identical expected; otherwise max per-channel delta <= 2 counts as
equivalent (float noise), more flags Outcome B.

Usage:
  svg-repro-verdict.py <results-root>
  svg-repro-verdict.py --selftest
Exit codes: 0 ok, 1 incomplete dumps or selftest failure, 2 usage.
Requires Pillow.
"""

import hashlib
import sys
import tempfile
from pathlib import Path

from PIL import Image

SVGS = ["pineapple", "watermelon", "cake1", "cake2", "cup_cake", "ice_cream"]


def rgba(path):
    return Image.open(path).convert("RGBA")


def pixel_hash(img):
    return hashlib.sha256(img.tobytes()).hexdigest()


def garbage_count(raw, control):
    """Pixels outside the artwork footprint where raw differs from control."""
    if raw.size != control.size:
        return None
    rb, cb = raw.tobytes(), control.tobytes()
    count = 0
    for i in range(3, len(cb), 4):          # RGBA: alpha at byte offset 3
        if cb[i] == 0 and rb[i - 3:i + 1] != cb[i - 3:i + 1]:
            count += 1
    return count


def max_channel_delta(a, b):
    ab, bb = a.tobytes(), b.tobytes()
    if len(ab) != len(bb):
        return 255
    if ab == bb:
        return 0
    return max(abs(x - y) for x, y in zip(ab, bb))


def iterations_of(mdir, svg, kind):
    files = sorted(mdir.glob(f"{svg}.*.{kind}.png"),
                   key=lambda p: int(p.name.split(".")[1]))
    return files


def score_machine(mdir):
    rows, complete = {}, True
    for svg in SVGS:
        raws = iterations_of(mdir, svg, "raw")
        controls = iterations_of(mdir, svg, "control")
        drawns = iterations_of(mdir, svg, "drawn")
        if not raws or len(raws) != len(controls) or len(raws) != len(drawns):
            print(f"ERROR: incomplete dump for {mdir.name}/{svg} "
                  f"(raw={len(raws)} control={len(controls)} drawn={len(drawns)})",
                  file=sys.stderr)
            complete = False
            continue
        raw_hashes = [pixel_hash(rgba(p)) for p in raws]
        ctl_hashes = [pixel_hash(rgba(p)) for p in controls]
        garbage = garbage_count(rgba(raws[0]), rgba(controls[0]))
        if garbage is None:
            print(f"ERROR: size mismatch raw/control {mdir.name}/{svg}",
                  file=sys.stderr)
            complete = False
            continue
        rows[svg] = {
            "raw_stable": len(set(raw_hashes)) == 1,
            "garbage_px": garbage,
            "control_stable": len(set(ctl_hashes)) == 1,
            "control_1": controls[0],
        }
    return rows, complete


def main(root):
    machines = sorted(d for d in root.iterdir() if d.is_dir())
    if not machines:
        print(f"ERROR: no machine directories under {root}", file=sys.stderr)
        return 1
    results, all_complete = {}, True
    for mdir in machines:
        rows, complete = score_machine(mdir)
        results[mdir.name] = rows
        all_complete = all_complete and complete

    ref = machines[0].name
    print(f"| machine | svg | raw-stable | garbage-px | control-stable "
          f"| control-vs-{ref} |")
    print("|---|---|---|---|---|---|")
    for machine in results:
        for svg, r in results[machine].items():
            if machine == ref or svg not in results[ref]:
                xarch = "-"
            else:
                a, b = rgba(r["control_1"]), rgba(results[ref][svg]["control_1"])
                if pixel_hash(a) == pixel_hash(b):
                    xarch = "identical"
                else:
                    d = max_channel_delta(a, b)
                    xarch = f"delta={d} ({'ok' if d <= 2 else 'OUTCOME-B'})"
            print(f"| {machine} | {svg} | {r['raw_stable']} | {r['garbage_px']} "
                  f"| {r['control_stable']} | {xarch} |")
    return 0 if all_complete else 1


def selftest():
    with tempfile.TemporaryDirectory() as td:
        root = Path(td)

        def make(machine, noisy):
            mdir = root / machine
            mdir.mkdir()
            for svg in SVGS:
                control = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
                for x in range(8, 24):
                    for y in range(8, 24):
                        control.putpixel((x, y), (255, 0, 0, 255))
                for i in (1, 2, 3):
                    raw = control.copy()
                    if noisy:  # deterministic per-iteration noise outside footprint
                        for k in range(10):
                            raw.putpixel(((k * 3 + i) % 8, (k * 5 + i) % 8),
                                         (255, 0, 255, 255))
                    raw.save(mdir / f"{svg}.{i}.raw.png")
                    control.save(mdir / f"{svg}.{i}.control.png")
                    control.save(mdir / f"{svg}.{i}.drawn.png")

        make("clean-arch", noisy=False)
        make("dirty-arch", noisy=True)

        clean, cc = score_machine(root / "clean-arch")
        dirty, dc = score_machine(root / "dirty-arch")
        assert cc and dc, "selftest dumps scored as incomplete"
        for svg in SVGS:
            assert clean[svg]["raw_stable"] is True
            assert clean[svg]["garbage_px"] == 0
            assert clean[svg]["control_stable"] is True
            assert dirty[svg]["raw_stable"] is False, "noise not detected as unstable"
            assert dirty[svg]["garbage_px"] > 0, "garbage not counted"
            assert dirty[svg]["control_stable"] is True
        a = rgba(clean[SVGS[0]]["control_1"])
        b = rgba(dirty[SVGS[0]]["control_1"])
        assert pixel_hash(a) == pixel_hash(b)
    print("selftest PASS")
    return 0


if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] == "--selftest":
        sys.exit(selftest())
    if len(sys.argv) != 2:
        print(__doc__, file=sys.stderr)
        sys.exit(2)
    sys.exit(main(Path(sys.argv[1])))
