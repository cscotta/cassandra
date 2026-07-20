#!/usr/bin/env bash
# Final fully-instrumented sweep: legacy-64KB + fixed-output 16/32/48/64 KB, same build/node.
# Per arm: timed streaming rewrite (concurrent compactors = all cores), authoritative on-disk ratio
# (settled), block fill, cold(n=40) per-read IOPS, tablehistograms, WARM-SLO (maxrlat) + WARM-SAT
# (saturation w/ CPU+device-util). Requires node started with io_tracking.enabled + set_sep_thread_name.
set -u
cd /home/cscotta/projects/cassandra
NT=~/.ccm/iobench/node1/bin/nodetool
CORES=$(nproc)
OUT=/tmp/final_sweep
mkdir -p "$OUT"
SUM="$OUT/summary.txt"; : > "$SUM"
flt(){ grep -viE "WARNING|Unsafe|high_scale|maintainers|reporting this|pkg_resources|import pkg"; }

echo "== preflight $(date +%T) ==" | tee -a "$SUM"
ps aux | grep -o "io_tracking.enabled=true\|set_sep_thread_name=true" | sort -u | sed 's/^/  flag: /' | tee -a "$SUM"
$NT -p 7100 setconcurrentcompactors "$CORES" >/dev/null 2>&1 && echo "  concurrent compactors = $CORES" | tee -a "$SUM"

sample_fill(){ python3 - "$1" <<'PY'
import glob, struct, os, sys
S=int(sys.argv[1])
files=sorted(glob.glob("/mnt/xfsdata/data/iouring_bench/readbench-*/*-Data.db"), key=os.path.getsize, reverse=True)[:4]
tp=tot=0
for f in files:
    n=os.path.getsize(f)//S
    with open(f,'rb') as fh:
        step=max(1,n//2000); i=0
        while i<n:
            fh.seek(i*S); h=fh.read(4)
            if len(h)<4: break
            pl=struct.unpack('>i',h)[0]
            if 0<=pl<=S-8: tp+=pl+8; tot+=1
            i+=step
print("avg_fill=%.2f%%" % (tp/(S*tot)*100) if tot else "avg_fill=n/a")
PY
}

for ARM in legacy 16 32 48 64; do
  echo "======== ARM=$ARM START $(date +%T) ========" | tee -a "$SUM"
  if [ "$ARM" = "legacy" ]; then LABEL=64; S=0
    io_bench/tools/cql.sh "ALTER TABLE iouring_bench.readbench WITH compression = {'class':'ZstdCompressor','compression_level':'8','chunk_length_in_kb':'64'}" 2>&1 | flt | grep -iE "CQLOK|CQLERR" | tee -a "$SUM"
  else LABEL=$ARM; S=$((ARM*1024)); CAP=$((ARM*8))
    io_bench/tools/cql.sh "ALTER TABLE iouring_bench.readbench WITH compression = {'class':'ZstdCompressor','compression_level':'8','compressed_chunk_length_in_kb':'${ARM}','max_uncompressed_chunk_length_in_kb':'${CAP}'}" 2>&1 | flt | grep -iE "CQLOK|CQLERR" | tee -a "$SUM"
  fi

  $NT -p 7100 setcompactionthroughput 0 >/dev/null 2>&1
  echo "  rewrite start $(date +%T)" | tee -a "$SUM"
  # -j 0 = use all available compaction threads (concurrent_compactors set to $CORES above); the
  # upgradesstables default is only 2 jobs regardless of concurrent_compactors.
  $NT -p 7100 upgradesstables -a -j 0 iouring_bench readbench 2>&1 | flt
  echo "  rewrite done  $(date +%T)" | tee -a "$SUM"

  echo "  settling 20s (obsolete sstable cleanup)..." | tee -a "$SUM"; sleep 20
  # authoritative ratio (metadata) + settled space
  $NT -p 7100 tablestats iouring_bench.readbench 2>/dev/null | flt | grep -iE "compression ratio|space used \(live\)" | sed 's/^/  ONDISK /' | tee -a "$SUM"
  [ "$S" -gt 0 ] && echo "  FILL $(sample_fill $S)" | tee -a "$SUM"

  $NT -p 7100 stop COMPACTION >/dev/null 2>&1; sleep 3
  ACT=$($NT -p 7100 compactionstats 2>/dev/null | grep -icE "Upgrade sstables|Compaction .* bytes")
  echo "  active compactions before measure: $ACT" | tee -a "$SUM"

  echo "  COLD(n=40) start $(date +%T)" | tee -a "$SUM"
  python3 io_bench/cold_readbench.py 40 2>&1 | flt > "$OUT/cold_${ARM}.txt"
  sed -n '/=== medians/,$p' "$OUT/cold_${ARM}.txt" | sed 's/^/  COLD /' | tee -a "$SUM"

  # pre-existing per-read shape: sstables/read, partition size, local read latency
  $NT -p 7100 tablehistograms iouring_bench.readbench 2>/dev/null | flt > "$OUT/hist_${ARM}.txt"
  sed -n '1,12p' "$OUT/hist_${ARM}.txt" | sed 's/^/  TABLEHIST /' | tee -a "$SUM"

  $NT -p 7100 stop COMPACTION >/dev/null 2>&1; sleep 2
  echo "  WARM-SLO start $(date +%T)" | tee -a "$SUM"
  python3 io_bench/warm_readbench.py 120s 50 32 "$LABEL" 2>&1 | flt > "$OUT/warm_slo_${ARM}.txt"
  grep -iE "^  ops served|^  errors|read latency|IOPS per query|CPU \(|device util|bottleneck|VFS preads|device reads \(p8|compression ratio|cachestat \(mm|Index.db|Data.db" "$OUT/warm_slo_${ARM}.txt" | sed 's/^/  SLO /' | tee -a "$SUM"

  $NT -p 7100 stop COMPACTION >/dev/null 2>&1; sleep 2
  echo "  WARM-SAT start $(date +%T)" | tee -a "$SUM"
  python3 io_bench/warm_readbench.py 120s r140000 64 "$LABEL" 2>&1 | flt > "$OUT/warm_sat_${ARM}.txt"
  grep -iE "^  ops served|^  errors|read latency|IOPS per query|CPU \(|device util|bottleneck|VFS preads|device reads \(p8|compression ratio|cachestat \(mm|Index.db|Data.db" "$OUT/warm_sat_${ARM}.txt" | sed 's/^/  SAT /' | tee -a "$SUM"

  echo "======== ARM=$ARM DONE $(date +%T) ========" | tee -a "$SUM"; echo | tee -a "$SUM"
done
echo "ALL DONE $(date +%T)" | tee -a "$SUM"
