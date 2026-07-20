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
# warm_readbench.py -- steady-state read benchmark of iouring_bench.readbench via
# cassandra-easy-stress IoUringReadBench, with PAGE-CACHE HIT RATE measured two ways:
#
#   1) bcc `cachestat` (kernel mm counters) -- system-wide HITS/MISSES/HITRATIO.
#      cachetop -p <pid> was the first choice but it is curses-only and cannot run
#      headless (no tty); cachestat is line-based and works. On this dedicated box
#      Cassandra dominates page-cache traffic, so system-wide ~= the node.
#   2) per-process VFS-vs-device IOPS delta: hit ~= 1 - device_reads/vfs_preads,
#      from vfs_reads.bt (PID-filtered) and block_io.bt (p8 sector-filtered). A
#      cache-hit pread issues no block IO, so this is a per-process/per-partition
#      cross-check that avoids the byte-level 16KB-page over-fetch confound.
#
# The working set (~188GB) far exceeds RAM (62GB), so a random read workload forces
# real misses with no cgroup cap. Starts cold (one drop_caches) and warms naturally.

import os
import re
import subprocess
import sys
import time
from pathlib import Path

IO_BENCH = Path(__file__).resolve().parent
sys.path.insert(0, str(IO_BENCH))
import point_read          # noqa: E402
import orchestrate as O    # noqa: E402

KS, TBL = "iouring_bench", "readbench"
DEV_T_PARENT = (259 << 20) | 0
NATIVE_HOST, NATIVE_PORT, LOCAL_DC = "127.0.0.1", 9042, "datacenter1"
LOG = Path(os.path.expanduser("~/.ccm/iobench/node1/logs/io_operations.log"))
PIDFILE = Path(os.path.expanduser("~/.ccm/iobench/node1/cassandra.pid"))
CACHESTAT = "/usr/share/bcc/tools/cachestat"
GEN_THREADS, GEN_PARTITIONS, GEN_ID = "16", "4000000", "001"  # must match SSTableGen keys
MM_CACHE = IO_BENCH / "ebpf" / "mm_cache.bt"
P8_SDEV = (259 << 20) | 8            # XFS fs dev_t for mm_filemap tracepoints (partition, not parent disk)
RB_DIR = "/mnt/xfsdata/data/iouring_bench"


def parse_cachestat(text):
    """Sum HITS/MISSES across cachestat data lines -> aggregate hit ratio.
    Columns: HITS MISSES DIRTIES HITRATIO BUFFERS_MB CACHED_MB."""
    hits = misses = n = 0
    for ln in (text or "").splitlines():
        p = ln.split()
        if len(p) >= 4 and p[0].lstrip("-").isdigit() and p[1].lstrip("-").isdigit():
            hits += int(p[0])
            misses += int(p[1])
            n += 1
    total = hits + misses
    return {"hits": hits, "misses": misses, "samples": n,
            "hitratio": (hits / total) if total else None}


def parse_errors(text):
    """Cumulative error Count from easy-stress rows (last '|'-group's first int)."""
    last = 0
    for ln in (text or "").splitlines():
        if "|" in ln:
            tail = ln.rsplit("|", 1)[-1].split()
            if tail and tail[0].isdigit():
                last = int(tail[0])
    return last


def parse_cachestat_series(text):
    """Per-sample [{hits,misses,hitratio,cached_mb}] from cachestat lines."""
    out = []
    for ln in (text or "").splitlines():
        p = ln.split()
        if len(p) >= 6 and p[0].lstrip("-").isdigit() and p[1].lstrip("-").isdigit():
            h, m = int(p[0]), int(p[1])
            tot = h + m
            try:
                cached = float(p[5])
            except ValueError:
                cached = None
            out.append({"hits": h, "misses": m,
                        "hitratio": (100.0 * h / tot) if tot else 0.0, "cached_mb": cached})
    return out


def parse_es_series(text):
    """Per-row [{reads,p99_ms,rate}] from easy-stress data rows (Reads group)."""
    out = []
    for ln in (text or "").splitlines():
        if "|" not in ln or "Count" in ln:
            continue
        groups = ln.split("|")
        if len(groups) < 2:
            continue
        r = groups[1].split()
        if len(r) >= 3 and r[0].isdigit():
            try:
                out.append({"reads": int(r[0]), "p99_ms": float(r[1]), "rate": float(r[2])})
            except ValueError:
                continue
    return out


