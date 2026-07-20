#!/usr/bin/env python3
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
# orchestrate.py -- drive a compression-chunk-size sweep {16,64,256} KB on a
# single-node Cassandra and measure per-request read IO two ways:
#
#   Phase A (exact, cold, isolated): one read at a time, page cache dropped
#     before each, eBPF collectors bounding the single-read window. Headline
#     per-request device IOPS / bytes / files / decompression come out of the
#     block_io.bt + vfs_reads.bt SUMMARY lines and the io_operations.log delta.
#
#   Phase B (aggregate, capped page cache): easy-stress read load under a
#     memory.high cgroup, collectors over the whole window; device IO / ops.
#
# NOTE on data sources: cqlsh is unavailable on this host (its bundled driver
# hard-caps at Python <=3.13; only 3.14 is installed). ALL CQL therefore flows
# through cassandra-easy-stress (Java driver). Per-request IO is measured from
# eBPF + the server's io_operations.log; the server-side per-read histograms are
# an OPTIONAL cross-check read over JMX with jshell (tools/jmx_hist.sh).

import argparse
import json
import os
import re
import signal
import statistics
import subprocess
import sys
import time
from pathlib import Path

import point_read

# --------------------------------------------------------------------------
# Paths / constants
# --------------------------------------------------------------------------
IO_BENCH = Path(__file__).resolve().parent
REPO = IO_BENCH.parent
HOME = Path(os.path.expanduser("~"))

EBPF_DIR = IO_BENCH / "ebpf"
BLOCK_BT = EBPF_DIR / "block_io.bt"
VFS_BT = EBPF_DIR / "vfs_reads.bt"
CGROUP_SH = IO_BENCH / "cgroup.sh"
CQL_SH = IO_BENCH / "tools" / "cql.sh"          # one-shot CQL via jshell+driver
METRICS_SH = IO_BENCH / "tools" / "metrics.sh"  # system_views histograms (preferred)
JMX_SH = IO_BENCH / "tools" / "jmx_hist.sh"     # optional JMX alternative
PREADER_SRC = IO_BENCH / "tools" / "PersistentReader.java"  # warm Phase A read client

# system_views per-read histogram views (server-side cross-check for eBPF/log).
HIST_VIEWS = [
    "read_iops_per_read", "bytes_read_per_read", "files_touched_per_read",
    "bytes_decompressed_per_read", "chunk_cache_hits_per_read", "chunk_cache_misses_per_read",
]

DEFAULT_DATA_ROOT = Path("/home/cscotta/io_bench_data")
JAR = REPO / "cassandra-easy-stress" / "build" / "libs" / "cassandra-easy-stress-10-all.jar"
JAR_BUILD_HINT = "cd cassandra-easy-stress && ./gradlew shadowJar"

# easy-stress KeyValue hardcodes its table name and PK column; see
# workloads/KeyValue.kt. The --table flag is retained for labelling, but the
# EFFECTIVE table these tools read/populate is always this pair.
ES_TABLE = "keyvalue"
ES_PK = "key"
ES_RUN_ID = "001"  # easy-stress --id; PK prefix is "<id>.<thread>." per WorkloadRunner


def key_for(n):
    """A populated KeyValue partition key. Populate ran -t 1 --pg sequence --id
    <ES_RUN_ID>, so keys are "<id>.0.0" .. "<id>.0.(rows-1)"."""
    return "%s.0.%d" % (ES_RUN_ID, n)

# --------------------------------------------------------------------------
# Parsers (self-tested below against hardcoded samples)
# --------------------------------------------------------------------------
_BLOCK_RE = re.compile(
    r"SUMMARY\s+read_completions=(\d+)\s+read_bytes=(\d+)\s+"
    r"write_completions=(\d+)\s+write_bytes=(\d+)\s+other_completions=(\d+)")
_VFS_RE = re.compile(
    r"SUMMARY\s+pread_count=(\d+)\s+pread_bytes=(\d+)\s+"
    r"preadv_count=(\d+)\s+preadv2_count=(\d+)")
# bpftrace auto-dumps maps as `@name[key]: value` and scalars as `@name: value`.
_MAP_RE = re.compile(r"^@(\w+)\[(.*)\]:\s+(.+)$")
_SCALAR_RE = re.compile(r"^@(\w+):\s+(.+)$")
# io_operations.log: logback prepends "<ts> [<thread>] " before the payload.
_IOLOG_RE = re.compile(
    r"src=(?P<src>\S+)\s+op=(?P<op>\S+)\s+ks=(?P<ks>\S+)\s+tbl=(?P<tbl>\S+)\s+"
    r"comp=(?P<comp>\S+)\s+off=(?P<off>\d+)\s+len=(?P<len>\d+)\s+path=(?P<path>.+)$")
# HIST lines from metrics.sh (system_views) OR jmx_hist.sh -- same shape.
_HIST_RE = re.compile(
    r"HIST\s+name=(\S+)\s+count=(\S+)\s+max=(\S+)\s+p50=(\S+)\s+p99=(\S+)")
