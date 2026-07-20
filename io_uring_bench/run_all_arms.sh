#!/bin/bash
B=/home/cscotta/projects/cassandra/io_uring_bench
run() { # NAME DAM DIRECTIO COMPREAD AUTOCOMPACT RESTORE READRATE
  echo "======== $(date +%T) PREP $1 ========"
  bash $B/prep_arm.sh "$1" "$2" "$3" "$4" "$5" "$6" 2>&1 | grep -viE "pkg_resources|UserWarning|import pkg"
  sync; sudo -n /usr/bin/sysctl -w vm.drop_caches=3 >/dev/null && echo "caches dropped for $1"
  echo "======== $(date +%T) MEASURE $1 ========"
  bash $B/measure_arm.sh "$1" "$7" 2>&1 | grep -viE "pkg_resources|UserWarning|import pkg"
}
run read-standard         standard false auto     off no  1.0
run read-direct           direct   false direct   off no  1.0
run read-iouring-odirect  io_uring true  io_uring off no  1.0
run mixed-standard        standard false auto     on  no  0.8
run mixed-iouring-odirect io_uring true  io_uring on  yes 0.8
echo "ALL ARMS DONE $(date +%T)"