def parse_hdr(path):
    """Mean/p50/p99 latency (ms) from an HdrHistogram outputPercentileDistribution file."""
    mean = p50 = p99 = None
    try:
        for ln in open(path):
            s = ln.strip()
            if s.startswith("#[Mean"):
                m = re.search(r"Mean\s*=\s*([\d.]+)", s)
                if m:
                    mean = float(m.group(1))
                continue
            p = s.split()
            if len(p) >= 2:
                try:
                    val, pct = float(p[0]), float(p[1])
                except ValueError:
                    continue
                if p50 is None and pct >= 0.5:
                    p50 = val
                if p99 is None and pct >= 0.99:
                    p99 = val
    except OSError:
        pass
    return {"mean": mean, "p50": p50, "p99": p99}


def compression_ratio():
    """SSTable compression ratio (compressed/uncompressed) from nodetool tablestats."""
    r = O.run(["ccm", "node1", "nodetool", "tablestats", "%s.%s" % (KS, TBL)], quiet=True, timeout=120)
    m = re.search(r"Compression Ratio:\s*([\d.]+)", r["out"] or "")
    return float(m.group(1)) if m else None


def _bar(val, vmax, color):
    w = 0.0 if (val is None or not vmax or vmax <= 0) else max(1.0, 100.0 * val / vmax)
    return ('<div style="background:%s;height:1em;width:%.1f%%;border-radius:3px"></div>' % (color, w))


def render_html(summary, cs_series, es_series, comp_hr, out_path):
    import html as _h
    css = ("body{font-family:-apple-system,Segoe UI,Roboto,sans-serif;max-width:1000px;"
           "margin:2rem auto;padding:0 1rem;line-height:1.5}h1{border-bottom:2px solid #ccc}"
           "h2{margin-top:1.8em}table{border-collapse:collapse;width:100%;margin:1em 0;font-size:.9em}"
           "th,td{border:1px solid #ccc;padding:.4em .6em;text-align:right}th:first-child,td:first-child{text-align:left}"
           ".row{display:grid;grid-template-columns:70px 1fr 110px;gap:.6em;align-items:center;margin:.15em 0}"
           ".cap{color:#666;font-size:.9em}.note{background:#f7f7f9;border:1px solid #e0e0e0;"
           "border-radius:6px;padding:.4em 1em;font-size:.9em}.metric{border:1px solid #e0e0e0;"
           "border-radius:6px;padding:.8em 1em;margin:1em 0}")
    h = ['<!DOCTYPE html><html><head><meta charset="utf-8"><title>readbench warm read run</title>',
         '<style>%s</style></head><body>' % css,
         '<h1>readbench &mdash; steady-state read run</h1>',
         '<p class="cap">%s</p>' % _h.escape(summary["subtitle"])]

    h.append('<h2>Summary</h2><table><tr><th>metric</th><th>value</th></tr>')
    for label, val in summary["rows"]:
        h.append('<tr><td>%s</td><td>%s</td></tr>' % (_h.escape(label), _h.escape(str(val))))
    h.append('</table>')

    if comp_hr:
        h.append('<h2>Per-component page-cache hit rate</h2>'
                 '<p class="cap">Page-level, from mm_filemap tracepoints per inode '
                 '(accesses = mm_filemap_get_pages ranges, misses = mm_filemap_add_to_page_cache); '
                 'inodes joined to sstable component. In-kernel counted, so accurate at throughput.</p>')
        h.append('<table><tr><th>component</th><th>hit rate</th><th>accessed pages</th><th>miss pages</th></tr>')
        for c in ("Index.db", "Data.db", "other"):
            d = comp_hr.get(c)
            if not d:
                continue
            hr = "%.1f%%" % (d["hit_rate"] * 100) if d["hit_rate"] is not None else "n/a"
            h.append('<tr><td>%s</td><td>%s</td><td>%d</td><td>%d</td></tr>'
                     % (c, hr, d["acc_pages"], d["miss_pages"]))
        h.append('</table>')

    def chart(title, series, key, color, fmt):
        vals = [s[key] for s in series if s.get(key) is not None]
        vmax = max(vals) if vals else 0
        parts = ['<div class="metric"><b>%s</b>' % _h.escape(title)]
        for i, s in enumerate(series):
            v = s.get(key)
            parts.append('<div class="row"><span>t%d</span>%s<span>%s</span></div>'
                         % (i, _bar(v, vmax, color), (fmt % v) if v is not None else "n/a"))
        parts.append('</div>')
        return "\n".join(parts)

    h.append('<h2>Throughput &amp; latency over time (easy-stress rows)</h2>')
    h.append(chart("Reads/s (1-min rate)", es_series, "rate", "#1982c4", "%.0f"))
    h.append(chart("Read p99 latency (ms)", es_series, "p99_ms", "#ff924c", "%.1f"))
    h.append('<h2>Page-cache over time (cachestat, 2s samples)</h2>')
    h.append(chart("Hit ratio (%)", cs_series, "hitratio", "#2a9d8f", "%.1f"))
    h.append(chart("Page cache resident (MB)", cs_series, "cached_mb", "#6a4c93", "%.0f"))

    h.append('<div class="note"><b>Caveats.</b><ul>'
             '<li>Page-cache hit rate is the reliable metric here (cachestat mm counters, cross-checked by the '
             'VFS-vs-device IOPS delta). The byte-level VFS-vs-device figure is confounded by 16&nbsp;KB-page '
             'over-fetch and is not shown.</li>'
             '<li>At this throughput, per-op eBPF read counts undercount (high-frequency tracepoint loss); use the '
             'cold isolated tool (cold_readbench.py) for exact per-request IO (2 reads, 72&nbsp;KB/req). Ratios '
             '(hit rate) are robust; absolute per-op eBPF counts at scale are lower bounds.</li>'
             '<li>disk_access_mode=standard (buffered index), chunk cache off, Zstd@8 64&nbsp;KB chunks, LCS, '
             'data on raw XFS (/dev/nvme0n1p8). Working set ~188&nbsp;GB &gt; 62&nbsp;GB RAM.</li>'
             '</ul></div>')
    h.append('</body></html>')
    Path(out_path).write_text("\n".join(h), encoding="utf-8")
    print("wrote %s" % out_path)


