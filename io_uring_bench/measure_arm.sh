#!/bin/bash
NAME=$1; READRATE=$2
pkill -f "iostat -x nvme0n1" 2>/dev/null; pkill -x mpstat 2>/dev/null; sleep 1
BASE=/home/cscotta/projects/cassandra; RES=$BASE/io_uring_bench/results; mkdir -p "$RES"
JAR=$BASE/cassandra-easy-stress/build/libs/cassandra-easy-stress-10-all.jar
iostat -x nvme0n1 5 > "$RES/$NAME.iostat" 2>&1 & IOPID=$!
mpstat 5 > "$RES/$NAME.mpstat" 2>&1 & MPPID=$!
java -jar $JAR run IoUringReadBench --host 127.0.0.1 --keyspace iouring_bench --no-schema \
  -p 4000000 -t 16 --id 001 --readrate $READRATE --rate 30000 --queue 64 -d "5m" > "$RES/$NAME.stress" 2>&1
kill $IOPID $MPPID 2>/dev/null
ccm node1 nodetool "tablehistograms iouring_bench.readbench" > "$RES/$NAME.histo" 2>&1
echo "MEASURE $NAME DONE $(date +%T)"
