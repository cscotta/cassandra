#!/bin/bash
B=/home/cscotta/projects/cassandra/io_uring_bench
bash $B/run_arm.sh read-standard      standard false auto     off 1.0 no
bash $B/run_arm.sh read-direct         direct   false direct   off 1.0 no
bash $B/run_arm.sh read-iouring-odirect io_uring true  io_uring off 1.0 no
echo "ALL READ ARMS DONE"
