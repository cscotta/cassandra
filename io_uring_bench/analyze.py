#!/usr/bin/env python3
"""Parse the io_uring benchmark arm results (easy-stress + iostat + mpstat + nodetool histo)
and emit a text summary plus a dependency-free HTML report with CSS bar charts."""
import glob
import os
import re

RES = "/home/cscotta/projects/cassandra/io_uring_bench/results"
ARMS = [
    ("read-standard", "Read-only · standard (buffered)"),
    ("read-direct", "Read-only · direct (O_DIRECT FileChannel)"),
    ("read-iouring-odirect", "Read-only · io_uring (O_DIRECT ring)"),
    ("mixed-standard", "80/20 mixed · standard"),
    ("mixed-iouring-odirect", "80/20 mixed · io_uring O_DIRECT (+compaction_read)"),
]
DUR = 300.0  # measured seconds


def last_data_line(path):
    line = None
    for ln in open(path, encoding="utf-8", errors="ignore"):
        if "|" in ln and "Count" not in ln and ln.strip():
            line = ln
    return line


def parse_stress(name):
    p = os.path.join(RES, name + ".stress")
    if not os.path.exists(p):
        return None
    ln = last_data_line(p)
    if not ln:
        return None
    parts = [x.split() for x in ln.split("|")]
    if len(parts) < 4 or len(parts[0]) < 3 or len(parts[1]) < 3 or len(parts[3]) < 1:
        return None  # arm failed / malformed (e.g. connection error), skip it
    w, r, d, e = parts[0], parts[1], parts[2], parts[3]
    return {
        "read_count": int(r[0]), "read_p99": float(r[1]), "read_rate": float(r[2]),
        "write_count": int(w[0]), "write_p99": float(w[1]), "write_rate": float(w[2]),
        "errors": int(e[0]),
        "read_thru": int(r[0]) / DUR, "write_thru": int(w[0]) / DUR,
    }


def parse_iostat(name):
    p = os.path.join(RES, name + ".iostat")
    if not os.path.exists(p):
        return None
    rows = []
    for ln in open(p, encoding="utf-8", errors="ignore"):
        if ln.startswith("nvme0n1"):
            f = ln.split()
            rows.append(f)
    if len(rows) <= 1:
        return None
    rows = rows[1:]  # drop since-boot first sample
    def avg(i):
        return sum(float(r[i]) for r in rows) / len(rows)
    return {"r_s": avg(1), "rkB_s": avg(2), "r_await": avg(5),
            "w_s": avg(7), "wkB_s": avg(8), "aqu": avg(21), "util": avg(22)}


def parse_mpstat(name):
    p = os.path.join(RES, name + ".mpstat")
    if not os.path.exists(p):
        return None
    us = sy = io = so = idl = n = 0
    for ln in open(p, encoding="utf-8", errors="ignore"):
        f = ln.split()
        if len(f) >= 13 and f[2] == "all" and f[0] != "Average:":
            us += float(f[3]); sy += float(f[5]); io += float(f[6]); so += float(f[8]); idl += float(f[12]); n += 1
    if not n:
        return None
    return {"usr": us/n, "sys": sy/n, "iowait": io/n, "soft": so/n, "idle": idl/n, "busy": 100 - idl/n}


def parse_histo(name):
    p = os.path.join(RES, name + ".histo")
    if not os.path.exists(p):
        return None
    for ln in open(p, encoding="utf-8", errors="ignore"):
        if ln.strip().startswith("99%"):
            f = ln.split()
            return {"srv_read_p99_us": float(f[1])}
    return None


def collect():
    data = {}
    for name, label in ARMS:
        s = parse_stress(name)
        if not s:
            continue
        data[name] = {"label": label, "stress": s, "iostat": parse_iostat(name),
                      "mpstat": parse_mpstat(name), "histo": parse_histo(name)}
    return data


def bar(val, vmax, color):
    w = 0 if vmax <= 0 else max(1.0, 100.0 * val / vmax)
    return f'<div style="background:{color};height:1.1em;width:{w:.1f}%;border-radius:3px"></div>'


METRICS = [
    ("Read throughput (ops/s)", lambda d: d["stress"]["read_thru"], "{:.0f}", "#2a9d8f", False),
    ("Write throughput (ops/s)", lambda d: d["stress"]["write_thru"], "{:.0f}", "#8ab17d", False),
    ("Client read p99 (ms)", lambda d: d["stress"]["read_p99"], "{:.2f}", "#e76f51", True),
    ("Server read p99 (µs)", lambda d: d["histo"]["srv_read_p99_us"] if d["histo"] else 0, "{:.0f}", "#e9c46a", True),
    ("Errors (total)", lambda d: d["stress"]["errors"], "{:.0f}", "#c1121f", True),
    ("CPU busy (%)", lambda d: d["mpstat"]["busy"] if d["mpstat"] else 0, "{:.1f}", "#264653", True),
    ("CPU %sys+%soft (kernel)", lambda d: (d["mpstat"]["sys"]+d["mpstat"]["soft"]) if d["mpstat"] else 0, "{:.1f}", "#457b9d", True),
    ("Disk read IOPS (r/s)", lambda d: d["iostat"]["r_s"] if d["iostat"] else 0, "{:.0f}", "#6a4c93", False),
    ("Disk read MB/s", lambda d: (d["iostat"]["rkB_s"]/1024) if d["iostat"] else 0, "{:.0f}", "#1982c4", True),
    ("Disk avg queue depth", lambda d: d["iostat"]["aqu"] if d["iostat"] else 0, "{:.1f}", "#ff924c", False),
    ("Disk %util", lambda d: d["iostat"]["util"] if d["iostat"] else 0, "{:.1f}", "#ff595e", False),
    ("Disk r_await (ms)", lambda d: d["iostat"]["r_await"] if d["iostat"] else 0, "{:.2f}", "#52796f", True),
]


