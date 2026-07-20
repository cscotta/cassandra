#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# setup_fs.sh — build a clean XFS-on-loopback measurement filesystem.
#
# Why: the data dir currently lives on btrfs, whose copy-on-write and data
# checksumming inject extra reads/writes and CPU that pollute an IO-efficiency
# measurement. We instead put Cassandra's data on a fresh XFS filesystem living
# in a single preallocated image file, attached through a loop device. That
# gives us:
#   * a device with a stable, known major:minor that the eBPF collectors can
#     filter on (a single loopN device carries ALL of Cassandra's file IO), and
#   * XFS's straight extent-mapped IO with no CoW/checksum amplification.
#
# Two btrfs traps we defuse here:
#   1) nocow: btrfs would otherwise CoW + checksum every write to the backing
#      image. nocow (chattr +C) MUST be set on an EMPTY file, so we set +C on the
#      DIRECTORY first and let the freshly-created image inherit it.
#   2) double page-caching: reads served from the loop device would otherwise be
#      cached BOTH in XFS's page cache and again in btrfs's page cache for the
#      backing file. We attach with --direct-io=on so the loop device reads the
#      backing file O_DIRECT — only the XFS-side page cache exists, which is what
#      we want to measure (page-cache hit vs. real device IO).
#
# Usage:
#   ./setup_fs.sh              # create/attach/mount (idempotent, safe to re-run)
#   ./setup_fs.sh --teardown   # unmount + detach the loop device
#
# Env overrides:
#   IMG_PATH   backing image file       (default /home/cscotta/io_bench_data/xfs.img)
#   IMG_SIZE   image size for fallocate  (default 120G)
#   MNT        mount point               (default /home/cscotta/io_bench_data/mnt)
#   FORCE_MKFS set to 1 to re-mkfs an image that already contains XFS (WIPES DATA)

set -euo pipefail

IMG_PATH="${IMG_PATH:-/home/cscotta/io_bench_data/xfs.img}"
IMG_SIZE="${IMG_SIZE:-120G}"
MNT="${MNT:-/home/cscotta/io_bench_data/mnt}"

IMG_DIR="$(dirname "$IMG_PATH")"
# loopdev.env lives one level ABOVE the mount (i.e. $MNT/../loopdev.env), on the
# btrfs parent, NOT on the XFS mount — so it survives --teardown/unmount and can
# be sourced by setup_cluster.sh, orchestrate.py, and the eBPF collectors.
ENV_FILE="$(dirname "$MNT")/loopdev.env"

# ---------------------------------------------------------------------------
teardown() {
  echo "== teardown =="
  if mountpoint -q "$MNT"; then
    echo "unmounting $MNT"
    sudo umount "$MNT"
  else
    echo "$MNT not mounted; nothing to unmount"
  fi
  # Detach every loop device currently backed by our image (usually one).
  local dev
  while IFS= read -r dev; do
    [ -n "$dev" ] || continue
    echo "detaching loop device $dev"
    sudo losetup -d "$dev"
  done < <(losetup -j "$IMG_PATH" 2>/dev/null | cut -d: -f1)
  echo "teardown complete (image file $IMG_PATH left in place)"
}

if [ "${1:-}" = "--teardown" ]; then
  teardown
  exit 0
fi

echo "== setup_fs =="
echo "IMG_PATH=$IMG_PATH  IMG_SIZE=$IMG_SIZE  MNT=$MNT"

# 1) Parent dir + nocow inheritance ------------------------------------------
echo "-- ensuring image directory $IMG_DIR"
mkdir -p "$IMG_DIR"

# chattr +C on the DIRECTORY so the image, created below, inherits nocow.
# (Setting +C directly on a non-empty file is ignored by btrfs.)
if lsattr -d "$IMG_DIR" 2>/dev/null | awk '{print $1}' | grep -q C; then
  echo "   nocow (+C) already set on $IMG_DIR"
else
  echo "   setting nocow (+C) on $IMG_DIR"
  chattr +C "$IMG_DIR"
fi

