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

// jmx_hist.jsh -- OPTIONAL cross-check of Cassandra's per-read histograms over
// JMX, using pure JDK (jshell), no third-party deps and no cqlsh/Python driver.
//
// Reads ks/tbl/host/port from environment variables (jshell scripts take no
// argv), connects to the node's local JMX RMI endpoint, and prints one
// "HIST name=<Metric> count=.. max=.. p50=.. p99=.." line per histogram.
// On any failure it prints a JMX_ERROR / HIST_ERR line and exits cleanly so the
// orchestrator can skip gracefully -- the eBPF SUMMARY and io_operations.log are
// the authoritative sources; JMX is only a confirmation.
//
// Invoke via tools/jmx_hist.sh <ks> <tbl> [host] [port]  (default 127.0.0.1:7100).

import javax.management.*;
import javax.management.remote.*;

String host = System.getenv().getOrDefault("JMX_HOST", "127.0.0.1");
String port = System.getenv().getOrDefault("JMX_PORT", "7100");
String ks   = System.getenv().getOrDefault("KS", "");
String tbl  = System.getenv().getOrDefault("TBL", "");

String[] metrics = {
    "ReadIopsPerRead", "BytesReadPerRead", "FilesTouchedPerRead",
    "BytesDecompressedPerRead", "ChunkCacheHitsPerRead", "ChunkCacheMissesPerRead"
};

try {
    JMXServiceURL url = new JMXServiceURL(
        "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi");
    JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
    for (String m : metrics) {
        try {
            ObjectName on = new ObjectName(
                "org.apache.cassandra.metrics:type=Table,keyspace=" + ks +
                ",scope=" + tbl + ",name=" + m);
            Object cnt = mbsc.getAttribute(on, "Count");
            Object max = mbsc.getAttribute(on, "Max");
            Object p50 = mbsc.getAttribute(on, "50thPercentile");
            Object p99 = mbsc.getAttribute(on, "99thPercentile");
            System.out.println("HIST name=" + m + " count=" + cnt +
                               " max=" + max + " p50=" + p50 + " p99=" + p99);
        } catch (Exception e) {
            System.out.println("HIST_ERR name=" + m + " err=" +
                               e.getClass().getSimpleName());
        }
    }
    jmxc.close();
} catch (Exception e) {
    System.out.println("JMX_ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage());
}

/exit
