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
# cql.sh -- run one CQL statement via jshell + the DataStax driver bundled in
# the easy-stress fat jar (cqlsh cannot run on this host's Python 3.14). The
# statement is passed to cql.jsh through the CQL env var; marker-prefixed output
# (CQLROW / CQLOK / CQLERR) is printed on stdout for the caller to grep.
#
# Usage: cql.sh "<CQL statement>"
# Env overrides: JAR, CONTACT_HOST (127.0.0.1), CONTACT_PORT (9042),
#                LOCAL_DC (datacenter1)

set -u

CQL_TEXT="${1:-}"
if [ -z "$CQL_TEXT" ]; then
  echo "usage: $0 \"<CQL statement>\"" >&2
  exit 2
fi

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
JAR="${JAR:-$REPO/cassandra-easy-stress/build/libs/cassandra-easy-stress-10-all.jar}"

if [ ! -f "$JAR" ]; then
  echo "CQLERR MissingJar: $JAR (build: cd cassandra-easy-stress && ./gradlew shadowJar)" >&2
  exit 3
fi

export CQL="$CQL_TEXT"
export CONTACT_HOST="${CONTACT_HOST:-127.0.0.1}"
export CONTACT_PORT="${CONTACT_PORT:-9042}"
export LOCAL_DC="${LOCAL_DC:-datacenter1}"

exec jshell -q --class-path "$JAR" "$HERE/cql.jsh"
