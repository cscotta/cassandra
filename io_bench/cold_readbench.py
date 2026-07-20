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
# cold_readbench.py -- cold single-partition read IO measurement against the
# pre-loaded, fully-compacted iouring_bench.readbench table on /mnt/xfsdata.
#
# For each measured read: drop the OS page cache, run exactly one point read
# through an already-warm persistent client (so the window holds only the query's
# cold IO, not a driver connect + schema fetch), and capture:
#   * io_operations.log  -> the query's reads split by component (Data.db vs
#     Index.db). With disk_access_mode=standard the index is buffered, so index
#     reads are preads that show up here (not invisible mmap faults).
#   * vfs_reads.bt (PID)  -> every Cassandra pread in the window = the clean
#     per-request logical IOPS/bytes (data + index).
#   * block_io.bt (parent disk 259:0) -> device reads. NOTE: block tracepoints
#     report a partition under its parent disk, so this is nvme0n1-wide; with a
#     warm client on an otherwise-quiet box it is dominated by this query's cold
#     sstable reads. (Sector-range narrowing to p8 is a TODO in block_io.bt.)
#   * system_views per-read histograms -> server-side cross-check.
#
# Reuses the validated collectors/parsers from orchestrate.py and the persistent
# read client from point_read.py.

import os
import statistics
import subprocess
import sys
import time
from pathlib import Path

IO_BENCH = Path(__file__).resolve().parent
sys.path.insert(0, str(IO_BENCH))
import point_read          # noqa: E402
import orchestrate as O    # noqa: E402

KS = "iouring_bench"
TBL = "readbench"
DEV_T_PARENT = (259 << 20) | 0     # block tracepoints report p8 under parent nvme0n1 (259:0)
NATIVE_HOST, NATIVE_PORT, LOCAL_DC = "127.0.0.1", 9042, "datacenter1"
LOG = Path(os.path.expanduser("~/.ccm/iobench/node1/logs/io_operations.log"))
PIDFILE = Path(os.path.expanduser("~/.ccm/iobench/node1/cassandra.pid"))
PREADER = IO_BENCH / "tools" / "PersistentReader.java"


def med(xs):
    xs = [x for x in xs if isinstance(x, (int, float))]
    return statistics.median(xs) if xs else None


def main():
    n = int(sys.argv[1]) if len(sys.argv) > 1 else 8
    pid = int(PIDFILE.read_text().strip())
    tmp = Path("/tmp/coldrb")
    tmp.mkdir(exist_ok=True)
    cfg = {"tmp": tmp, "attach_wait": 15, "settle": 1.5}

    # Partition sector window so block_io.bt (tracing the parent disk 259:0)
    # counts only /mnt/xfsdata (p8) IO, excluding sibling /home traffic.
    cenv = O.parse_env_file("/mnt/xfsdata/cluster.env")
    part_start = int(cenv.get("PART_START", "0") or 0)
    part_sectors = int(cenv.get("PART_SECTORS", "0") or 0)
    blk_arg = (DEV_T_PARENT, part_start, part_sectors)
    print("# block_io filter: parent dev_t=%d p8 sectors=[%d,%d)"
          % (DEV_T_PARENT, part_start, part_start + part_sectors))

    O.set_iolog({"cluster": "iobench"}, "TRACE")
    reader = point_read.PersistentReader(str(O.JAR), str(PREADER), NATIVE_HOST, NATIVE_PORT,
                                         LOCAL_DC, KS, TBL, stderr_path=str(tmp / "preader.err"))
    if not reader.ok:
        print("ERROR: persistent reader failed to connect")
        return
    reader.read_key("%s.0.0" % O.ES_RUN_ID)  # warm (not measured)

    rows = []
    print("%-12s %5s %5s %6s %7s %8s %8s %9s %9s" %
          ("key", "data", "index", "vfsIO", "vfsKB", "devIO", "devKB", "logKB", "rows"))
    for i in range(n):
        key = "%s.0.%d" % (O.ES_RUN_ID, i + 1)
        O.drop_caches()
        off = LOG.stat().st_size if LOG.exists() else 0
        blk = O.Collector(O.BLOCK_BT, blk_arg, tmp / ("blk%d.out" % i), "# block_io.bt").start()
        vfs = O.Collector(O.VFS_BT, pid, tmp / ("vfs%d.out" % i), "# vfs_reads.bt").start()
        blk.wait_attached(cfg["attach_wait"])
        vfs.wait_attached(cfg["attach_wait"])
        time.sleep(1.0)
        rd = reader.read_key(key, timeout=60)
        time.sleep(cfg["settle"])
        blk_txt = blk.stop()
        vfs_txt = vfs.stop()
        with open(LOG, errors="ignore") as f:
            f.seek(off)
            iolog_txt = f.read()

        block = O.parse_block_summary(blk_txt) or {}
        vfsd = O.parse_vfs_summary(vfs_txt) or {}
        iol = O.parse_io_operations(iolog_txt, ks=KS, tbl=TBL)
        comp = iol["by_component"]
        row = {
            "key": key, "rows": rd.get("rows"),
            "data": comp.get("Data.db", 0), "index": comp.get("Index.db", 0),
            "vfs_io": vfsd.get("pread_count", 0), "vfs_kb": vfsd.get("pread_bytes", 0) / 1024.0,
            "dev_io": block.get("read_completions", 0), "dev_kb": block.get("read_bytes", 0) / 1024.0,
            "log_kb": iol["target_bytes_logical"] / 1024.0,
        }
        rows.append(row)
        print("%-12s %5d %5d %6d %7.1f %8d %8.1f %9.1f %9s" %
              (key, row["data"], row["index"], row["vfs_io"], row["vfs_kb"],
               row["dev_io"], row["dev_kb"], row["log_kb"], row["rows"]))

    reader.close()
    O.set_iolog({"cluster": "iobench"}, "OFF")

    print("\n=== medians over %d cold reads ===" % n)
    for k, lbl in [("data", "Data.db reads/req"), ("index", "Index.db reads/req"),
                   ("vfs_io", "VFS preads/req (data+index)"), ("vfs_kb", "VFS KB/req"),
                   ("dev_io", "device reads/req"), ("dev_kb", "device KB/req"),
                   ("log_kb", "logical KB/req (readbench)")]:
        print("  %-30s %s" % (lbl, med([r[k] for r in rows])))

    r = O.run(["bash", str(O.METRICS_SH), KS, TBL, NATIVE_HOST, str(NATIVE_PORT), LOCAL_DC],
              timeout=90, quiet=True, env_extra={"JAR": str(O.JAR)})
    hist = O.parse_hist(r["out"])
    if hist:
        print("\n=== server per-request histograms (system_views %s.%s) ===" % (KS, TBL))
        for a in ("read_iops", "bytes_read", "files_touched", "bytes_decompressed"):
            print("  %-22s p50=%s p99=%s max=%s"
                  % (a, O.hist_stat(hist, a, "p50"), O.hist_stat(hist, a, "p99"), O.hist_stat(hist, a, "max")))


if __name__ == "__main__":
    main()