# CQLROW compression column carries chunk_length_in_kb=<N> among key=value pairs.
_CHUNK_RE = re.compile(r"chunk_length_in_kb=(\d+)")
# Aliases so a histogram lookup works whether names came from system_views
# (snake_case views) or JMX (CamelCase metric names).
_HIST_ALIASES = {
    "files_touched": ("files_touched_per_read", "FilesTouchedPerRead"),
    "bytes_decompressed": ("bytes_decompressed_per_read", "BytesDecompressedPerRead"),
    "bytes_read": ("bytes_read_per_read", "BytesReadPerRead"),
    "read_iops": ("read_iops_per_read", "ReadIopsPerRead"),
    "chunk_cache_hits": ("chunk_cache_hits_per_read", "ChunkCacheHitsPerRead"),
    "chunk_cache_misses": ("chunk_cache_misses_per_read", "ChunkCacheMissesPerRead"),
}


def parse_block_summary(text):
    m = _BLOCK_RE.search(text or "")
    if not m:
        return None
    return {"read_completions": int(m.group(1)), "read_bytes": int(m.group(2)),
            "write_completions": int(m.group(3)), "write_bytes": int(m.group(4)),
            "other_completions": int(m.group(5))}


def parse_vfs_summary(text):
    m = _VFS_RE.search(text or "")
    if not m:
        return None
    return {"pread_count": int(m.group(1)), "pread_bytes": int(m.group(2)),
            "preadv_count": int(m.group(3)), "preadv2_count": int(m.group(4))}


def parse_bpftrace_maps(text):
    """Return {map_name: {key: value_str}} for @map[key] dumps, plus scalars
    under key ''. Values kept as strings (hist buckets aren't numeric)."""
    maps = {}
    for ln in (text or "").splitlines():
        m = _MAP_RE.match(ln.strip())
        if m:
            maps.setdefault(m.group(1), {})[m.group(2)] = m.group(3).strip()
            continue
        m = _SCALAR_RE.match(ln.strip())
        if m:
            maps.setdefault(m.group(1), {})[""] = m.group(2).strip()
    return maps


def parse_io_operations(text, ks=None, tbl=None, sample=25):
    """Parse io_operations.log lines. If ks/tbl given, the target-table view is
    filtered to src=READ on that ks/tbl. Returns counts by source, distinct
    target paths (files touched), summed logical bytes, and a small sample."""
    by_source = {}
    target_paths = set()
    target_bytes = 0
    target_reads = 0
    by_component = {}
    lines = []
    for ln in (text or "").splitlines():
        m = _IOLOG_RE.search(ln)
        if not m:
            continue
        d = m.groupdict()
        by_source[d["src"]] = by_source.get(d["src"], 0) + 1
        is_target = (d["src"] == "READ" and
                     (ks is None or d["ks"] == ks) and
                     (tbl is None or d["tbl"] == tbl))
        if is_target:
            target_reads += 1
            target_bytes += int(d["len"])
            target_paths.add(d["path"])
            by_component[d["comp"]] = by_component.get(d["comp"], 0) + 1
        if len(lines) < sample:
            lines.append({k: (int(v) if k in ("off", "len") else v)
                          for k, v in d.items()})
    return {
        "by_source": by_source,
        "target_reads": target_reads,
        "files_touched": len(target_paths),
        "target_paths": sorted(target_paths),
        "target_bytes_logical": target_bytes,
        "by_component": by_component,
        "sample": lines,
    }


def parse_hist(text):
    """Parse HIST lines (system_views metrics.sh OR jmx_hist.sh) into
    {name: {count,max,p50,p99}} floats. Returns None if none parsed / errored."""
    if not text or "JMX_ERROR" in text or "CQL_ERROR" in text:
        return None
    out = {}
    for m in _HIST_RE.finditer(text):
        name = m.group(1)
        try:
            out[name] = {"count": float(m.group(2)), "max": float(m.group(3)),
                         "p50": float(m.group(4)), "p99": float(m.group(5))}
        except ValueError:
            continue
    return out or None


def hist_max(hist, alias):
    """Look up a histogram's max by canonical alias across snake/Camel names."""
    if not hist:
        return None
    for name in _HIST_ALIASES.get(alias, (alias,)):
        if name in hist:
            return hist[name]["max"]
    return None


def hist_stat(hist, alias, stat="max"):
    """Look up a histogram statistic (count/max/p50/p99) by canonical alias."""
    if not hist:
        return None
    for name in _HIST_ALIASES.get(alias, (alias,)):
        if name in hist:
            return hist[name].get(stat)
    return None


def parse_compression_chunk(text):
    """Extract chunk_length_in_kb from a CQLROW compression column dump."""
    m = _CHUNK_RE.search(text or "")
    return int(m.group(1)) if m else None


def dev_t_from_majmin(majmin):
    """LOOP_MAJMIN is decimal 'MAJ:MIN'; block tracepoints use (MAJOR<<20)|MINOR."""
    maj_s, min_s = majmin.strip().split(":")
    maj, mn = int(maj_s), int(min_s)
    return (maj << 20) | mn


def parse_env_file(path):
    env = {}
    p = Path(path)
    if not p.exists():
        return env
    for ln in p.read_text().splitlines():
        ln = ln.strip()
        if not ln or ln.startswith("#") or "=" not in ln:
            continue
        k, v = ln.split("=", 1)
        env[k.strip()] = v.strip()
    return env


def parse_duration_seconds(s):
    """'3m' -> 180, '90s' -> 90, '1h' -> 3600, bare number -> seconds."""
    s = str(s).strip().lower()
    total = 0
    matched = False
    for num, unit in re.findall(r"(\d+)\s*([dhms]?)", s):
        if not num:
            continue
        matched = True
        n = int(num)
        total += n * {"d": 86400, "h": 3600, "m": 60, "s": 1, "": 1}[unit]
    return total if matched else 0


