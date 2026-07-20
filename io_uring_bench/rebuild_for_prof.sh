#!/bin/bash
# Rebuild a 3-node io_uring+O_DIRECT cluster with a 16 GB/node leveled dataset for CPU profiling.
B=/home/cscotta/projects/cassandra
NW="pkg_resources|UserWarning|import pkg|WARNING|Unsafe|high-scale|egrep|obsolescent"
CP="$B/build/apache-cassandra-7.0-SNAPSHOT.jar:$(echo $B/build/lib/jars/*.jar | tr ' ' ':')"
JVMARGS=$(cat /tmp/cass_jvmargs.txt)
JH=/usr/lib/jvm/java-25-openjdk
JAR=$B/cassandra-easy-stress/build/libs/cassandra-easy-stress-10-all.jar
STAGE=$B/io_uring_bench/staging_prof
ready() { for N in 1 2 3; do for t in $(seq 1 40); do if (exec 3<>/dev/tcp/127.0.0.$N/9042) 2>/dev/null; then exec 3>&- 3<&-; break; fi; sleep 2; done; done; sleep 5; }

echo "=== $(date +%T) 1. regen 16M rows (16 GB/node) ==="
rm -rf "$STAGE"; mkdir -p "$STAGE"
$JH/bin/java -Xmx28g -XX:+UseZGC --enable-native-access=ALL-UNNAMED $JVMARGS \
  -Dgen.t=16 -Dgen.p=1000000 -Dgen.writers=10 -Dgen.out="$STAGE" -cp "$B/io_uring_bench/gen:$CP" SSTableGen 2>&1 | grep -iE "Done:"

echo "=== $(date +%T) 2. create + start cluster (io_uring O_DIRECT) ==="
ccm create iouringbench --install-dir=$B -n 3 2>&1 | grep -viE "$NW" | tail -1
ccm updateconf disk_access_mode:io_uring io_uring_direct_io:true compaction_read_disk_access_mode:io_uring concurrent_reads:128 2>&1 | grep -viE "$NW"
ccm start --jvm_arg="-Xms8G" --jvm_arg="-Xmx8G" --jvm_arg="--enable-native-access=ALL-UNNAMED" --wait-for-binary-proto 2>&1 | grep -viE "$NW" >/dev/null
ready

echo "=== $(date +%T) 3. create schema (RF=3, Zstd@8, LCS) ==="
$JH/bin/java -jar $JAR run IoUringReadBench --host 127.0.0.1 --keyspace iouring_bench \
  --replication "{'class':'SimpleStrategy','replication_factor':3}" \
  --compression "{'class':'ZstdCompressor','compression_level':'8'}" \
  --compaction "{'class':'LeveledCompactionStrategy','sstable_size_in_mb':'160'}" \
  --populate 0 -n 1 -t 1 --readrate 1.0 2>&1 | grep -iE "Stress complete|Exception" | tail -1

echo "=== $(date +%T) 4. import ==="
SHARDS=$(for w in $(seq 0 9); do echo -n "$STAGE/shard$w "; done)
for N in 1 2 3; do ccm node$N nodetool "import --no-tokens --no-verify --copy-data iouring_bench readbench $SHARDS" 2>&1 | grep -viE "$NW"; done

echo "=== $(date +%T) 5. relevel + restart + disable autocompaction ==="
ccm stop 2>&1 | grep -viE "$NW" >/dev/null
for N in 1 2 3; do CASSANDRA_JDK_UNSUPPORTED=true CASSANDRA_CONF=$HOME/.ccm/iouringbench/node$N/conf \
  tools/bin/sstableofflinerelevel iouring_bench readbench 2>&1 | grep -iE "L[0-9]=" | tail -2; done
ccm start --jvm_arg="-Xms8G" --jvm_arg="-Xmx8G" --jvm_arg="--enable-native-access=ALL-UNNAMED" --wait-for-binary-proto 2>&1 | grep -viE "$NW" >/dev/null
ready
for N in 1 2 3; do ccm node$N nodetool "disableautocompaction iouring_bench readbench" >/dev/null 2>&1; done
echo -n "io_uring available (node1): "; grep -c "io_uring is available" $HOME/.ccm/iouringbench/node1/logs/system.log
echo "REBUILD DONE $(date +%T)"
