#!/bin/bash
# args: NAME DAM DIRECTIO COMPREAD AUTOCOMPACT READRATE RESTORE
NAME=$1; DAM=$2; DIRECTIO=$3; COMPREAD=$4; AUTOCOMPACT=$5; READRATE=$6; RESTORE=$7
NW="pkg_resources|UserWarning|import pkg|WARNING|Unsafe|high-scale|egrep|obsolescent"
BASE=/home/cscotta/projects/cassandra
CCM=$HOME/.ccm/iouringbench
RES=$BASE/io_uring_bench/results; mkdir -p "$RES"
JAR=$BASE/cassandra-easy-stress/build/libs/cassandra-easy-stress-10-all.jar
echo "### $(date +%T) ARM $NAME start: dam=$DAM directio=$DIRECTIO compread=$COMPREAD autocompact=$AUTOCOMPACT readrate=$READRATE restore=$RESTORE"
ccm updateconf disk_access_mode:$DAM io_uring_direct_io:$DIRECTIO compaction_read_disk_access_mode:$COMPREAD 2>&1 | grep -viE "$NW"
ccm stop 2>&1 | grep -viE "$NW" >/dev/null
if [ "$RESTORE" = "yes" ]; then
  echo "restoring golden snapshot..."
  for N in 1 2 3; do
    TBL=$(ls -d $CCM/node$N/data0/iouring_bench/readbench-* 2>/dev/null | head -1)
    find "$TBL" -maxdepth 1 -type f -delete
    cp -al "$TBL"/snapshots/golden/*-big-* "$TBL"/ 2>/dev/null
    rm -f $CCM/node$N/commitlogs/* 2>/dev/null
  done
fi
ccm start --jvm_arg="-Xms8G" --jvm_arg="-Xmx8G" --jvm_arg="--enable-native-access=ALL-UNNAMED" --wait-for-binary-proto 2>&1 | grep -viE "$NW" >/dev/null
if [ "$AUTOCOMPACT" = "off" ]; then for N in 1 2 3; do ccm node$N nodetool "disableautocompaction iouring_bench readbench" >/dev/null 2>&1; done; fi
if [ "$DAM" = "io_uring" ]; then
  echo -n "io_uring availability lines in node1 log: "; grep -c "io_uring is available" $CCM/node1/logs/system.log 2>/dev/null
fi
# system-metric samplers over the run
iostat -x nvme0n1 5 > "$RES/$NAME.iostat" 2>&1 & IOPID=$!
mpstat 5 > "$RES/$NAME.mpstat" 2>&1 & MPPID=$!
# 5-minute measured run
java -jar $JAR run IoUringReadBench --host 127.0.0.1 --keyspace iouring_bench --no-schema \
  -p 4000000 -t 16 --id 001 --readrate $READRATE --rate 50000 --queue 64 -d "5m" > "$RES/$NAME.stress" 2>&1
kill $IOPID $MPPID 2>/dev/null
ccm node1 nodetool "tablehistograms iouring_bench.readbench" > "$RES/$NAME.histo" 2>&1
echo "### $(date +%T) ARM $NAME DONE"
