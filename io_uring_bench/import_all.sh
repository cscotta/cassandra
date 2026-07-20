#!/bin/bash
for N in 1 2 3; do
  echo "=== $(date +%T) importing into node$N ==="
  ccm node$N nodetool "import --no-tokens --no-verify --copy-data iouring_bench readbench /home/cscotta/projects/cassandra/io_uring_bench/staging/shard0 /home/cscotta/projects/cassandra/io_uring_bench/staging/shard1 /home/cscotta/projects/cassandra/io_uring_bench/staging/shard2 /home/cscotta/projects/cassandra/io_uring_bench/staging/shard3 /home/cscotta/projects/cassandra/io_uring_bench/staging/shard4 /home/cscotta/projects/cassandra/io_uring_bench/staging/shard5 /home/cscotta/projects/cassandra/io_uring_bench/staging/shard6 /home/cscotta/projects/cassandra/io_uring_bench/staging/shard7 /home/cscotta/projects/cassandra/io_uring_bench/staging/shard8 /home/cscotta/projects/cassandra/io_uring_bench/staging/shard9 " && echo "=== $(date +%T) node$N OK ===" || echo "=== node$N FAILED ==="
done
echo "ALL IMPORTS DONE $(date +%T)"
