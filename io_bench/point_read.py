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
# point_read.py -- the Phase A "one isolated cold read" primitive.
#
# cqlsh cannot run on this host (bin/cqlsh + the bundled cassandra-driver hard-cap
# at Python <=3.13; only 3.14 is installed), so we cannot issue a hand-crafted
# single-key CQL SELECT. Instead we drive cassandra-easy-stress's KeyValue
# workload for EXACTLY ONE operation, read-only, single-threaded, rate 1:
#
#     java -jar <jar> run KeyValue --host <h> --keyspace <ks> --no-schema \
#          -n 1 -r 1.0 -t 1 --rate 1 -p <partitions>
#
#   -n 1     -> one operation total
#   -r 1.0   -> that operation is a read
#   -t 1     -> one client thread
#   --rate 1 -> one op/s ceiling; with -n 1 there is never >1 read in flight
#   --no-schema -> reuse the keyspace/table the write phase already created
#
# The eBPF collectors are started before this call and stopped after, so the
# device/VFS IO they attribute during the window belongs to this single read
# (plus a small, filterable amount of client-connection system-table traffic,
# which the io_operations.log delta lets orchestrate.py exclude by ks/tbl).
#
# orchestrate.py imports single_read() directly; the CLI is for manual checks.

import argparse
import json
import re
import select
import subprocess
import time

# Reads column of easy-stress's console table: "Count p99 rate"; the leading int
# is the cumulative read count. Used to confirm the one read actually executed.
_READS_RE = re.compile(r"\|\s*(\d+)\s+[\d.]+\s+[\d.]+\s*\|\s*\d+\s+[\d.]+\s+[\d.]+\s*\|")


def single_read(jar, ks, host="127.0.0.1", partitions=20000000, threads=1,
                timeout=180, seq_id="001", extra_args=None):
    """Run exactly one read op via easy-stress. Returns a timing/status dict.

    Uses --pg sequence with the same --id as the write phase so the single read
    targets an EXISTING key (sequence position 0 = "<seq_id>.0.0"); the default
    random generator over a huge -p almost always misses the populated subset,
    bloom-filters out, and reads no Data.db (nothing to attribute)."""
    argv = [
        "java", "-jar", jar, "run", "KeyValue",
        "--host", host,
        "--keyspace", ks,
        "--no-schema",
        "-n", "1",
        "-r", "1.0",
        "-t", str(threads),
        "--rate", "1",
        "-p", str(partitions),
        "--pg", "sequence",
        "--id", seq_id,
    ]
    if extra_args:
        argv.extend(extra_args)
    t0 = time.monotonic()
    try:
        proc = subprocess.run(argv, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                              text=True, timeout=timeout)
        rc, out, err = proc.returncode, proc.stdout, proc.stderr
    except subprocess.TimeoutExpired as e:
        rc = 124
        out = e.stdout.decode() if isinstance(e.stdout, bytes) else (e.stdout or "")
        err = "timeout after %ss" % timeout
    elapsed = time.monotonic() - t0
    return {
        "rc": rc,
        "ok": rc == 0,
        "elapsed_s": elapsed,
        "reads_reported": parse_reads_count(out),
        "stdout_tail": (out or "")[-600:],
        "stderr_tail": (err or "")[-400:],
    }


def parse_reads_count(text):
    """Pull the cumulative Reads Count from the last easy-stress data row."""
    last = None
    for m in _READS_RE.finditer(text or ""):
        last = int(m.group(1))
    return last


class PersistentReader:
    """A long-lived, already-warm single-partition read client.

    Wraps io_bench/tools/PersistentReader.java (run via the JDK single-file source
    launcher against the driver in the easy-stress jar). It connects ONCE, so each
    Phase A measurement window carries only the single query's cold IO instead of a
    fresh driver connect + schema-metadata fetch. Protocol over stdio: write one
    key per line, read back a "DONE key=.. rows=.. us=.." line; "QUIT" -> "BYE".
    Stray driver/SLF4J log lines on stdout are ignored (we match on the marker)."""

    def __init__(self, jar, source, host="127.0.0.1", port=9042, dc="datacenter1",
                 ks="iobench_ks", table="keyvalue", stderr_path=None, ready_timeout=120):
        argv = ["java", "--class-path", str(jar), "--source", "25", str(source),
                str(host), str(port), str(dc), str(ks), str(table)]
        self._err = open(stderr_path, "w") if stderr_path else subprocess.DEVNULL
        self.proc = subprocess.Popen(argv, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                     stderr=self._err, text=True, bufsize=1)
        self.ok = self._await("READY", ready_timeout) is not None

    def _await(self, prefix, timeout):
        """Return the first stdout line starting with `prefix`, or None on
        timeout/EOF; other (log) lines are skipped."""
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            if self.proc.poll() is not None:
                for ln in self.proc.stdout:  # drain buffered output after exit
                    if ln.startswith(prefix):
                        return ln.strip()
                return None
            r, _, _ = select.select([self.proc.stdout], [], [], 0.5)
            if not r:
                continue
            ln = self.proc.stdout.readline()
            if not ln:
                return None
            if ln.startswith(prefix):
                return ln.strip()
        return None

    def read_key(self, key, timeout=60):
        """Issue exactly one read for `key`; returns dict(ok, rows, us, ...)."""
        if self.proc.poll() is not None:
            return {"ok": False, "err": "reader exited", "key": key}
        try:
            self.proc.stdin.write(key + "\n")
            self.proc.stdin.flush()
        except (BrokenPipeError, OSError) as e:
            return {"ok": False, "err": str(e), "key": key}
        line = self._await("DONE", timeout)
        if line is None:
            return {"ok": False, "err": "no DONE marker", "key": key}
        m = re.search(r"rows=(\d+)\s+us=(\d+)", line)
        return {"ok": True, "key": key,
                "rows": int(m.group(1)) if m else None,
                "us": int(m.group(2)) if m else None, "line": line}

    def close(self, timeout=10):
        try:
            if self.proc.poll() is None:
                self.proc.stdin.write("QUIT\n")
                self.proc.stdin.flush()
                self.proc.wait(timeout=timeout)
        except (BrokenPipeError, OSError, subprocess.TimeoutExpired):
            try:
                self.proc.kill()
            except OSError:
                pass
        finally:
            if self._err not in (subprocess.DEVNULL, None):
                try:
                    self._err.close()
                except OSError:
                    pass


def _self_test():
    sample = (
        "  Count  Latency (p99)  1min (req/s) |   Count  Latency (p99)  1min (req/s) |"
        "   Count  Latency (p99)  1min (req/s) |   Count  1min (errors/s)\n"
        "      0          0.00             0 |       1          5.10             1 |"
        "       0              0             0 |       0                0\n"
    )
    assert parse_reads_count(sample) == 1, parse_reads_count(sample)
    print("point_read self-test OK")


def main():
    ap = argparse.ArgumentParser(description="Issue exactly one isolated read via easy-stress.")
    ap.add_argument("--jar", help="path to cassandra-easy-stress fat jar")
    ap.add_argument("--ks", default="iobench_ks")
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--partitions", type=int, default=20000000)
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args()
    if args.self_test:
        _self_test()
        return
    if not args.jar:
        ap.error("--jar is required (or use --self-test)")
    print(json.dumps(single_read(args.jar, args.ks, args.host, args.partitions), indent=2))


if __name__ == "__main__":
    main()