# --------------------------------------------------------------------------
# Shell helpers -- every external call is wrapped and NON-FATAL.
# --------------------------------------------------------------------------
def log(msg):
    print("[%s] %s" % (time.strftime("%H:%M:%S"), msg), flush=True)


def run(argv, timeout=600, quiet=False, cwd=None, env_extra=None):
    """Run argv (list). Never raises; returns dict(rc,out,err). Logs failures."""
    if not quiet:
        log("$ " + " ".join(str(a) for a in argv))
    env = None
    if env_extra:
        env = os.environ.copy()
        env.update({k: str(v) for k, v in env_extra.items()})
    try:
        p = subprocess.run([str(a) for a in argv], stdout=subprocess.PIPE,
                           stderr=subprocess.PIPE, text=True, timeout=timeout,
                           cwd=str(cwd) if cwd else None, env=env)
        rc, out, err = p.returncode, p.stdout, p.stderr
    except subprocess.TimeoutExpired as e:
        rc = 124
        out = (e.stdout.decode() if isinstance(e.stdout, bytes) else e.stdout) or ""
        err = "TIMEOUT after %ss" % timeout
    except FileNotFoundError as e:
        rc, out, err = 127, "", str(e)
    if rc != 0 and not quiet:
        log("  -> rc=%d %s" % (rc, (err or "").strip()[:200]))
    return {"rc": rc, "out": out, "err": err}


def drop_caches():
    run(["sudo", "sh", "-c", "echo 3 > /proc/sys/vm/drop_caches"], timeout=60)


def tcp_gate(host, port, tries=30, delay=2.0):
    """Wait for a TCP accept on host:port (native proto readiness gate)."""
    import socket
    for _ in range(tries):
        try:
            with socket.create_connection((host, int(port)), timeout=2):
                return True
        except OSError:
            time.sleep(delay)
    return False


def find_jvm_pid(cluster):
    """The Cassandra JVM tgid, for cgroup attach and vfs_reads.bt's PID filter.

    Primary source: ccm's node pidfile (~/.ccm/<cluster>/node1/cassandra.pid).
    Fallback: pgrep for a java process whose cmdline names this node's dir."""
    pidfile = HOME / ".ccm" / cluster / "node1" / "cassandra.pid"
    if pidfile.exists():
        try:
            pid = int(pidfile.read_text().strip())
            if Path("/proc/%d" % pid).exists():
                return pid
        except (ValueError, OSError):
            pass
    node_dir = str(HOME / ".ccm" / cluster / "node1")
    r = run(["pgrep", "-f", "java.*%s" % node_dir], quiet=True)
    for tok in (r["out"] or "").split():
        if tok.isdigit() and Path("/proc/%s" % tok).exists():
            return int(tok)
    return None


# --------------------------------------------------------------------------
# eBPF collector control
# --------------------------------------------------------------------------
class Collector:
    """A backgrounded `sudo bpftrace -B line <script> <arg>` writing to a file.

    Stopped by SIGINT'ing the real (root) bpftrace via `sudo pkill -INT` on the
    script basename, which fires bpftrace's END block so SUMMARY is flushed."""

    def __init__(self, script, arg, outfile, marker):
        self.script = Path(script)
        # arg may be a single value or a list/tuple of bpftrace positional params
        # (e.g. block_io.bt takes DEV_T [PART_START PART_SECTORS]).
        self.args = [str(a) for a in arg] if isinstance(arg, (list, tuple)) else [str(arg)]
        self.outfile = Path(outfile)
        self.marker = marker
        self.proc = None
        self._fh = None

    def start(self):
        self._fh = open(self.outfile, "w")
        argv = ["sudo", "bpftrace", "-B", "line", str(self.script)] + self.args
        log("  start collector: %s" % " ".join(argv))
        self.proc = subprocess.Popen(argv, stdout=self._fh, stderr=subprocess.STDOUT,
                                     text=True, start_new_session=True)
        return self

    def wait_attached(self, timeout=15):
        """Poll the output file for the BEGIN marker (probes attached)."""
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            if self.proc.poll() is not None:
                return False
            try:
                if self.marker in self.outfile.read_text(errors="ignore"):
                    return True
            except OSError:
                pass
            time.sleep(0.3)
        return False

    def stop(self, timeout=20):
        if self.proc is None:
            return self._read()
        run(["sudo", "pkill", "-INT", "-f", self.script.name], quiet=True)
        try:
            self.proc.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            run(["sudo", "pkill", "-TERM", "-f", self.script.name], quiet=True)
            try:
                self.proc.wait(timeout=10)
            except subprocess.TimeoutExpired:
                pass
        if self._fh:
            self._fh.flush()
            self._fh.close()
        return self._read()

    def _read(self):
        try:
            return self.outfile.read_text(errors="ignore")
        except OSError:
            return ""


def start_collectors(cfg, tag):
    """Start block_io + vfs_reads collectors for one window; return the pair."""
    blk = Collector(BLOCK_BT, cfg["dev_t"], cfg["tmp"] / ("block_%s.out" % tag),
                    "# block_io.bt").start()
    vfs = Collector(VFS_BT, cfg["jvm_pid"], cfg["tmp"] / ("vfs_%s.out" % tag),
                    "# vfs_reads.bt").start()
    blk.wait_attached(cfg["attach_wait"])
    vfs.wait_attached(cfg["attach_wait"])
    # A brief settle so probes are live before the read/window begins.
    time.sleep(cfg["settle"])
    return blk, vfs