def component_hit_rates(mm_text, sstable_dir):
    """Join per-inode page-cache accesses/misses (mm_cache.bt) to sstable components
    and return {component: {acc_pages, miss_pages, hit_rate}}. Inodes not in the
    readbench dir (system tables, commitlog, ...) fall under 'other'."""
    comp = {}
    out = O.run(["bash", "-c", "find %s -maxdepth 2 -name '*.db' -printf '%%i %%f\\n'" % sstable_dir],
                quiet=True)["out"]
    for ln in out.splitlines():
        p = ln.split()
        if len(p) == 2 and p[0].isdigit():
            fn = p[1]
            comp[int(p[0])] = fn.rsplit("-", 1)[-1] if "-" in fn else fn  # -> Data.db / Index.db / ...
    maps = O.parse_bpftrace_maps(mm_text)
    agg = {}
    for ino_s, v in maps.get("acc", {}).items():
        c = comp.get(int(ino_s), "other") if ino_s.isdigit() else "other"
        agg.setdefault(c, [0, 0])
        agg[c][0] += int(v)
    for ino_s, v in maps.get("miss", {}).items():
        c = comp.get(int(ino_s), "other") if ino_s.isdigit() else "other"
        agg.setdefault(c, [0, 0])
        agg[c][1] += int(v)
    res = {}
    for c, (a, m) in agg.items():
        res[c] = {"acc_pages": a, "miss_pages": m, "hit_rate": (1.0 - m / a) if a else None}
    return res


