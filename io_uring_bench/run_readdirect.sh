#!/bin/bash
B=/home/cscotta/projects/cassandra/io_uring_bench
echo "======== $(date +%T) PREP read-direct (restore golden, hardened readiness) ========"
bash $B/prep_arm.sh read-direct direct false direct off yes 2>&1 | grep -viE "pkg_resources|UserWarning|import pkg"
sync; sudo -n /usr/bin/sysctl -w vm.drop_caches=3 >/dev/null && echo "caches dropped for read-direct"
echo "======== $(date +%T) MEASURE read-direct ========"
bash $B/measure_arm.sh read-direct 1.0 2>&1 | grep -viE "pkg_resources|UserWarning|import pkg"
echo "READ-DIRECT RETRY DONE $(date +%T)"