# --------------------------------------------------------------------------
# Server-side cross-checks over CQL (jshell + driver in the fat jar). Preferred
# over JMX for metrics; both emit the same HIST line format so parse_hist reads
# either. Non-fatal: if CQL is unreachable, eBPF + io_operations.log stand.
# --------------------------------------------------------------------------
def cql_query(cfg, cql, timeout=90):
    r = run(["bash", str(CQL_SH), cql], timeout=timeout, quiet=True,
            env_extra={"CONTACT_HOST": cfg["native_host"],
                       "CONTACT_PORT": str(cfg["native_port"]),
                       "LOCAL_DC": cfg["local_dc"]})
    return r["out"]


def read_histograms(cfg):
    src = cfg["hist_source"]
    if src == "none":
        return None
    if src == "jmx":
        if not JMX_SH.exists():
            return None
        r = run(["bash", str(JMX_SH), cfg["ks"], ES_TABLE, cfg["native_host"],
                 str(cfg["jmx_port"])], timeout=60, quiet=True)
        return parse_hist(r["out"])
    # default: system_views over CQL
    if not METRICS_SH.exists():
        return None
    r = run(["bash", str(METRICS_SH), cfg["ks"], ES_TABLE, cfg["native_host"],
             str(cfg["native_port"]), cfg["local_dc"]], timeout=90, quiet=True,
            env_extra={"JAR": str(JAR)})
    return parse_hist(r["out"])


def verify_chunk(cfg):
    """Read the live table's compression and confirm chunk_length_in_kb==target
    before measuring, so the sweep never mislabels a config."""
    cql = ("SELECT compression FROM system_schema.tables "
           "WHERE keyspace_name='%s' AND table_name='%s'" % (cfg["ks"], ES_TABLE))
    out = cql_query(cfg, cql)
    found = parse_compression_chunk(out)
    ok = (found == cfg["chunk_kb"])
    if not ok:
        log("  WARN: chunk verify: table reports chunk_length_in_kb=%s, expected %d"
            % (found, cfg["chunk_kb"]))
    else:
        log("  chunk verified: chunk_length_in_kb=%d" % found)
    return {"expected": cfg["chunk_kb"], "found": found, "ok": ok}


# --------------------------------------------------------------------------
# ccm / easy-stress wrappers
# --------------------------------------------------------------------------
def ccm_node(cluster, *args, timeout=600):
    return run(["ccm", "node1", *args], timeout=timeout)


def compression_opt(chunk_kb):
    return ("{'class':'ZstdCompressor','chunk_length_in_kb':%d,'compression_level':8}"
            % chunk_kb)


def start_cluster(cfg):
    log("== starting cluster (chunk=%d KB) ==" % cfg["chunk_kb"])
    run(["ccm", "stop"], timeout=180)
    run([
        "ccm", "start",
        "--jvm_arg=-Xms%s" % cfg["heap"],
        "--jvm_arg=-Xmx%s" % cfg["heap"],
        "--jvm_arg=-Dcassandra.io_tracking.enabled=true",
        # Rename worker threads to ReadStage-N during the task so IoOperationsLog
        # classifies src=READ (not UNKNOWN) and eBPF per-comm attribution works.
        "--jvm_arg=-Dcassandra.set_sep_thread_name=true",
        "--jvm_arg=--enable-native-access=ALL-UNNAMED",
        "--wait-for-binary-proto",
    ], timeout=300)
    ok = tcp_gate(cfg["native_host"], cfg["native_port"])
    log("  native proto 127.0.0.1:%s accepting = %s" % (cfg["native_port"], ok))
    time.sleep(3)
    cfg["jvm_pid"] = find_jvm_pid(cfg["cluster"])
    log("  cassandra JVM pid = %s" % cfg["jvm_pid"])
    return ok and cfg["jvm_pid"] is not None


def populate(cfg):
    """Write the dataset with easy-stress KeyValue (-r 0.0, sequence keys).

    --drop recreates the keyspace fresh for this config; --replication sets RF 1;
    --compression stamps the Zstd chunk size onto the table. --pg sequence with
    -t 1 yields deterministic keys <id>.0.0 .. <id>.0.(N-1)."""
    n = cfg["populate"]
    log("== populate %d rows (chunk=%d KB) ==" % (n, cfg["chunk_kb"]))
    argv = [
        "java", "-jar", str(JAR), "run", "KeyValue",
        "--host", cfg["native_host"],
        "--keyspace", cfg["ks"],
        "--drop",
        "--replication", "{'class':'SimpleStrategy','replication_factor':1}",
        "--compression", compression_opt(cfg["chunk_kb"]),
        "--id", ES_RUN_ID,
        "--pg", "sequence",
        "-t", "1",
        "-r", "0.0",
        "-n", str(n),
        "-p", str(max(n, cfg["phase_b_partitions"])),
        "--rate", str(cfg["populate_rate"]),
    ]
    r = run(argv, timeout=cfg["populate_timeout"])
    ccm_node(cfg["cluster"], "nodetool", "flush", timeout=300)
    ccm_node(cfg["cluster"], "nodetool", "disableautocompaction", cfg["ks"], ES_TABLE)
    return {"rc": r["rc"], "ok": r["rc"] == 0, "rows": n,
            "stdout_tail": (r["out"] or "")[-600:]}


