#!/usr/bin/env bash
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
# metrics.sh -- read the six per-read IO histograms from system_views in one
# jshell/driver session. Preferred over JMX for the server-side cross-check.
#
# Usage: metrics.sh <keyspace> <table> [host] [port] [datacenter]
# Env overrides: JAR

set -u

KS="${1:-}"
TBL="${2:-}"
export CONTACT_HOST="${3:-127.0.0.1}"
export CONTACT_PORT="${4:-9042}"
export LOCAL_DC="${5:-datacenter1}"

if [ -z "$KS" ] || [ -z "$TBL" ]; then
  echo "usage: $0 <keyspace> <table> [host] [port] [datacenter]" >&2
  exit 2
fi

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
JAR="${JAR:-$REPO/cassandra-easy-stress/build/libs/cassandra-easy-stress-10-all.jar}"

if [ ! -f "$JAR" ]; then
  echo "CQL_ERROR MissingJar: $JAR" >&2
  exit 3
fi

export KS TBL
exec jshell -q --class-path "$JAR" "$HERE/metrics.jsh"
