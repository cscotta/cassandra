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

package org.apache.cassandra.distributed.test;

import java.nio.ByteBuffer;

import com.codahale.metrics.Meter;

import org.junit.Test;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.distributed.Cluster;
import org.apache.cassandra.io.util.AsyncReadProviders;
import org.apache.cassandra.metrics.CassandraMetricsRegistry;
import org.apache.cassandra.metrics.DefaultNameFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * End-to-end validation that {@code disk_access_mode: io_uring} serves real reads on a live node. Self-skips unless a
 * usable io_uring provider is present (Linux + JDK 25 + kernel support). A single node writes rows across two flushes,
 * then a full scan forces data-file chunk reads (chunk-cache misses) through the io_uring reader; the test asserts the
 * data reads back correctly, that the node actually resolved {@code io_uring} (no startup fallback), and that the ring
 * saw submissions (proving reads flowed through io_uring rather than the FileChannel fallback).
 */
public class IoUringReadPathTest extends TestBaseImpl
{
    @Test
    public void readsFlowThroughIoUring() throws Exception
    {
        assumeTrue("io_uring unavailable on this host", AsyncReadProviders.get().isAvailable());

        try (Cluster cluster = init(Cluster.build(1)
                                           .withConfig(c -> c.set("disk_access_mode", "io_uring"))
                                           .start()))
        {
            cluster.schemaChange(withKeyspace("CREATE TABLE %s.t (k int PRIMARY KEY, v blob)"));

            int rows = 500;
            ByteBuffer blob = ByteBuffer.wrap(new byte[512]);
            for (int i = 0; i < rows; i++)
            {
                cluster.get(1).executeInternal(withKeyspace("INSERT INTO %s.t (k, v) VALUES (?, ?)"), i, blob.duplicate());
                if (i == rows / 2)
                    cluster.get(1).flush(KEYSPACE);   // two sstables, so the scan spans multiple data files
            }
            cluster.get(1).flush(KEYSPACE);

            // Full scan reads every data-file chunk from the flushed sstables -> chunk-cache misses -> io_uring reads.
            Object[][] result = cluster.get(1).executeInternal(withKeyspace("SELECT k FROM %s.t"));
            assertThat(result.length).isEqualTo(rows);

            // The node actually resolved io_uring (the startup availability check did not fall back to standard).
            String mode = cluster.get(1).callOnInstance(() -> DatabaseDescriptor.getDiskAccessMode().name());
            assertThat(mode).describedAs("resolved disk_access_mode").isEqualTo("io_uring");

            // Reads flowed through the ring: the io_uring Submissions meter advanced during the scan.
            long submissions = cluster.get(1).callOnInstance(() -> {
                CassandraMetricsRegistry.MetricName name = new DefaultNameFactory("IoUring").createMetricName("Submissions");
                Meter meter = CassandraMetricsRegistry.Metrics.getMeters().get(name.getMetricName());
                return meter == null ? 0L : meter.getCount();
            });
            assertThat(submissions).describedAs("io_uring submissions during the read scan").isGreaterThan(0L);
        }
    }
}