def set_iolog(cfg, level):
    ccm_node(cfg["cluster"], "nodetool", "setlogginglevel", "io_operations", level,
             timeout=120)


def iolog_size(cfg):
    try:
        return cfg["iolog_path"].stat().st_size
    except OSError:
        return 0


def iolog_read_since(cfg, offset):
    """Read io_operations.log bytes appended since `offset` (this window only)."""
    try:
        with open(cfg["iolog_path"], "r", errors="ignore") as f:
            f.seek(offset)
            return f.read()
    except OSError:
        return ""


# --------------------------------------------------------------------------
# Phase A -- exact cold isolated reads
# --------------------------------------------------------------------------
def stats(values):
    vals = [v for v in values if isinstance(v, (int, float))]
    if not vals:
        return {"mean": None, "median": None, "n": 0, "min": None, "max": None}
    return {"mean": statistics.fmean(vals), "median": statistics.median(vals),
            "n": len(vals), "min": min(vals), "max": max(vals)}


def phase_a(cfg):
    log("== PHASE A: %d cold isolated reads (chunk=%d KB) ==" %
        (cfg["phase_a_reads"], cfg["chunk_kb"]))
    set_iolog(cfg, "TRACE")
    # A persistent, already-warm read client: connect ONCE so each measured window
    # contains only the single query's cold IO, not a fresh driver connect + schema
    # fetch (which previously dwarfed the query -- system.local + system_schema.*).
    reader = point_read.PersistentReader(
        str(JAR), str(PREADER_SRC), cfg["native_host"], cfg["native_port"],
        cfg["local_dc"], cfg["ks"], ES_TABLE,
        stderr_path=str(cfg["tmp"] / "preader.err"))
    if not reader.ok:
        log("  WARN: persistent read client failed to connect; skipping Phase A")
        set_iolog(cfg, "OFF")
        return {"reads": 0, "agg": {}, "per_read": [],
                "error": "persistent_reader_connect_failed"}
    # Warm the client (statement prepare + first-touch) with a key we do NOT measure.
    reader.read_key(key_for(0), timeout=cfg["point_read_timeout"])

    per_read = []
    for i in range(cfg["phase_a_reads"]):
        key = key_for(i + 1)  # measured keys follow the warm key
        drop_caches()
        off = iolog_size(cfg)
        blk, vfs = start_collectors(cfg, "a%d" % i)
        rd = reader.read_key(key, timeout=cfg["point_read_timeout"])
        time.sleep(cfg["settle"])
        blk_txt = blk.stop()
        vfs_txt = vfs.stop()
        iolog_txt = iolog_read_since(cfg, off)
        hist = read_histograms(cfg)

        block = parse_block_summary(blk_txt) or {}
        vfsd = parse_vfs_summary(vfs_txt) or {}
        iolog = parse_io_operations(iolog_txt, ks=cfg["ks"], tbl=ES_TABLE)

        dev_bytes = block.get("read_bytes", 0)
        dev_iops = block.get("read_completions", 0)
        vfs_bytes = vfsd.get("pread_bytes", 0)
        vfs_iops = vfsd.get("pread_count", 0)
        # Query-attributed (authoritative) signals, independent of the device window:
        #   - io_operations filtered to src=READ on the target ks/tbl (per-read exact)
        #   - the server's own per-request histograms (counted data-path reads/bytes)
        iolog_target_reads = iolog["target_reads"]
        iolog_target_bytes = iolog["target_bytes_logical"]
        counted_reads_db = hist_max(hist, "read_iops")
        counted_bytes_db = hist_max(hist, "bytes_read")
        files = iolog["files_touched"] or hist_max(hist, "files_touched")
        decompressed = hist_max(hist, "bytes_decompressed")
        # Device window is meaningful now that the client is warm: data-path pread(s)
        # + mmap index page-fault(s) + readahead. Over-fetch vs the VFS request.
        overfetch = (dev_bytes / vfs_bytes) if vfs_bytes else None
        pc_hit = (max(0.0, 1.0 - dev_bytes / vfs_bytes)) if vfs_bytes else None
        amp = (decompressed / vfs_bytes) if (decompressed and vfs_bytes) else None

        per_read.append({
            "read_index": i, "key": key, "point_read": rd,
            "block": block, "vfs": vfsd, "iolog_delta": iolog, "hist": hist,
            "derived": {
                # query-attributed: what it actually costs to serve the request
                "iolog_target_reads": iolog_target_reads,
                "iolog_target_bytes": iolog_target_bytes,
                "counted_reads_db": counted_reads_db,
                "counted_bytes_db": counted_bytes_db,
                "files_per_req": files,
                "decompressed_per_req": decompressed,
                # device window (warm client -> only this query's IO)
                "device_iops_per_req": dev_iops,
                "device_bytes_per_req": dev_bytes,
                "vfs_iops_per_req": vfs_iops,
                "vfs_bytes_per_req": vfs_bytes,
                "overfetch_ratio": overfetch,
                "page_cache_hit": pc_hit,
                "decompression_amplification": amp,
            },
        })
        log("  read %d/%d key=%s: device_iops=%s counted_reads=%s iolog_target=%s "
            "device_bytes=%s vfs_bytes=%s files=%s" %
            (i + 1, cfg["phase_a_reads"], key, dev_iops, counted_reads_db,
             iolog_target_reads, dev_bytes, vfs_bytes, files))
    reader.close()
    set_iolog(cfg, "OFF")

    def col(k):
        return stats([r["derived"][k] for r in per_read])

    agg = {k: col(k) for k in (
        "iolog_target_reads", "iolog_target_bytes", "counted_reads_db",
        "counted_bytes_db", "files_per_req", "decompressed_per_req",
        "device_iops_per_req", "device_bytes_per_req", "vfs_iops_per_req",
        "vfs_bytes_per_req", "overfetch_ratio", "page_cache_hit",
        "decompression_amplification")}
    return {"reads": cfg["phase_a_reads"], "agg": agg, "per_read": per_read}


