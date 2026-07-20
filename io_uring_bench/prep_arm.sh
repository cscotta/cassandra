#!/bin/bash
NAME=$1; DAM=$2; DIRECTIO=$3; COMPREAD=$4; AUTOCOMPACT=$5; RESTORE=$6
NW="pkg_resources|UserWarning|import pkg|WARNING|Unsafe|high-scale|egrep|obsolescent"
CCM=$HOME/.ccm/iouringbench
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
# readiness poll: all 3 nodes must report binary proto running
for N in 1 2 3; do
  for t in $(seq 1 30); do
    if ccm node$N nodetool "statusbinary" 2>/dev/null | grep -q running; then break; fi
    sleep 2
  done
done
# real connectivity gate: statusbinary can report running before the port accepts, so actually
# open a TCP connection to the native port on each node (retry ~60s), then settle.
for N in 1 2 3; do
  for t in $(seq 1 30); do
    if (exec 3<>/dev/tcp/127.0.0.$N/9042) 2>/dev/null; then exec 3>&- 3<&-; break; fi
    sleep 2
  done
done
sleep 5
if [ "$AUTOCOMPACT" = "off" ]; then for N in 1 2 3; do ccm node$N nodetool "disableautocompaction iouring_bench readbench" >/dev/null 2>&1; done; fi
echo -n "nodes running binary: "; for N in 1 2 3; do ccm node$N nodetool "statusbinary" 2>/dev/null | grep -o running; done | grep -c running
if [ "$DAM" = "io_uring" ]; then echo -n "io_uring available (node1 log): "; grep -c "io_uring is available" $CCM/node1/logs/system.log 2>/dev/null; fi
echo "PREP $NAME READY"