def _header_mapped_means(text, header_token, data_token):
    """Parse a `iostat -x` / `mpstat` style stream: rows under a header line (containing
    header_token) whose first data column equals data_token. Returns a name->mean dict over
    all samples after the first (the first sample is since-boot and is dropped)."""
    header = None
    samples = []
    for line in text.splitlines():
        toks = line.split()
        if header_token in toks:
            header = toks
        elif header and data_token in toks and len(toks) == len(header):
            samples.append(dict(zip(header, toks)))
    samples = samples[1:]  # drop since-boot first sample
    means = {}
    if samples:
        for k in samples[0]:
            vals = []
            for s in samples:
                try:
                    vals.append(float(s[k]))
                except (TypeError, ValueError):
                    pass
            if vals:
                means[k] = sum(vals) / len(vals)
    means["_samples"] = len(samples)
    return means


def parse_iostat(text):
    m = _header_mapped_means(text, "Device", "nvme0n1")
    rmb = m.get("rMB/s")
    if rmb is None and m.get("rkB/s") is not None:
        rmb = m["rkB/s"] / 1024.0
    return {"util": m.get("%util"), "r_s": m.get("r/s"), "rMB_s": rmb,
            "r_await": m.get("r_await"), "aqu": m.get("aqu-sz", m.get("avgqu-sz")),
            "rareq_sz": m.get("rareq-sz"), "samples": m.get("_samples", 0)}


def parse_mpstat(text):
    m = _header_mapped_means(text, "%idle", "all")
    idle = m.get("%idle")
    return {"busy": (100.0 - idle) if idle is not None else None,
            "iowait": m.get("%iowait"), "usr": m.get("%usr"), "sys": m.get("%sys"),
            "idle": idle, "samples": m.get("_samples", 0)}


def _bottleneck_hint(mpstat, iostat):
    util = iostat.get("util")
    busy = mpstat.get("busy")
    iowait = mpstat.get("iowait")
    if util is not None and util >= 85:
        return "DEVICE-bound (disk %util ~%.0f%%) -- this IS available throughput" % util
    if busy is not None and busy >= 85:
        return "CPU-bound (busy ~%.0f%%)" % busy
    if iowait is not None and iowait >= 25:
        return "IO-wait bound (iowait ~%.0f%%); add client concurrency for more outstanding IO" % iowait
    return ("under-utilized: neither device (%s%%util) nor CPU (%s%% busy) saturated -- raise threads/rate"
            % ("%.0f" % util if util is not None else "?", "%.0f" % busy if busy is not None else "?"))