# --------------------------------------------------------------------------
# Phase B -- aggregate under a capped page cache
# --------------------------------------------------------------------------
def phase_b(cfg):
    log("== PHASE B: %s read load, mem.high=%s (chunk=%d KB) ==" %
        (cfg["phase_b_duration"], cfg["mem_high"], cfg["chunk_kb"]))
    env = os.environ.copy()
    env["MEM_HIGH"] = cfg["mem_high"]
    # cgroup: create + attach the JVM so page-cache is reclaimed under pressure.
    run_env(["bash", str(CGROUP_SH), "create"], env)
    if cfg["jvm_pid"]:
        run_env(["bash", str(CGROUP_SH), "attach", str(cfg["jvm_pid"])], env)

    blk, vfs = start_collectors(cfg, "b")
    t0 = time.monotonic()
    argv = [
        "java", "-jar", str(JAR), "run", "KeyValue",
        "--host", cfg["native_host"],
        "--keyspace", cfg["ks"],
        "--no-schema",
        "-r", "1.0",
        "-d", cfg["phase_b_duration"],
        "-p", str(cfg["phase_b_partitions"]),
        "--rate", str(cfg["phase_b_rate"]),
        "--id", ES_RUN_ID,
    ]
    dur_s = parse_duration_seconds(cfg["phase_b_duration"])
    es = run(argv, timeout=dur_s + cfg["phase_b_slack"])
    wall = time.monotonic() - t0
    blk_txt = blk.stop()
    vfs_txt = vfs.stop()
    run_env(["bash", str(CGROUP_SH), "clear"], env)

    block = parse_block_summary(blk_txt) or {}
    vfsd = parse_vfs_summary(vfs_txt) or {}
    ops = point_read.parse_reads_count(es["out"]) or 0
    dev_bytes = block.get("read_bytes", 0)
    dev_iops = block.get("read_completions", 0)
    vfs_bytes = vfsd.get("pread_bytes", 0)
    ops_per_s = (ops / wall) if wall else None
    pc_hit_pct = (max(0.0, 1.0 - dev_bytes / vfs_bytes) * 100.0) if vfs_bytes else None

    return {
        "duration": cfg["phase_b_duration"],
        "wall_seconds": wall,
        "ops_served": ops,
        "ops_per_s": ops_per_s,
        "device_iops_per_op": (dev_iops / ops) if ops else None,
        "device_bytes_per_op": (dev_bytes / ops) if ops else None,
        "page_cache_hit_pct": pc_hit_pct,
        "block_summary": block,
        "vfs_summary": vfsd,
        "easy_stress_tail": (es["out"] or "")[-800:],
    }


def run_env(argv, env, timeout=180):
    log("$ " + " ".join(str(a) for a in argv))
    try:
        p = subprocess.run([str(a) for a in argv], stdout=subprocess.PIPE,
                           stderr=subprocess.STDOUT, text=True, timeout=timeout, env=env)
        if p.returncode != 0:
            log("  -> rc=%d %s" % (p.returncode, (p.stdout or "").strip()[:200]))
        return p.returncode
    except (subprocess.TimeoutExpired, FileNotFoundError) as e:
        log("  -> failed: %s" % e)
        return 1


