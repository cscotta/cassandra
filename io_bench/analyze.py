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
# analyze.py -- read every <out>/chunk-*.json produced by orchestrate.py and
# emit a dependency-free HTML report (plain HTML + inline CSS bar cells, no
# third-party libraries -- mirrors io_uring_bench/analyze.py) plus a plaintext
# summary. One row per compression chunk size.

import argparse
import glob
import html
import json
import os

# (title, agg_key, fmt, color, lower_is_better)
# Phase A metrics read the mean of the per-read aggregate; Phase B reads scalars.
# The headline read counts are query-attributed (the DB's own per-request counter
# and io_operations filtered to the target table); the device count is the loop
# device total for the (warm-client) window, which also captures the mmap index.
METRICS_A = [
    ("Counted reads / request (DB, data path)", "counted_reads_db", "{:,.2f}", "#6a4c93", True),
    ("io_operations reads / request (target table)", "iolog_target_reads", "{:,.2f}", "#4c6a93", True),
    ("Device reads / request (data + mmap index + readahead)", "device_iops_per_req", "{:,.1f}", "#e76f51", True),
    ("Bytes read / request (VFS pread)", "vfs_bytes_per_req", "{:,.0f}", "#1982c4", True),
    ("Device bytes / request", "device_bytes_per_req", "{:,.0f}", "#e76f51", True),
    ("Files touched / request", "files_per_req", "{:,.2f}", "#2a9d8f", True),
    ("Bytes decompressed / request", "decompressed_per_req", "{:,.0f}", "#8ab17d", True),
    ("Decompression amplification (decompressed/bytes_read)", "decompression_amplification", "{:,.2f}", "#ff924c", True),
    ("Readahead over-fetch (device/VFS)", "overfetch_ratio", "{:,.2f}", "#ff595e", True),
]
METRICS_B = [
    ("Device IOPS / op", "device_iops_per_op", "{:,.3f}", "#6a4c93", True),
    ("Device bytes / op", "device_bytes_per_op", "{:,.0f}", "#e76f51", True),
    ("Page-cache hit %", "page_cache_hit_pct", "{:,.1f}", "#2a9d8f", False),
    ("Throughput (ops/s)", "ops_per_s", "{:,.0f}", "#264653", False),
]


def load(out_dir):
    rows = []
    for path in sorted(glob.glob(os.path.join(out_dir, "chunk-*.json"))):
        try:
            with open(path, encoding="utf-8") as f:
                rows.append(json.load(f))
        except (OSError, ValueError) as e:
            print("skip %s: %s" % (path, e))
    rows.sort(key=lambda d: d.get("chunk_kb", 0))
    return rows


def agg_med(d, key):
    # Median across the per-read window is robust to a window that happens to
    # catch unrelated background device IO (flush/gossip); the query-attributed
    # counted/io_operations reads are noise-free regardless.
    p = d.get("phase_a") or {}
    cell = (p.get("agg") or {}).get(key) or {}
    return cell.get("median")


def pb(d, key):
    return (d.get("phase_b") or {}).get(key)


def fmt_cell(fmt, val):
    return "n/a" if val is None else fmt.format(val)


def bar(val, vmax, color):
    if val is None or vmax is None or vmax <= 0:
        w = 0.0
    else:
        w = max(1.0, 100.0 * val / vmax)
    return ('<div style="background:%s;height:1.1em;width:%.1f%%;'
            'border-radius:3px"></div>' % (color, w))


def label(d):
    return "%s KB" % d.get("chunk_kb", "?")


# --------------------------------------------------------------------------
# Plaintext report
# --------------------------------------------------------------------------
def render_txt(rows):
    lines = []
    lines.append("Cassandra compression-chunk IO-efficiency sweep")
    lines.append("=" * 60)
    chunks = [label(d) for d in rows]
    lines.append("configs: " + ", ".join(chunks) if chunks else "no data")
    lines.append("")
    lines.append("PHASE A (cold, isolated single reads; lower is better):")
    for title, key, fmt, _c, _lo in METRICS_A:
        vals = [fmt_cell(fmt, agg_med(d, key)) for d in rows]
        lines.append("  %-52s %s" % (title, "  ".join("%14s" % v for v in vals)))
    lines.append("")
    lines.append("PHASE B (aggregate under capped page cache):")
    for title, key, fmt, _c, _lo in METRICS_B:
        vals = [fmt_cell(fmt, pb(d, key)) for d in rows]
        lines.append("  %-52s %s" % (title, "  ".join("%14s" % v for v in vals)))
    lines.append("")
    lines.append("header: " + "  ".join("%14s" % c for c in chunks))
    return "\n".join(lines) + "\n"


