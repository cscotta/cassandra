#!/usr/bin/env bash
#
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
# ----------------------------------------------------------------------------
# probe_check.sh -- preflight for the io_bench eBPF collectors.
#
# Verifies that every tracepoint and bcc tool the collectors depend on exists
# on THIS kernel, and prints the exact block_rq_complete and sys_enter_pread64
# field names (they vary by kernel; block_io.bt / vfs_reads.bt are written
# against the names printed here). Exits non-zero if anything required is
# missing.  Read-only: it only lists probes and stats files.
# ----------------------------------------------------------------------------

set -u

# bpftrace must run as root; use sudo transparently when not already root.
if [ "$(id -u)" -eq 0 ]; then
    BPFTRACE="bpftrace"
else
    BPFTRACE="sudo bpftrace"
fi

BCC_DIR="${BCC_DIR:-/usr/share/bcc/tools}"

FAIL=0
pass() { printf '  PASS  %s\n' "$1"; }
fail() { printf '  FAIL  %s\n' "$1"; FAIL=1; }

echo "== io_bench eBPF preflight =="

# --- bpftrace present ------------------------------------------------------
if command -v bpftrace >/dev/null 2>&1; then
    pass "bpftrace present ($(bpftrace --version 2>/dev/null | head -1))"
else
    fail "bpftrace not found on PATH"
    echo
    echo "RESULT: FAIL (bpftrace missing; nothing else can be checked)"
    exit 1
fi

# --- BTF (needed for tracepoint arg typing) --------------------------------
if [ -r /sys/kernel/btf/vmlinux ]; then
    pass "kernel BTF present (/sys/kernel/btf/vmlinux)"
else
    fail "kernel BTF missing (/sys/kernel/btf/vmlinux) -- args-> access may fail"
fi

# --- required tracepoints --------------------------------------------------
# A tracepoint exists if `bpftrace -l <glob>` prints a matching full name.
check_tp() {
    local tp="$1"
    if $BPFTRACE -l "$tp" 2>/dev/null | grep -qx "$tp"; then
        pass "tracepoint $tp"
    else
        fail "tracepoint $tp (not found)"
    fi
}

echo "-- block tracepoints (block_io.bt) --"
$BPFTRACE -l 'tracepoint:block:block_rq_*' 2>/dev/null | sed 's/^/     /'
check_tp "tracepoint:block:block_rq_issue"
check_tp "tracepoint:block:block_rq_complete"

echo "-- syscall read tracepoints (vfs_reads.bt) --"
$BPFTRACE -l 'tracepoint:syscalls:sys_enter_pread*' 2>/dev/null | sed 's/^/     /'
check_tp "tracepoint:syscalls:sys_enter_pread64"
check_tp "tracepoint:syscalls:sys_enter_preadv"
# preadv2 is optional (older kernels lack it); warn but do not fail.
if $BPFTRACE -l 'tracepoint:syscalls:sys_enter_preadv2' 2>/dev/null \
        | grep -qx 'tracepoint:syscalls:sys_enter_preadv2'; then
    pass "tracepoint:syscalls:sys_enter_preadv2 (optional)"
else
    echo "  WARN  tracepoint:syscalls:sys_enter_preadv2 absent (optional)"
fi

# --- bcc tools for the secondary cross-checks ------------------------------
echo "-- bcc tools ($BCC_DIR) --"
for t in cachestat cachetop readahead biosnoop biopattern biolatency filetop; do
    if [ -x "$BCC_DIR/$t" ]; then
        pass "bcc tool $t"
    else
        fail "bcc tool $t (missing or not executable in $BCC_DIR)"
    fi
done

# --- print the EXACT field names on this kernel ----------------------------
# The .bt scripts are written against these names; if a run misbehaves after a
# kernel upgrade, diff this output against the field lists in the .bt headers.
echo
echo "== field names on this kernel (bake these into the .bt scripts) =="
echo "-- tracepoint:block:block_rq_complete --"
$BPFTRACE -lv 'tracepoint:block:block_rq_complete' 2>/dev/null | sed 's/^/     /'
echo "-- tracepoint:block:block_rq_issue --"
$BPFTRACE -lv 'tracepoint:block:block_rq_issue' 2>/dev/null | sed 's/^/     /'
echo "-- tracepoint:syscalls:sys_enter_pread64 --"
$BPFTRACE -lv 'tracepoint:syscalls:sys_enter_pread64' 2>/dev/null | sed 's/^/     /'
echo "-- tracepoint:syscalls:sys_enter_preadv --"
$BPFTRACE -lv 'tracepoint:syscalls:sys_enter_preadv' 2>/dev/null | sed 's/^/     /'

echo
if [ "$FAIL" -eq 0 ]; then
    echo "RESULT: PASS -- all required probes and tools present."
    exit 0
else
    echo "RESULT: FAIL -- see FAIL lines above."
    exit 1
fi