# --------------------------------------------------------------------------
# Per-config driver
# --------------------------------------------------------------------------
def run_config(chunk_kb, args, loop_env, cluster_env):
    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)
    tmp = out_dir / ("tmp-chunk-%d" % chunk_kb)
    tmp.mkdir(parents=True, exist_ok=True)

    # populate sizing: cover the read key range for Phase B; small for A-only.
    phases = ["a", "b"] if args.phase == "both" else [args.phase]
    if args.populate > 0:
        pop = args.populate
    elif "b" in phases:
        pop = args.phase_b_partitions
    else:
        pop = max(args.phase_a_reads * 4, 1000)

    cfg = {
        "chunk_kb": chunk_kb,
        "cluster": cluster_env.get("CLUSTER", "iobench"),
        "ks": args.ks,
        "table_logical": args.table,
        "heap": args.heap,
        "mem_high": args.mem_high,
        "native_host": cluster_env.get("NATIVE_HOST", "127.0.0.1"),
        "native_port": cluster_env.get("NATIVE_PORT", "9042"),
        "jmx_port": int(cluster_env.get("JMX_PORT", "7100")),
        "dev_t": dev_t_from_majmin(loop_env["LOOP_MAJMIN"]),
        "loop_dev": loop_env.get("LOOP_DEV", ""),
        "loop_majmin": loop_env.get("LOOP_MAJMIN", ""),
        "phase_a_reads": args.phase_a_reads,
        "phase_b_duration": args.phase_b_duration,
        "phase_b_partitions": args.phase_b_partitions,
        "phase_b_rate": args.phase_b_rate,
        "phase_b_slack": args.phase_b_slack,
        "populate": pop,
        "populate_rate": args.populate_rate,
        "populate_timeout": args.populate_timeout,
        "point_read_timeout": args.point_read_timeout,
        "attach_wait": args.attach_wait,
        "settle": args.settle,
        "hist_source": args.hist_source,
        "local_dc": args.local_dc,
        "iolog_path": HOME / ".ccm" / cluster_env.get("CLUSTER", "iobench") /
                      "node1" / "logs" / "io_operations.log",
        "tmp": tmp,
    }

    result = {
        "chunk_kb": chunk_kb,
        "ks": cfg["ks"],
        "table_logical": args.table,
        "table_effective": ES_TABLE,
        "pk_col": ES_PK,
        "dev_t": cfg["dev_t"],
        "loop_dev": cfg["loop_dev"],
        "loop_majmin": cfg["loop_majmin"],
        "heap": cfg["heap"],
        "mem_high": cfg["mem_high"],
        "populate_rows": pop,
        "compression": compression_opt(chunk_kb),
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S"),
        "phase_a": None,
        "phase_b": None,
    }

    if not JAR.exists():
        log("ERROR: easy-stress jar missing at %s" % JAR)
        log("  build it first:  %s" % JAR_BUILD_HINT)
        result["error"] = "jar_missing"
        _write_config(out_dir, chunk_kb, result)
        return result

    if not start_cluster(cfg):
        log("WARN: cluster did not come up cleanly for chunk=%d; continuing" % chunk_kb)
    result["jvm_pid"] = cfg.get("jvm_pid")

    pop_res = populate(cfg)
    result["populate"] = pop_res

    # Confirm the live table actually has the target chunk size before measuring.
    result["chunk_verify"] = verify_chunk(cfg)

    if "a" in phases:
        try:
            result["phase_a"] = phase_a(cfg)
        except Exception as e:  # keep the sweep alive
            log("WARN: phase A failed for chunk=%d: %r" % (chunk_kb, e))
            result["phase_a_error"] = repr(e)
    if "b" in phases:
        try:
            result["phase_b"] = phase_b(cfg)
        except Exception as e:
            log("WARN: phase B failed for chunk=%d: %r" % (chunk_kb, e))
            result["phase_b_error"] = repr(e)

    _write_config(out_dir, chunk_kb, result)
    return result


def _write_config(out_dir, chunk_kb, result):
    path = out_dir / ("chunk-%d.json" % chunk_kb)
    path.write_text(json.dumps(result, indent=2, default=str))
    log("wrote %s" % path)


# --------------------------------------------------------------------------
# Self-test of the parsers (no cluster needed)
# --------------------------------------------------------------------------
def self_test():
    b = parse_block_summary(
        "# block_io.bt dev_t=7340032 major=7 minor=0\n"
        "SUMMARY read_completions=12 read_bytes=196608 write_completions=0 "
        "write_bytes=0 other_completions=1\n")
    assert b == {"read_completions": 12, "read_bytes": 196608,
                 "write_completions": 0, "write_bytes": 0,
                 "other_completions": 1}, b

    v = parse_vfs_summary(
        "SUMMARY pread_count=6 pread_bytes=98304 preadv_count=0 preadv2_count=0\n")
    assert v == {"pread_count": 6, "pread_bytes": 98304,
                 "preadv_count": 0, "preadv2_count": 0}, v

    maps = parse_bpftrace_maps(
        "@rd_issue_bytes_by_comm[ReadStage-1]: 65536\n"
        "@pread_bytes_by_fd[123]: 16384\n"
        "@read_completions: 12\n")
    assert maps["rd_issue_bytes_by_comm"]["ReadStage-1"] == "65536", maps
    assert maps["pread_bytes_by_fd"]["123"] == "16384", maps
    assert maps["read_completions"][""] == "12", maps

    iolog_line = (
        "2026-07-18T10:30:00,123 [ReadStage-1] src=READ op=r ks=iobench_ks "
        "tbl=keyvalue comp=Data.db off=12345 len=678 "
        "path=/mnt/data/iobench_ks/keyvalue-abc/nb-1-big-Data.db")
    io = parse_io_operations(iolog_line + "\n", ks="iobench_ks", tbl="keyvalue")
    assert io["target_reads"] == 1, io
    assert io["files_touched"] == 1, io
    assert io["target_bytes_logical"] == 678, io
    assert io["by_source"]["READ"] == 1, io
    assert io["by_component"]["Data.db"] == 1, io
    # a non-target read (different ks) must not count toward the target totals
    io2 = parse_io_operations(
        iolog_line.replace("ks=iobench_ks", "ks=system") + "\n",
        ks="iobench_ks", tbl="keyvalue")
    assert io2["target_reads"] == 0 and io2["by_source"]["READ"] == 1, io2

    hist = parse_hist(
        "HIST name=bytes_read_per_read count=1 max=16384.0 p50=16384.0 p99=16384.0\n"
        "HIST name=files_touched_per_read count=1 max=2.0 p50=2.0 p99=2.0\n"
        "HIST name=bytes_decompressed_per_read count=1 max=65536.0 p50=65536.0 p99=65536.0\n")
    assert hist["bytes_read_per_read"]["max"] == 16384.0, hist
    assert hist_max(hist, "files_touched") == 2.0, hist
    assert hist_max(hist, "bytes_decompressed") == 65536.0, hist
    # JMX CamelCase names resolve through the same aliases.
    jmxh = parse_hist("HIST name=FilesTouchedPerRead count=1 max=3.0 p50=3.0 p99=3.0\n")
    assert hist_max(jmxh, "files_touched") == 3.0, jmxh
    assert parse_hist("JMX_ERROR ConnectException: refused") is None
    assert parse_hist("CQL_ERROR NoNodeAvailableException: none") is None

    comp = ("CQLROW compression={chunk_length_in_kb=64, "
            "class=org.apache.cassandra.io.compress.ZstdCompressor, compression_level=8}")
    assert parse_compression_chunk(comp) == 64, parse_compression_chunk(comp)
    assert parse_compression_chunk("CQLOK norows") is None

    assert dev_t_from_majmin("7:0") == 7340032, dev_t_from_majmin("7:0")
    assert dev_t_from_majmin("259:3") == (259 << 20) | 3
    assert parse_duration_seconds("3m") == 180
    assert parse_duration_seconds("1h") == 3600
    assert parse_duration_seconds("90s") == 90
    assert parse_duration_seconds("300") == 300

    st = stats([2, 4, 6, None])
    assert st["mean"] == 4 and st["median"] == 4 and st["n"] == 3, st
    print("orchestrate self-test OK")


