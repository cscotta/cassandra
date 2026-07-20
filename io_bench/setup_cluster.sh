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
# setup_cluster.sh — create/configure a single-node ccm cluster for the
# IO-efficiency benchmark, using the locally built Cassandra source tree.
#
# Caches are turned OFF so that a read that isn't an OS page-cache hit becomes a
# real device read we can attribute on the data device:
#   * file_cache_enabled: false  -> chunk (buffer) cache OFF
#   * row_cache_size: 0MiB       -> row cache OFF
#   * key_cache_size: 0MiB       -> key cache OFF
# disk_access_mode: standard makes BOTH data and index buffered (positional
# pread), so index reads are pread syscalls visible to eBPF/io_operations rather
# than mmap page faults. Data + commitlog are pointed at the dedicated raw XFS
# volume (/mnt/xfsdata, its own block device) so every byte of Cassandra file IO
# lands on the one device the collectors watch.
#
# Usage:
#   ./setup_cluster.sh          # create if needed, (re)configure, (re)start (idempotent)
#
# Env overrides:
#   CLUSTER    ccm cluster name                 (default iobench)
#   DATA_DIR   base dir for data+commitlog       (default /mnt/xfsdata)
#   CHUNK_KB   compression chunk_length_in_kb    (default 64) — recorded for the
#              schema/orchestrator; it is a per-table property, not a yaml key
#   INSTALL_DIR locally built Cassandra tree     (default /home/cscotta/projects/cassandra)
#   HEAP       JVM heap (-Xms/-Xmx)              (default 8G)

set -euo pipefail

CLUSTER="${CLUSTER:-iobench}"
CHUNK_KB="${CHUNK_KB:-64}"
INSTALL_DIR="${INSTALL_DIR:-/home/cscotta/projects/cassandra}"
HEAP="${HEAP:-8G}"

# Suppress ccm's noisy pkg_resources / warning chatter, same spirit as prep_arm.sh.
NW="pkg_resources|UserWarning|import pkg|WARNING|Unsafe|high-scale|obsolescent"

# Default DATA_DIR to the dedicated raw XFS volume at /mnt/xfsdata.
if [ -z "${DATA_DIR:-}" ]; then
  DATA_DIR="/mnt/xfsdata"
fi
if [ ! -d "$DATA_DIR" ]; then
  echo "ERROR: DATA_DIR $DATA_DIR does not exist (mount the XFS volume first)." >&2
  exit 1
fi

DATA_SUBDIR="$DATA_DIR/data"
COMMITLOG_SUBDIR="$DATA_DIR/commitlog"

echo "== setup_cluster =="
echo "CLUSTER=$CLUSTER  DATA_DIR=$DATA_DIR  CHUNK_KB=$CHUNK_KB"
echo "INSTALL_DIR=$INSTALL_DIR  HEAP=$HEAP"

# Sanity: the source tree must be built — ccm --install-dir needs the artifacts,
# it does NOT download or build for us.
if ! ls "$INSTALL_DIR"/build/apache-cassandra-*.jar >/dev/null 2>&1; then
  echo "ERROR: no built jar under $INSTALL_DIR/build (apache-cassandra-*.jar)." >&2
  echo "       Build it first, e.g.:  (cd $INSTALL_DIR && ant jar)" >&2
  exit 1
fi

# Data/commitlog dirs live on the XFS mount, which setup_fs.sh chowned to us, so
# create them as this user (Cassandra will refuse to start if it can't write here).
echo "-- ensuring data dirs on XFS mount"
mkdir -p "$DATA_SUBDIR" "$COMMITLOG_SUBDIR"

# 1) Create the cluster if it does not exist --------------------------------
if [ -d "$HOME/.ccm/$CLUSTER" ]; then
  echo "-- cluster '$CLUSTER' already exists; selecting it"
  ccm switch "$CLUSTER" 2>&1 | grep -viE "$NW" || true
else
  echo "-- ccm create $CLUSTER --install-dir=$INSTALL_DIR -n 1"
  ccm create "$CLUSTER" --install-dir="$INSTALL_DIR" -n 1 2>&1 | grep -viE "$NW" || true
fi

# 2) Configure (idempotent — updateconf just rewrites each node's cassandra.yaml)
# We use -y/--yaml so we can set a list (data_file_directories) and size strings
# (0MiB) cleanly; ccm's kv parser can't express those.
echo "-- ccm updateconf (caches off, dirs -> XFS mount)"
ccm updateconf -y "$(cat <<EOF
disk_access_mode: standard
file_cache_enabled: false
row_cache_size: 0MiB
key_cache_size: 0MiB
concurrent_reads: 64
concurrent_writes: 64
commitlog_directory: $COMMITLOG_SUBDIR
data_file_directories:
  - $DATA_SUBDIR
