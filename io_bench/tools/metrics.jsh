/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// metrics.jsh -- read all six per-read IO histograms from system_views in a
// SINGLE driver session (one JVM launch, not six), via jshell + the driver in
// the easy-stress fat jar. Reads ks/tbl from the KS/TBL env vars; invoke via
// tools/metrics.sh. This is the server-side cross-check for eBPF/log, and is
// preferred over JMX for metrics.
//
// Emits one marker line per view (values are numbers, no spaces, so orchestrate
// parses them with a simple regex):
//   HIST name=<view> count=<bigint> max=<double> p50=<double> p99=<double>
//   HIST_ERR name=<view> err=<Exception>          (view/query failed)
//   CQL_ERROR <message>                            (could not connect)

import com.datastax.oss.driver.api.core.*;
import com.datastax.oss.driver.api.core.cql.*;
import java.net.InetSocketAddress;

String host = System.getenv().getOrDefault("CONTACT_HOST", "127.0.0.1");
int port = Integer.parseInt(System.getenv().getOrDefault("CONTACT_PORT", "9042"));
String dc = System.getenv().getOrDefault("LOCAL_DC", "datacenter1");
String ks = System.getenv().getOrDefault("KS", "");
String tbl = System.getenv().getOrDefault("TBL", "");

String[] views = {
    "read_iops_per_read", "bytes_read_per_read", "files_touched_per_read",
    "bytes_decompressed_per_read", "chunk_cache_hits_per_read", "chunk_cache_misses_per_read"
};

try (CqlSession session = CqlSession.builder()
        .addContactPoint(new InetSocketAddress(host, port))
        .withLocalDatacenter(dc)
        .build()) {
    for (String v : views) {
        try {
            String q = "SELECT count,max,p50th,p99th FROM system_views." + v +
                       " WHERE keyspace_name='" + ks + "' AND table_name='" + tbl + "'";
            Row row = session.execute(q).one();
            if (row == null) {
                System.out.println("HIST name=" + v + " count=0 max=0.0 p50=0.0 p99=0.0");
            } else {
                System.out.println("HIST name=" + v +
                    " count=" + row.getLong("count") +
                    " max=" + row.getDouble("max") +
                    " p50=" + row.getDouble("p50th") +
                    " p99=" + row.getDouble("p99th"));
            }
        } catch (Exception e) {
            System.out.println("HIST_ERR name=" + v + " err=" + e.getClass().getSimpleName());
        }
    }
} catch (Exception e) {
    System.out.println("CQL_ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage());
}

/exit