def main():
    duration = sys.argv[1] if len(sys.argv) > 1 else "120s"
    maxrlat = sys.argv[2] if len(sys.argv) > 2 else "50"   # p99 read-latency SLO (ms); "sat"/"0"/"none" = saturate
    threads = sys.argv[3] if len(sys.argv) > 3 else "32"
    chunk_label = sys.argv[4] if len(sys.argv) > 4 else "64"   # on-disk chunk_length_in_kb (label for report/JSON)
    # Throughput mode is chosen by the 2nd arg:
    #   "<ms>"      -> adaptive --maxrlat SLO (throttles to hold p99; under-measures capacity)
    #   "sat"/"0"   -> open-loop flood (find the CPU/device ceiling; expect errors past the knee)
    #   "rNNNNN"    -> fixed open-loop offered rate NNNNN ops/s, no SLO (probe sustainable throughput)
    saturate = str(maxrlat).lower() in ("sat", "0", "none", "max")
    fixed_rate = None
    if str(maxrlat).lower().startswith("r") and str(maxrlat)[1:].isdigit():
        fixed_rate = str(maxrlat)[1:]
    rate_ceiling = fixed_rate if fixed_rate else ("3000000" if saturate else "100000")
    dur_s = O.parse_duration_seconds(duration)
    pid = int(PIDFILE.read_text().strip())
    tmp = Path("/tmp/warmrb")
    tmp.mkdir(exist_ok=True)

    cenv = O.parse_env_file("/mnt/xfsdata/cluster.env")
    ps = int(cenv.get("PART_START", "0") or 0)
    nsec = int(cenv.get("PART_SECTORS", "0") or 0)
    jar = str(O.JAR)

    print("== warm readbench read run ==  duration=%s mode=%s threads=%s (rate ceiling=%s)"
          % (duration, ("SATURATE" if saturate else ("fixed-rate=%s" % fixed_rate) if fixed_rate
                        else ("maxrlat=%sms" % maxrlat)), threads, rate_ceiling))
    print("   p8 device window sectors=[%d,%d)  jvm_pid=%d" % (ps, ps + nsec, pid))

    O.drop_caches()  # cold start; the run warms the cache naturally
    blk = O.Collector(O.BLOCK_BT, (DEV_T_PARENT, ps, nsec), tmp / "blk.out", "# block_io.bt").start()
    vfs = O.Collector(O.VFS_BT, pid, tmp / "vfs.out", "# vfs_reads.bt").start()
    blk.wait_attached(15)
    vfs.wait_attached(15)
    mm = O.Collector(MM_CACHE, P8_SDEV, tmp / "mm.out", "# mm_cache.bt").start()
    mm.wait_attached(15)
    # cachestat: 2s samples covering the window + margin; unbuffered so a late
    # terminate still leaves complete lines; let it exit by count.
    cs_count = max(3, dur_s // 2 + 3)
    cs_fh = open(tmp / "cs.out", "w")
    cs = subprocess.Popen(["sudo", "env", "PYTHONUNBUFFERED=1", CACHESTAT, "2", str(cs_count)],
                          stdout=cs_fh, stderr=subprocess.STDOUT, start_new_session=True)
    # CPU (mpstat) + device utilization (iostat -x nvme0n1): 2s samples over the window. These answer
    # "is throughput CPU- or device-bound?" -- %idle/%iowait vs device %util/aqu-sz.
    io_fh = open(tmp / "iostat.out", "w")
    iostat_p = subprocess.Popen(["iostat", "-x", "nvme0n1", "2", str(cs_count)],
                                stdout=io_fh, stderr=subprocess.STDOUT)
    mp_fh = open(tmp / "mpstat.out", "w")
    mpstat_p = subprocess.Popen(["mpstat", "2", str(cs_count)],
                                stdout=mp_fh, stderr=subprocess.STDOUT)
    time.sleep(1)

    argv = ["java", "-jar", jar, "run", "IoUringReadBench", "--host", NATIVE_HOST,
            "--keyspace", KS, "--no-schema", "-p", GEN_PARTITIONS, "-t", threads,
            "--id", GEN_ID, "--readrate", "1.0", "--rate", rate_ceiling,
            "--hdr", str(tmp / "hdr"), "-d", duration]
    if not saturate and not fixed_rate:
        argv += ["--maxrlat", maxrlat]
    print("   $ " + " ".join(argv))
    es = subprocess.run(argv, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
                        timeout=dur_s + 180)

    blk_txt = blk.stop()
    vfs_txt = vfs.stop()
    mm_txt = mm.stop()
    try:
        cs.wait(timeout=6)
    except subprocess.TimeoutExpired:
        subprocess.run(["sudo", "pkill", "-INT", "-f", CACHESTAT], check=False)
        try:
            cs.wait(timeout=5)
        except subprocess.TimeoutExpired:
            pass
    cs_fh.close()
    for p, fh in ((iostat_p, io_fh), (mpstat_p, mp_fh)):
        try:
            p.wait(timeout=8)
        except subprocess.TimeoutExpired:
            p.terminate()
        fh.close()
    iostat = parse_iostat((tmp / "iostat.out").read_text(errors="ignore"))
    mpstat = parse_mpstat((tmp / "mpstat.out").read_text(errors="ignore"))

    # Server-counted per-query IOPS under load (system_views histograms; needs io_tracking.enabled).
    # This is the "IOPS per query" metric: read_iops = data-path reads the query issued, files_touched
    # = sstable components read, bytes_decompressed = decompression amplification.
    qhist = {}
    try:
        r = O.run(["bash", str(O.METRICS_SH), KS, TBL, NATIVE_HOST, str(NATIVE_PORT), LOCAL_DC],
                  timeout=90, quiet=True, env_extra={"JAR": str(O.JAR)})
        qhist = O.parse_hist(r["out"]) or {}
    except Exception:
        qhist = {}

    block = O.parse_block_summary(blk_txt) or {}
    vfsd = O.parse_vfs_summary(vfs_txt) or {}
    cstat = parse_cachestat((tmp / "cs.out").read_text(errors="ignore"))
    comp_hr = component_hit_rates(mm_txt, RB_DIR)
    hdr = parse_hdr(str(tmp / "hdr-reads.txt"))
    cratio = compression_ratio()
    ops = point_read.parse_reads_count(es.stdout) or 0
    errors = parse_errors(es.stdout)

    dev_iops = block.get("read_completions", 0)
    dev_bytes = block.get("read_bytes", 0)
    vfs_iops = vfsd.get("pread_count", 0)
    vfs_bytes = vfsd.get("pread_bytes", 0)
    hit_iops = (1.0 - dev_iops / vfs_iops) if vfs_iops else None
    hit_bytes = (1.0 - dev_bytes / vfs_bytes) if vfs_bytes else None

    def per_op(x):
        return (x / ops) if ops else None

    print("\n=== results over %s ===" % duration)
    print("  ops served                 %d  (%.0f ops/s)" % (ops, ops / dur_s if dur_s else 0))
    print("  errors                     %d  (%.2f%% -- want ~0; else overloaded)"
          % (errors, 100.0 * errors / (ops + errors) if (ops + errors) else 0))
    print("  read latency (ms):         avg=%s p50=%s p99=%s"
          % tuple(("%.3f" % v if v is not None else "n/a") for v in (hdr["mean"], hdr["p50"], hdr["p99"])))
    print("  compression ratio:         %s  (chunk_length_in_kb=%s)"
          % ("%.4f" % cratio if cratio is not None else "n/a", chunk_label))
    print("  VFS preads                 %d  (%.2f/op, %.1f KB/op)"
          % (vfs_iops, per_op(vfs_iops) or 0, (per_op(vfs_bytes) or 0) / 1024))
    print("  device reads (p8)          %d  (%.2f/op, %.1f KB/op)"
          % (dev_iops, per_op(dev_iops) or 0, (per_op(dev_bytes) or 0) / 1024))
    print("  IOPS per query (server):   read_iops=%s  files_touched=%s  bytes_read=%s  bytes_decompressed=%s"
          % (O.hist_max(qhist, "read_iops"), O.hist_max(qhist, "files_touched"),
             O.hist_max(qhist, "bytes_read"), O.hist_max(qhist, "bytes_decompressed")))
    def _f(x, fmt="%.1f"):
        return (fmt % x) if x is not None else "n/a"
    print("  CPU (mpstat, %d cores):     busy=%s%%  iowait=%s%%  idle=%s%%  (usr=%s sys=%s)"
          % (os.cpu_count() or 0, _f(mpstat["busy"]), _f(mpstat["iowait"]), _f(mpstat["idle"]),
             _f(mpstat.get("usr")), _f(mpstat.get("sys"))))
    print("  device util (iostat nvme0n1): %%util=%s  r/s=%s  rMB/s=%s  r_await=%sms  aqu-sz=%s"
          % (_f(iostat["util"]), _f(iostat["r_s"], "%.0f"), _f(iostat["rMB_s"]),
             _f(iostat["r_await"], "%.2f"), _f(iostat["aqu"], "%.2f")))
    print("  bottleneck hint:           %s"
          % _bottleneck_hint(mpstat, iostat))
    print("  page-cache hit rate:")
    print("    cachestat (mm, sys-wide) %s  (hits=%d misses=%d over %d samples)"
          % ("%.1f%%" % (cstat["hitratio"] * 100) if cstat["hitratio"] is not None else "n/a",
             cstat["hits"], cstat["misses"], cstat["samples"]))
    print("    VFS-vs-device IOPS       %s  (per-process, per-partition)"
          % ("%.1f%%" % (hit_iops * 100) if hit_iops is not None else "n/a"))
    print("    VFS-vs-device bytes      %s  (confounded by 16KB-page over-fetch)"
          % ("%.1f%%" % (hit_bytes * 100) if hit_bytes is not None else "n/a"))
    print("  per-component page-cache hit rate (mm_filemap, page-level):")
    for c in ("Index.db", "Data.db", "other"):
        d = comp_hr.get(c)
        if d and d["hit_rate"] is not None:
            print("    %-10s %5.1f%%  (accessed=%d miss=%d pages)"
                  % (c, d["hit_rate"] * 100, d["acc_pages"], d["miss_pages"]))
    data_lines = [l for l in (es.stdout or "").splitlines() if "|" in l]
    print("\n  easy-stress last rows (Writes | Reads | Deletes | Errors):")
    for l in data_lines[-4:]:
        print("   " + l.strip())

    # ---- HTML + JSON report (same dependency-free style as the sweep report) ----
    import json as _json
    cs_series = parse_cachestat_series((tmp / "cs.out").read_text(errors="ignore"))
    es_series = parse_es_series(es.stdout)
    def _chr(c):
        d = comp_hr.get(c, {})
        return "%.1f%%" % (d["hit_rate"] * 100) if d.get("hit_rate") is not None else "n/a"

    summary = {
        "subtitle": ("iouring_bench.readbench | duration=%s maxrlat=%sms threads=%s | "
                     "disk_access_mode=standard, chunk cache off, Zstd@8 64KB, LCS, /dev/nvme0n1p8, "
                     "working set ~188GB > 62GB RAM" % (duration, maxrlat, threads)),
        "rows": [
            ("chunk_length_in_kb", chunk_label),
            ("compression ratio", "%.4f" % cratio if cratio is not None else "n/a"),
            ("read latency avg (ms)", "%.3f" % hdr["mean"] if hdr["mean"] is not None else "n/a"),
            ("read latency p50 (ms)", "%.3f" % hdr["p50"] if hdr["p50"] is not None else "n/a"),
            ("read latency p99 (ms)", "%.3f" % hdr["p99"] if hdr["p99"] is not None else "n/a"),
            ("ops served", "%d" % ops),
            ("throughput", "%.0f ops/s" % (ops / dur_s if dur_s else 0)),
            ("errors", "%d (%.2f%%)" % (errors, 100.0 * errors / (ops + errors) if (ops + errors) else 0)),
            ("page-cache hit rate (cachestat, mm)",
             "%.1f%%" % (cstat["hitratio"] * 100) if cstat["hitratio"] is not None else "n/a"),
            ("page-cache hit rate (VFS-vs-device IOPS)",
             "%.1f%%" % (hit_iops * 100) if hit_iops is not None else "n/a"),
            ("index (Index.db) page-cache hit rate", _chr("Index.db")),
            ("data (Data.db) page-cache hit rate", _chr("Data.db")),
            ("cachestat hits / misses", "%d / %d" % (cstat["hits"], cstat["misses"])),
            ("device reads/op (p8, approx)", "%.2f" % (dev_iops / ops) if ops else "n/a"),
            ("device KB/op (p8, approx)", "%.1f" % ((dev_bytes / ops) / 1024) if ops else "n/a"),
        ],
    }
    results_dir = O.REPO / "io_bench" / "results"
    results_dir.mkdir(parents=True, exist_ok=True)
    (results_dir / "warm_report.json").write_text(_json.dumps(
        {"summary": summary, "cachestat_series": cs_series, "es_series": es_series,
         "block": block, "vfs": vfsd, "ops": ops, "errors": errors,
         "hit_iops": hit_iops, "component_hit_rates": comp_hr}, indent=2))
    render_html(summary, cs_series, es_series, comp_hr, str(results_dir / "warm_report.html"))
    (results_dir / ("chunk-%s-warm.json" % chunk_label)).write_text(_json.dumps({
        "chunk_kb": int(chunk_label), "compression_ratio": cratio,
        "ops": ops, "ops_per_s": (ops / dur_s) if dur_s else None, "errors": errors,
        "lat_avg_ms": hdr["mean"], "lat_p50_ms": hdr["p50"], "lat_p99_ms": hdr["p99"],
        "hit_index": comp_hr.get("Index.db", {}).get("hit_rate"),
        "hit_data": comp_hr.get("Data.db", {}).get("hit_rate"),
        "hit_cachestat": cstat["hitratio"],
        "vfs_kb_per_op": (vfs_bytes / ops / 1024) if ops else None,
        "device_kb_per_op": (dev_bytes / ops / 1024) if ops else None,
    }, indent=2))
    print("wrote %s" % (results_dir / ("chunk-%s-warm.json" % chunk_label)))


if __name__ == "__main__":
    main()