EOF
)" 2>&1 | grep -viE "$NW" || true

# 3) (Re)start with JDK 25 native access -------------------------------------
echo "-- stopping cluster (best effort) then starting"
ccm stop 2>&1 | grep -viE "$NW" >/dev/null || true
ccm start \
  --jvm_arg="-Xms$HEAP" --jvm_arg="-Xmx$HEAP" \
  --jvm_arg="--enable-native-access=ALL-UNNAMED" \
  --wait-for-binary-proto 2>&1 | grep -viE "$NW" >/dev/null || true

# statusbinary can report "running" before the port actually accepts, so gate on
# a real TCP connect to the native port (mirrors prep_arm.sh), then settle.
echo -n "-- waiting for 127.0.0.1:9042 to accept "
for _ in $(seq 1 30); do
  if (exec 3<>/dev/tcp/127.0.0.1/9042) 2>/dev/null; then exec 3>&- 3<&-; echo "OK"; break; fi
  echo -n "."
  sleep 2
done
sleep 3

# 4) Report ------------------------------------------------------------------
echo "== ready =="
echo -n "   node1 binary: "; ccm node1 nodetool statusbinary 2>/dev/null | grep -o running || echo "NOT running"
echo "   data_file_directories = $DATA_SUBDIR"
echo "   commitlog_directory   = $COMMITLOG_SUBDIR"
# ccm assigns node1 JMX on 7100 by default; orchestrate.py toggles the
# ReadIOTracker MXBean over this port.
echo "   JMX port (node1)      = 7100   (orchestrate.py drives ReadIOTracker here)"
echo "   CHUNK_KB              = $CHUNK_KB  (apply as chunk_length_in_kb at table creation)"

# Publish cluster facts (incl. the data block device for the eBPF collectors) for
# the orchestrator. Written INTO $DATA_DIR (its parent /mnt is root-owned).
# NOTE: block_rq_* tracepoints report a PARTITION's IO under its PARENT disk's
# dev_t (verified: reads to nvme0n1p8 show up as nvme0n1 259:0). So block_io.bt
# must filter on PARENT_DEV_MAJMIN, optionally narrowed to this partition's
# sector range [PART_START, PART_START+PART_SECTORS) to exclude sibling-partition
# (e.g. /home) IO on the same physical disk.
DEV_SRC="$(findmnt -no SOURCE "$DATA_DIR" 2>/dev/null)"
DEV_BASE="$(basename "$DEV_SRC")"
DEV_MAJMIN="$(cat "/sys/class/block/$DEV_BASE/dev" 2>/dev/null)"
PARENT="$(lsblk -no PKNAME "$DEV_SRC" 2>/dev/null | head -1)"
if [ -n "$PARENT" ]; then
  PARENT_MAJMIN="$(cat "/sys/class/block/$PARENT/dev" 2>/dev/null)"
  PART_START="$(cat "/sys/class/block/$DEV_BASE/start" 2>/dev/null)"
  PART_SECTORS="$(cat "/sys/class/block/$DEV_BASE/size" 2>/dev/null)"
else
  # whole-device (e.g. a loop device): it is its own gendisk, no sector offset.
  PARENT_MAJMIN="$DEV_MAJMIN"; PART_START=0; PART_SECTORS=0
fi
CLUSTER_ENV="$DATA_DIR/cluster.env"
cat > "$CLUSTER_ENV" <<EOF
# Written by setup_cluster.sh — source this from orchestrate.py / driver scripts.
CLUSTER=$CLUSTER
DATA_DIR=$DATA_DIR
DATA_FILE_DIRECTORIES=$DATA_SUBDIR
COMMITLOG_DIRECTORY=$COMMITLOG_SUBDIR
DATA_DEVICE=$DEV_SRC
DEV_MAJMIN=$DEV_MAJMIN
PARENT_DEV_MAJMIN=$PARENT_MAJMIN
PART_START=$PART_START
PART_SECTORS=$PART_SECTORS
CHUNK_KB=$CHUNK_KB
JMX_PORT=7100
NATIVE_HOST=127.0.0.1
NATIVE_PORT=9042
EOF
echo "   data device          = $DEV_SRC ($DEV_MAJMIN); parent for eBPF = $PARENT ($PARENT_MAJMIN), sectors [$PART_START,+$PART_SECTORS)"
echo "   wrote $CLUSTER_ENV"
