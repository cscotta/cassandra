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
# analyze_chunk_sweep.py -- render a dependency-free HTML comparison across the
# compression-chunk-size sweep (reads io_bench/results/chunk-*-warm.json produced
# by warm_readbench.py). One column per chunk size; rows are the metrics.

import argparse
import glob
import html
import json
import os

# (label, json_key, fmt, scale, lower_is_better)
ROWS = [
    ("Compression ratio", "compression_ratio", "{:.4f}", 1, None),
    ("Read latency avg (ms)", "lat_avg_ms", "{:.3f}", 1, True),
    ("Read latency p50 (ms)", "lat_p50_ms", "{:.3f}", 1, True),
    ("Read latency p99 (ms)", "lat_p99_ms", "{:.3f}", 1, True),
    ("Throughput (ops/s)", "ops_per_s", "{:,.0f}", 1, False),
    ("Errors", "errors", "{:,.0f}", 1, True),
    ("Index.db hit rate (%)", "hit_index", "{:.1f}", 100, False),
    ("Data.db hit rate (%)", "hit_data", "{:.1f}", 100, False),
    ("cachestat hit rate (%)", "hit_cachestat", "{:.1f}", 100, False),
    ("VFS KB / op", "vfs_kb_per_op", "{:.1f}", 1, True),
    ("Device KB / op", "device_kb_per_op", "{:.1f}", 1, True),
]


def load(out_dir):
    rows = []
    for p in glob.glob(os.path.join(out_dir, "chunk-*-warm.json")):
        try:
            rows.append(json.load(open(p, encoding="utf-8")))
        except (OSError, ValueError) as e:
            print("skip %s: %s" % (p, e))
    rows.sort(key=lambda d: d.get("chunk_kb", 0))
    return rows


def cell(d, key, fmt, scale):
    v = d.get(key)
    if v is None:
        return "n/a"
    try:
        return fmt.format(v * scale)
    except (ValueError, TypeError):
        return str(v)


def render(cfgs, out_path):
    css = ("body{font-family:-apple-system,Segoe UI,Roboto,sans-serif;max-width:900px;margin:2rem auto;"
           "padding:0 1rem;line-height:1.5}h1{border-bottom:2px solid #ccc}"
           "table{border-collapse:collapse;width:100%;margin:1em 0}th,td{border:1px solid #ccc;"
           "padding:.45em .7em;text-align:right}th:first-child,td:first-child{text-align:left}"
           "tr:nth-child(even){background:#fafafa}.cap{color:#666;font-size:.9em}"
           ".note{background:#f7f7f9;border:1px solid #e0e0e0;border-radius:6px;padding:.4em 1em;font-size:.9em}")
    h = ['<!DOCTYPE html><html><head><meta charset="utf-8"><title>readbench chunk-size sweep</title>',
         '<style>%s</style></head><body>' % css,
         '<h1>readbench &mdash; compression chunk-size sweep</h1>',
         '<p class="cap">Point-read (IoUringReadBench) over iouring_bench.readbench (~188&nbsp;GB, 64M rows) on '
         'raw XFS /dev/nvme0n1p8. disk_access_mode=standard (buffered index), chunk cache off, Zstd@8, LCS. '
         'Latency-bounded adaptive load (--maxrlat). Page-aligned chunk sizes on a 16&nbsp;KB-page kernel: '
         '16&nbsp;KB=1 page, 32&nbsp;KB=2, 64&nbsp;KB=4.</p>']
    if not cfgs:
        h.append('<p><b>No chunk-*-warm.json found.</b></p></body></html>')
        open(out_path, "w").write("\n".join(h))
        print("wrote %s (empty)" % out_path)
        return
    hdr = "".join("<th>%d KB</th>" % d.get("chunk_kb", 0) for d in cfgs)
    h.append('<table><tr><th>metric</th>%s</tr>' % hdr)
    for label, key, fmt, scale, lo in ROWS:
        arrow = " &#9660;" if lo is True else (" &#9650;" if lo is False else "")
        cells = "".join("<td>%s</td>" % html.escape(cell(d, key, fmt, scale)) for d in cfgs)
        h.append("<tr><td>%s%s</td>%s</tr>" % (html.escape(label), arrow, cells))
    h.append("</table>")
    h.append('<div class="note">&#9660; lower is better, &#9650; higher is better. '
             'Compression ratio = compressed/uncompressed on-disk (readbench values are incompressible random '
             'bytes, so ratio stays near 1; smaller chunks add slightly more per-chunk framing overhead). '
             'The IO win of smaller chunks is fewer bytes read per point lookup (VFS/Device KB/op), since a '
             'point read fetches one whole chunk to return a ~3.1&nbsp;KB value. Hit rates from mm_filemap '
             'per-inode tracepoints; latency from easy-stress HDR (--hdr).</div>')
    h.append('</body></html>')
    open(out_path, "w", encoding="utf-8").write("\n".join(h))
    print("wrote %s (%d configs: %s)" % (out_path, len(cfgs), ", ".join("%dKB" % d["chunk_kb"] for d in cfgs)))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "results"))
    args = ap.parse_args()
    render(load(args.out), os.path.join(args.out, "chunk_sweep_report.html"))


if __name__ == "__main__":
    main()