# --------------------------------------------------------------------------
# CLI
# --------------------------------------------------------------------------
def build_parser():
    ap = argparse.ArgumentParser(
        description="Compression-chunk-size IO-efficiency sweep for Cassandra.")
    ap.add_argument("--chunks", default="16,64,256",
                    help="comma-separated chunk_length_in_kb sweep (default 16,64,256)")
    ap.add_argument("--phase", choices=["a", "b", "both"], default="both")
    ap.add_argument("--only-chunk", type=int, default=None,
                    help="run just this one chunk size (smoke test)")
    ap.add_argument("--phase-a-reads", type=int, default=50)
    ap.add_argument("--phase-b-duration", default="3m")
    ap.add_argument("--phase-b-partitions", type=int, default=20000000)
    ap.add_argument("--phase-b-rate", type=int, default=20000,
                    help="offered ops/s for Phase B easy-stress load")
    ap.add_argument("--phase-b-slack", type=int, default=300,
                    help="extra seconds allowed over the nominal duration")
    ap.add_argument("--mem-high", default="10G")
    ap.add_argument("--ks", default="iobench_ks")
    ap.add_argument("--table", default="kv",
                    help="logical/report label; effective table is always "
                         "easy-stress's 'keyvalue' (see notes)")
    ap.add_argument("--heap", default="8G")
    ap.add_argument("--populate", type=int, default=0,
                    help="rows to write before measuring (0 = auto by phase)")
    ap.add_argument("--populate-rate", type=int, default=50000)
    ap.add_argument("--populate-timeout", type=int, default=7200)
    ap.add_argument("--point-read-timeout", type=int, default=180)
    ap.add_argument("--attach-wait", type=float, default=15.0,
                    help="seconds to wait for bpftrace probes to attach")
    ap.add_argument("--settle", type=float, default=1.0,
                    help="seconds of settle around each collector window")
    ap.add_argument("--hist-source", choices=["cql", "jmx", "none"], default="cql",
                    help="server-side per-read histogram cross-check source "
                         "(cql=system_views via jshell, preferred; jmx; none)")
    ap.add_argument("--local-dc", default="datacenter1",
                    help="driver local datacenter (SimpleSnitch => datacenter1)")
    ap.add_argument("--data-root", default=str(DEFAULT_DATA_ROOT),
                    help="dir holding loopdev.env + cluster.env")
    ap.add_argument("--out", default=str(IO_BENCH / "results"))
    ap.add_argument("--self-test", action="store_true",
                    help="run parser self-tests and exit (no cluster needed)")
    return ap


def main():
    args = build_parser().parse_args()
    if args.self_test:
        self_test()
        return 0

    data_root = Path(args.data_root)
    loop_env = parse_env_file(data_root / "loopdev.env")
    cluster_env = parse_env_file(data_root / "cluster.env")
    if "LOOP_MAJMIN" not in loop_env:
        log("ERROR: LOOP_MAJMIN not found in %s/loopdev.env; run setup_fs.sh first"
            % data_root)
        return 2

    if args.only_chunk is not None:
        chunks = [args.only_chunk]
    else:
        chunks = [int(c) for c in args.chunks.split(",") if c.strip()]

    log("sweep chunks=%s phase=%s out=%s dev_t=%d (%s)" %
        (chunks, args.phase, args.out, dev_t_from_majmin(loop_env["LOOP_MAJMIN"]),
         loop_env.get("LOOP_DEV", "?")))

    for chunk in chunks:
        try:
            run_config(chunk, args, loop_env, cluster_env)
        except Exception as e:  # a whole-config crash must not kill the sweep
            log("ERROR: config chunk=%d crashed: %r" % (chunk, e))
    log("sweep complete. render report with:  python3 %s --out %s" %
        (IO_BENCH / "analyze.py", args.out))
    return 0


if __name__ == "__main__":
    sys.exit(main())