# --------------------------------------------------------------------------
# HTML report
# --------------------------------------------------------------------------
HEADER_NOTE = """
<p class="cap">One node, XFS on a loop device, caches off
(<code>file_cache_enabled:false</code>, row/key cache 0). Zstd@8, RF&nbsp;1.
Headline per-request numbers come from eBPF (<code>block_io.bt</code> at the loop
device, <code>vfs_reads.bt</code> at the JVM's <code>pread</code>s) and the
server's <code>io_operations.log</code>; the <code>system_views</code> per-read
histograms are a CQL cross-check (jshell + driver). Lower is better where
noted&nbsp;(&#9660;).</p>
<div class="note"><b>Measurement caveats baked into these numbers</b>
<ul>
<li><b>16&nbsp;KB page floor.</b> This kernel has 16&nbsp;KB base pages, so
<code>read_ahead_kb</code> is floored at one page (16&nbsp;KB). A cold miss reads
at least one page even when the compressed chunk is smaller, so a 16&nbsp;KB
chunk config cannot go below a one-page fetch.</li>
<li><b>Chunk (buffer) cache disabled.</b> Every logical sstable read issues a
<code>pread</code>, so VFS read IOPS equal logical reads with no in-JVM caching
in the way.</li>
<li><b>mmap'd index is invisible to VFS/metrics under <code>disk_access_mode:
auto</code>.</b> Index-file access via mmap generates page faults, not
<code>pread</code> syscalls, so it does not appear in <code>vfs_reads.bt</code>
or the pread-based metrics; only the loop-device eBPF sees that IO.</li>
<li><b>Attribution point is the loop device.</b> Device bytes/IOPS are counted at
the single loop device backing the XFS mount (<code>block_rq_complete</code>),
which is exactly the set of Cassandra's file IO and nothing else.</li>
<li><b>cqlsh unavailable.</b> All CQL (schema, reads, metrics) runs through the
DataStax driver in the easy-stress jar. Phase A drives a <b>persistent, warm read
client</b> (<code>tools/PersistentReader.java</code>) that connects once, so each
measured window holds only the single query's cold IO &mdash; not a fresh driver
connect and <code>system_schema</code> fetch.</li>
</ul></div>
"""

FINDINGS = """
<h2>How to read this</h2>
<ul>
<li><b>Three read counts, on purpose.</b> <i>Counted reads (DB)</i> is Cassandra's
own per-request physical-read counter &mdash; the data-path reads it issued (index
is mmap under <code>auto</code>, so it is not counted here). <i>io_operations reads
(target table)</i> is the same thing seen from the log, filtered to the queried
table. <i>Device reads</i> is every loop-device read completion in the window,
which additionally includes the mmap <code>Index.db</code> page-fault(s) and any
readahead. For a narrow-partition point read the first two are ~1; the device
count is ~2 (data + index).</li>
<li><b>IOPS/request and bytes/request are the headline.</b> Smaller compression
chunks should cut bytes/request (less over-read per lookup) but can raise
IOPS/request and files touched; larger chunks amortize IOPS but inflate bytes
and decompression work. The sweep makes that trade-off explicit.</li>
<li><b>Over-fetch ratio</b> = device bytes &divide; VFS bytes over the same
window. &gt;1 means readahead/alignment pulled more off the device than the JVM
asked for; with the one-page readahead floor this is bounded but nonzero for
sub-page chunks.</li>
<li><b>Decompression amplification</b> = bytes decompressed &divide; bytes read.
It rises with chunk size because a point lookup must inflate a whole chunk to
return one row.</li>
<li><b>Phase B page-cache hit %</b> is derived from the VFS-vs-device byte delta
under the <code>memory.high</code> cap; it shows how much of the working set the
capped cache still absorbed at load.</li>
</ul>
"""