def render(data):
    names = [n for n, _ in ARMS if n in data]
    # text summary
    print(f"{'metric':32}", *[f"{n:>26}" for n in names])
    for title, fn, fmt, _c, lower in METRICS:
        vals = [fn(data[n]) for n in names]
        print(f"{title:32}", *[f"{fmt.format(v):>26}" for v in vals])
    # html
    html = ['<!DOCTYPE html><html><head><meta charset="utf-8"><title>io_uring Cassandra benchmark</title>',
            '<style>body{font-family:-apple-system,Segoe UI,Roboto,sans-serif;max-width:1100px;margin:2rem auto;padding:0 1rem;line-height:1.5}',
            'h1{border-bottom:2px solid #ccc}h2{margin-top:1.8em}table{border-collapse:collapse;width:100%;margin:1em 0;font-size:.9em}',
            'th,td{border:1px solid #ccc;padding:.4em .6em;text-align:right}th:first-child,td:first-child{text-align:left}',
            '.mgrid{display:grid;grid-template-columns:1fr;gap:1.4em}.metric{border:1px solid #e0e0e0;border-radius:6px;padding:.8em 1em}',
            '.row{display:grid;grid-template-columns:320px 1fr 90px;gap:.6em;align-items:center;margin:.25em 0}',
            '.lo{color:#2a7} .cap{color:#666;font-size:.85em}</style></head><body>',
            '<h1>io_uring read path — Cassandra 3-node benchmark</h1>',
            '<p class="cap">64&nbsp;GB/node (RF=3, 64M&times;1KB, Zstd@8, LCS-160, leveled), cold start (drop_caches per arm), '
            '30k ops/s offered, 16 threads, queue 64, 5&nbsp;min/arm. Lower is better where noted (▼).</p>']
    # table
    html.append('<h2>Summary</h2><table><tr><th>metric</th>' + ''.join(f'<th>{data[n]["label"]}</th>' for n in names) + '</tr>')
    for title, fn, fmt, _c, lower in METRICS:
        arrow = ' ▼' if lower else ''
        html.append(f'<tr><td>{title}{arrow}</td>' + ''.join(f'<td>{fmt.format(fn(data[n]))}</td>' for n in names) + '</tr>')
    html.append('</table>')
    # bar charts
    html.append('<h2>Charts</h2><div class="mgrid">')
    for title, fn, fmt, color, lower in METRICS:
        vals = [fn(data[n]) for n in names]
        vmax = max(vals) if vals else 0
        html.append(f'<div class="metric"><b>{title}</b>{" <span class=lo>(lower is better)</span>" if lower else ""}')
        for n, v in zip(names, vals):
            html.append(f'<div class="row"><span>{data[n]["label"]}</span>{bar(v, vmax, color)}<span>{fmt.format(v)}</span></div>')
        html.append('</div>')
    html.append('</div>')
    html.append('''<h2>Findings</h2>
<ul>
<li><b>Same throughput across arms</b> — the 30k ops/s offered rate was below the cluster's ceiling, so every arm met it; this is a latency/efficiency comparison, not a max-throughput one.</li>
<li><b>The workload is CPU-bound, not device-bound</b> — CPU sits at ~95–98% while the NVMe is only ~56% utilised with a shallow queue (~3). On this fast Apple SSD + 10 cores + small (1&nbsp;KB) rows, CPU saturates before the device, so io_uring's async / deep-queue advantage (the 3.6–16&times; the microbenchmark showed at QD&ge;16) cannot show up as throughput or latency gains.</li>
<li><b>The consistent io_uring+O_DIRECT win is I/O efficiency</b> — it reads about <b>half the device bandwidth</b> for the same logical throughput (570 vs 1056 MB/s read-only; 458 vs 835 MB/s mixed) with lower per-read device latency (r_await 0.10–0.13 vs 0.14–0.17&nbsp;ms). Mechanism: O_DIRECT reads only the aligned compressed chunk, whereas buffered <code>standard</code> triggers OS read-ahead that roughly doubles bytes fetched. This efficiency would convert to throughput/latency gains on genuinely device-bound deployments (slower or more-contended storage, larger-than-RAM working sets, or higher client concurrency than one 10-core box can drive without CPU-saturating first).</li>
<li><b>Correctness</b> — no FSReadError / CorruptSSTable / overload errors in any node log during the arms; per-arm error rates &lt;0.01% (transient ramp timeouts). Compaction ran throughout the mixed arms with <code>compaction_read_disk_access_mode=io_uring</code>.</li>
</ul>
<h2>Caveats</h2>
<ul>
<li>Rate-capped at 30k ops/s (sub-saturation). Pushing to saturation collapsed into CPU thrash (load &gt;200) rather than a clean device-bound plateau — this box cannot sustain a device-saturating read rate without CPU-limiting first.</li>
<li>An O_DIRECT-FileChannel control (<code>disk_access_mode: direct</code>) could not be run — <code>direct</code> is not a supported <code>disk_access_mode</code> in this build (only for <code>compaction_read_disk_access_mode</code>). The bandwidth halving is therefore attributed mechanistically to O_DIRECT (no read-ahead), not cleanly isolated as io_uring-specific vs FileChannel-O_DIRECT.</li>
<li>Each arm starts cold (drop_caches); buffered arms re-warm the page cache during the run while O_DIRECT arms stay cold throughout.</li>
</ul>''')
    html.append('</body></html>')    out = "/home/cscotta/projects/cassandra/io_uring_bench/report.html"
    open(out, "w").write("\n".join(html))
    print("\nHTML report:", out)


if __name__ == "__main__":
    render(collect())
