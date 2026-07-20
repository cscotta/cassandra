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
# jmx_hist.sh -- thin wrapper that feeds ks/tbl/host/port to jmx_hist.jsh via
# environment variables (jshell scripts cannot take positional args) and runs
# it through jshell (pure JDK, no dependencies). OPTIONAL: orchestrate.py calls
# this only as a cross-check and ignores it if JMX is unreachable.
#
# Usage: jmx_hist.sh <keyspace> <table> [jmx_host] [jmx_port]
#        (host defaults 127.0.0.1, port defaults 7100 == ccm node1 JMX)

set -u

KS="${1:-}"
TBL="${2:-}"
JMX_HOST="${3:-127.0.0.1}"
JMX_PORT="${4:-7100}"

if [ -z "$KS" ] || [ -z "$TBL" ]; then
  echo "usage: $0 <keyspace> <table> [host] [port]" >&2
  exit 2
fi

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export KS TBL JMX_HOST JMX_PORT

# -q quiets jshell's startup banner; the .jsh ends with /exit so this returns.
exec jshell -q "$HERE/jmx_hist.jsh"