# 2) Preallocated backing image ---------------------------------------------
if [ -f "$IMG_PATH" ]; then
  echo "-- image $IMG_PATH already exists; skipping fallocate"
  if ! lsattr "$IMG_PATH" 2>/dev/null | awk '{print $1}' | grep -q C; then
    echo "   WARNING: existing image lacks nocow (+C). It was likely created before"
    echo "            +C was set on the directory. For clean measurement, remove it"
    echo "            and re-run:  ./setup_fs.sh --teardown && rm -f $IMG_PATH && ./setup_fs.sh"
  fi
else
  echo "-- fallocate -l $IMG_SIZE $IMG_PATH (preallocated, nocow-inherited)"
  fallocate -l "$IMG_SIZE" "$IMG_PATH"
fi

# 3) XFS filesystem ----------------------------------------------------------
FSTYPE="$(sudo blkid -o value -s TYPE "$IMG_PATH" 2>/dev/null || true)"
if [ "$FSTYPE" = "xfs" ] && [ "${FORCE_MKFS:-0}" != "1" ]; then
  echo "-- $IMG_PATH already holds XFS; skipping mkfs (set FORCE_MKFS=1 to WIPE + remake)"
else
  echo "-- mkfs.xfs -f $IMG_PATH"
  sudo mkfs.xfs -f "$IMG_PATH"
fi

# 4) Attach loop device (O_DIRECT to the backing file) -----------------------
LOOP_DEV="$(losetup -j "$IMG_PATH" 2>/dev/null | cut -d: -f1 | head -1)"
if [ -n "$LOOP_DEV" ]; then
  echo "-- image already attached at $LOOP_DEV; re-asserting direct-io=on"
  sudo losetup --direct-io=on "$LOOP_DEV"
else
  echo "-- losetup --direct-io=on --find --show $IMG_PATH"
  LOOP_DEV="$(sudo losetup --direct-io=on --find --show "$IMG_PATH")"
fi
LOOP_BASE="$(basename "$LOOP_DEV")"
echo "   loop device: $LOOP_DEV"

# 5) Mount -------------------------------------------------------------------
mkdir -p "$MNT"
if mountpoint -q "$MNT"; then
  echo "-- $MNT already mounted; skipping mount"
else
  echo "-- mount $LOOP_DEV $MNT"
  sudo mount "$LOOP_DEV" "$MNT"
fi
# XFS root is root-owned after mkfs; Cassandra runs as this (non-root) user and
# must be able to create its data/commitlog subdirs, so hand the mount to us.
echo "-- chown mount to $(id -un):$(id -gn)"
sudo chown "$(id -u)":"$(id -g)" "$MNT"

# 6) Readahead ---------------------------------------------------------------
# This kernel has 16 KB base pages (getconf PAGE_SIZE = 16384), so read_ahead_kb
# is floored at one page: writing "4" is rounded UP to 16 (one page). We set it
# low deliberately so a page-cache MISS reads ~one page, not a fat prefetch
# window that would mask the per-read IO we are trying to attribute.
echo "-- setting read_ahead_kb=4 on $LOOP_BASE (16 KB pages floor this to 16 = one page)"
echo 4 | sudo tee "/sys/block/$LOOP_BASE/queue/read_ahead_kb" >/dev/null

# 7) Discover major:minor and publish env for downstream tools ---------------
# The loop device's major:minor is exposed directly in sysfs as /sys/block/<dev>/dev.
# This is the value the eBPF collectors must match against (e.g. bio tracepoint
# dev filtering). lsblk -ndo MAJ:MIN "$LOOP_DEV" reports the same thing.
LOOP_MAJMIN="$(cat "/sys/block/$LOOP_BASE/dev")"

echo "== ready =="
echo "   LOOP_DEV    = $LOOP_DEV"
echo "   LOOP_MAJMIN = $LOOP_MAJMIN   (eBPF collectors filter on this)"
echo "   MNT         = $MNT"
lsblk -o NAME,MAJ:MIN,SIZE,FSTYPE,MOUNTPOINT "$LOOP_DEV" 2>/dev/null || true

cat > "$ENV_FILE" <<EOF
# Written by setup_fs.sh — source this from other io_bench scripts.
LOOP_DEV=$LOOP_DEV
LOOP_MAJMIN=$LOOP_MAJMIN
MNT=$MNT
IMG_PATH=$IMG_PATH
EOF
echo "   wrote $ENV_FILE"
