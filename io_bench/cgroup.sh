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
# cgroup.sh — cgroup v2 memory-pressure helper for Phase B.
#
# Phase B measures Cassandra under a bounded page cache: we cap the JVM's cgroup
# with memory.high so page-cache pages are reclaimed under pressure, forcing real
# device reads instead of near-100% page-cache hits. This does NOT create/start
# the cluster; it just parks the already-running JVM in a constrained cgroup.
#
# Usage:
#   ./cgroup.sh create           # create $SLICE, set memory.high=$MEM_HIGH
#   ./cgroup.sh attach <pid>     # move a running process (the Cassandra JVM) in
#   ./cgroup.sh clear            # lift the cap (memory.high=max)
#   ./cgroup.sh remove           # evict procs to root, then delete $SLICE
#   ./cgroup.sh status           # show current settings/membership
#
# Env overrides:
#   SLICE      cgroup name under /sys/fs/cgroup   (default iobench.slice)
#   MEM_HIGH   throttling threshold               (default 10G; accepts K/M/G or "max")

set -euo pipefail

SLICE="${SLICE:-iobench.slice}"
MEM_HIGH="${MEM_HIGH:-10G}"

CG_ROOT="/sys/fs/cgroup"
CG_DIR="$CG_ROOT/$SLICE"

# cgroup v2 presents itself as filesystem type "cgroup2fs" at $CG_ROOT. v1 (the
# old multi-hierarchy layout) has no unified memory.high and is not supported here.
require_v2() {
  local fstype
  fstype="$(stat -fc %T "$CG_ROOT" 2>/dev/null || echo unknown)"
  if [ "$fstype" != "cgroup2fs" ]; then
    echo "ERROR: $CG_ROOT is not cgroup v2 (found: $fstype). This helper needs unified cgroup v2." >&2
    exit 1
  fi
}

# memory.high takes a byte count or the literal "max"; it does NOT parse "10G".
to_bytes() {
  local v="$1"
  if [ "$v" = "max" ]; then echo max; return; fi
  local num="${v%[KkMmGg]}"
  local suf="${v#"$num"}"
  case "$suf" in
    G|g) echo $(( num * 1024 * 1024 * 1024 )) ;;
    M|m) echo $(( num * 1024 * 1024 )) ;;
    K|k) echo $(( num * 1024 )) ;;
    "")  echo "$num" ;;
    *)   echo "ERROR: cannot parse MEM_HIGH='$v' (use e.g. 10G, 512M, or max)" >&2; exit 1 ;;
  esac
}

cmd_create() {
  require_v2
  # For memory.high to exist in the child, the parent (root) must delegate the
  # memory controller via cgroup.subtree_control. On systemd hosts it usually
  # already lists "memory"; add it best-effort if missing.
  if ! grep -qw memory "$CG_ROOT/cgroup.subtree_control" 2>/dev/null; then
    echo "-- enabling +memory in $CG_ROOT/cgroup.subtree_control"
    echo "+memory" | sudo tee "$CG_ROOT/cgroup.subtree_control" >/dev/null || \
      echo "   (could not enable +memory at root; memory.high may be unavailable)"
  fi
  if [ -d "$CG_DIR" ]; then
    echo "-- cgroup $CG_DIR already exists"
  else
    echo "-- creating cgroup $CG_DIR"
    sudo mkdir -p "$CG_DIR"
  fi
  local bytes; bytes="$(to_bytes "$MEM_HIGH")"
  echo "-- setting memory.high=$bytes (from MEM_HIGH=$MEM_HIGH)"
  echo "$bytes" | sudo tee "$CG_DIR/memory.high" >/dev/null
  echo "created: $CG_DIR  memory.high=$(cat "$CG_DIR/memory.high" 2>/dev/null || echo '?')"
}

cmd_attach() {
  require_v2
  local pid="${1:-}"
  if [ -z "$pid" ]; then echo "usage: $0 attach <pid>" >&2; exit 1; fi
  if [ ! -d "$CG_DIR" ]; then echo "ERROR: $CG_DIR does not exist; run 'create' first" >&2; exit 1; fi
  if [ ! -d "/proc/$pid" ]; then echo "ERROR: no such process $pid" >&2; exit 1; fi
  echo "-- attaching pid $pid to $CG_DIR"
  echo "$pid" | sudo tee "$CG_DIR/cgroup.procs" >/dev/null
  echo "attached. members now:"; sudo cat "$CG_DIR/cgroup.procs" 2>/dev/null || true
}

cmd_clear() {
  require_v2
  if [ ! -d "$CG_DIR" ]; then echo "-- $CG_DIR does not exist; nothing to clear"; return; fi
  echo "-- lifting cap: memory.high=max on $CG_DIR"
  echo max | sudo tee "$CG_DIR/memory.high" >/dev/null
}

cmd_remove() {
  require_v2
  if [ ! -d "$CG_DIR" ]; then echo "-- $CG_DIR does not exist; nothing to remove"; return; fi
  # A cgroup can only be rmdir'd when empty, so migrate any remaining procs back
  # to the root cgroup first.
  if [ -s "$CG_DIR/cgroup.procs" ]; then
    echo "-- evicting remaining procs to root cgroup"
    local p
    while IFS= read -r p; do
      [ -n "$p" ] || continue
      echo "$p" | sudo tee "$CG_ROOT/cgroup.procs" >/dev/null 2>&1 || true
    done < "$CG_DIR/cgroup.procs"
  fi
  echo "-- rmdir $CG_DIR"
  sudo rmdir "$CG_DIR"
  echo "removed."
}

cmd_status() {
  require_v2
  if [ ! -d "$CG_DIR" ]; then echo "$CG_DIR: does not exist"; return; fi
  echo "cgroup:      $CG_DIR"
  echo "memory.high: $(cat "$CG_DIR/memory.high" 2>/dev/null || echo '?')"
  echo "memory.current: $(cat "$CG_DIR/memory.current" 2>/dev/null || echo '?')"
  echo "procs:"
  sudo cat "$CG_DIR/cgroup.procs" 2>/dev/null || true
}

case "${1:-}" in
  create) cmd_create ;;
  attach) shift; cmd_attach "${1:-}" ;;
  clear)  cmd_clear ;;
  remove) cmd_remove ;;
  status) cmd_status ;;
  *) echo "usage: $0 {create|attach <pid>|clear|remove|status}" >&2; exit 1 ;;
esac