def render_html(rows, out_path):
    h = ['<!DOCTYPE html><html><head><meta charset="utf-8">',
         '<title>Cassandra compression-chunk IO-efficiency</title>',
         '<style>body{font-family:-apple-system,Segoe UI,Roboto,sans-serif;'
         'max-width:1100px;margin:2rem auto;padding:0 1rem;line-height:1.5}',
         'h1{border-bottom:2px solid #ccc}h2{margin-top:1.8em}',
         'table{border-collapse:collapse;width:100%;margin:1em 0;font-size:.9em}',
         'th,td{border:1px solid #ccc;padding:.4em .6em;text-align:right}',
         'th:first-child,td:first-child{text-align:left}',
         '.mgrid{display:grid;grid-template-columns:1fr;gap:1.4em}',
         '.metric{border:1px solid #e0e0e0;border-radius:6px;padding:.8em 1em}',
         '.row{display:grid;grid-template-columns:180px 1fr 130px;gap:.6em;'
         'align-items:center;margin:.25em 0}',
         '.lo{color:#2a7}.cap{color:#666;font-size:.9em}',
         '.note{background:#f7f7f9;border:1px solid #e0e0e0;border-radius:6px;'
         'padding:.4em 1em;font-size:.9em}</style></head><body>',
         '<h1>Cassandra compression-chunk-size IO-efficiency sweep</h1>',
         HEADER_NOTE]

    if not rows:
        h.append('<p><b>No chunk-*.json found.</b> Run orchestrate.py first.</p></body></html>')
        _write(out_path, "\n".join(h))
        return

    chunk_hdr = "".join("<th>%s</th>" % html.escape(label(d)) for d in rows)

    # Config / verification strip.
    h.append("<h2>Configs</h2><table><tr><th>property</th>%s</tr>" % chunk_hdr)
    def cfg_row(title, fn):
        return ("<tr><td>%s</td>%s</tr>" %
                (title, "".join("<td>%s</td>" % html.escape(str(fn(d))) for d in rows)))
    h.append(cfg_row("chunk_length_in_kb (verified)",
                     lambda d: (d.get("chunk_verify") or {}).get("found", "?")))
    h.append(cfg_row("populate rows", lambda d: d.get("populate_rows", "?")))
    h.append(cfg_row("loop device", lambda d: d.get("loop_dev", "?")))
    h.append(cfg_row("effective table", lambda d: "%s.%s" %
                     (d.get("ks", "?"), d.get("table_effective", "?"))))
    h.append("</table>")

    # Phase A + B summary tables.
    h.append("<h2>Phase A &mdash; cold isolated per-request IO</h2>")
    h.append(_summary_table(rows, METRICS_A, agg_med, chunk_hdr))
    h.append("<h2>Phase B &mdash; aggregate under capped page cache</h2>")
    h.append(_summary_table(rows, METRICS_B, pb, chunk_hdr))

    # Charts.
    h.append('<h2>Charts</h2><div class="mgrid">')
    for title, key, fmt, color, lo in METRICS_A:
        h.append(_chart(rows, title, lambda d, k=key: agg_med(d, k), fmt, color, lo, "A"))
    for title, key, fmt, color, lo in METRICS_B:
        h.append(_chart(rows, title, lambda d, k=key: pb(d, k), fmt, color, lo, "B"))
    h.append('</div>')

    h.append(FINDINGS)
    h.append('</body></html>')
    _write(out_path, "\n".join(h))


def _summary_table(rows, metrics, getter, chunk_hdr):
    out = ['<table><tr><th>metric</th>%s</tr>' % chunk_hdr]
    for title, key, fmt, _c, lo in metrics:
        arrow = " &#9660;" if lo else ""
        cells = "".join("<td>%s</td>" % html.escape(fmt_cell(fmt, getter(d, key)))
                        for d in rows)
        out.append("<tr><td>%s%s</td>%s</tr>" % (html.escape(title), arrow, cells))
    out.append("</table>")
    return "\n".join(out)


def _chart(rows, title, getval, fmt, color, lo, phase):
    vals = [getval(d) for d in rows]
    present = [v for v in vals if v is not None]
    vmax = max(present) if present else 0
    tag = "lower is better" if lo else "higher is better"
    parts = ['<div class="metric"><b>[%s] %s</b> <span class=lo>(%s)</span>'
             % (phase, html.escape(title), tag)]
    for d, v in zip(rows, vals):
        parts.append('<div class="row"><span>%s</span>%s<span>%s</span></div>'
                     % (html.escape(label(d)), bar(v, vmax, color), fmt_cell(fmt, v)))
    parts.append('</div>')
    return "\n".join(parts)


def _write(path, text):
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)
    print("wrote %s" % path)


def main():
    ap = argparse.ArgumentParser(description="Render the chunk-size sweep report.")
    ap.add_argument("--out", default=os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                                  "results"),
                    help="dir holding chunk-*.json (report.html/.txt written here)")
    args = ap.parse_args()
    rows = load(args.out)
    render_html(rows, os.path.join(args.out, "report.html"))
    _write(os.path.join(args.out, "report.txt"), render_txt(rows))
    print(render_txt(rows))


if __name__ == "__main__":
    main()
