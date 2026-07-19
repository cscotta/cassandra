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

package org.apache.cassandra.distributed.test.metric;

import org.junit.Test;

import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.distributed.Cluster;
import org.apache.cassandra.distributed.api.ConsistencyLevel;
import org.apache.cassandra.distributed.test.TestBaseImpl;
import org.apache.cassandra.metrics.ReadIOTracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end validation that per-request read IO attribution populates the new per-table histograms.
 *
 * Runs with {@code disk_access_mode: standard} (so the partition index reads through {@code ChannelProxy} and is
 * countable) and {@code file_cache_enabled: false} (chunk cache off, so every logical chunk read is a real read).
 * Reads go through the full coordinator path so the {@code StorageProxy.LocalReadRunnable} accounting bracket fires;
 * {@code executeInternal} would bypass it. Under {@code disk_access_mode: auto} only the data path is counted
 * (the index is mmap) — that path is identical to the {@code standard} data path exercised here.
 */
public class ReadIOMetricsTest extends TestBaseImpl
{
    @Test
    public void perReadIoMetricsPopulate() throws Exception
    {
        try (Cluster cluster = init(Cluster.build(1)
                                           .withConfig(c -> {
                                               c.set("disk_access_mode", "standard");
                                               c.set("file_cache_enabled", false);
                                           })
                                           .start()))
        {
            cluster.get(1).runOnInstance(() -> ReadIOTracker.setEnabled(true));

            cluster.schemaChange(withKeyspace("CREATE TABLE %s.t (k int PRIMARY KEY, v text) " +
                                              "WITH compression = {'class':'ZstdCompressor','chunk_length_in_kb':64}"));

            String value = text(1024);
            for (int i = 0; i < 500; i++)
                cluster.coordinator(1).execute(withKeyspace("INSERT INTO %s.t (k, v) VALUES (?, ?)"),
                                               ConsistencyLevel.ONE, i, value);
            cluster.get(1).flush(KEYSPACE);

            // Read a partition through the coordinator path (cold: chunk cache is off) -> ChannelProxy reads.
            Object[][] r = cluster.coordinator(1).execute(withKeyspace("SELECT k, v FROM %s.t WHERE k = ?"),
                                                          ConsistencyLevel.ONE, 42);
            assertEquals(1, r.length);

            long reads = cluster.get(1).callOnInstance(() ->
                Keyspace.open(KEYSPACE).getColumnFamilyStore("t").metric.readIopsPerRead.cf.getCount());
            long maxBytes = cluster.get(1).callOnInstance(() ->
                Keyspace.open(KEYSPACE).getColumnFamilyStore("t").metric.bytesReadPerRead.cf.getSnapshot().getMax());
            long maxDecompressed = cluster.get(1).callOnInstance(() ->
                Keyspace.open(KEYSPACE).getColumnFamilyStore("t").metric.bytesDecompressedPerRead.cf.getSnapshot().getMax());
            long maxFiles = cluster.get(1).callOnInstance(() ->
                Keyspace.open(KEYSPACE).getColumnFamilyStore("t").metric.filesTouchedPerRead.cf.getSnapshot().getMax());

            assertTrue("expected at least one recorded read, got " + reads, reads > 0);
            assertTrue("expected bytes read from disk, got " + maxBytes, maxBytes > 0);
            assertTrue("expected decompressed bytes, got " + maxDecompressed, maxDecompressed > 0);
            assertTrue("expected at least one file touched (Data.db), got " + maxFiles, maxFiles >= 1);
        }
    }

    private static String text(int len)
    {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append((char) ('a' + (i % 26)));
        return sb.toString();
    }
}
