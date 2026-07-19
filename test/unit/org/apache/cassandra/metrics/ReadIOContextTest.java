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

package org.apache.cassandra.metrics;

import org.junit.Test;

import org.apache.cassandra.metrics.ReadIOContext.Snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ReadIOContextTest
{
    @Test
    public void accumulatesReadsBytesAndOps()
    {
        ReadIOContext c = new ReadIOContext();
        c.addPhysicalRead(4096);
        c.addPhysicalRead(8192);
        Snapshot s = c.snapshot();
        assertEquals(2, s.physicalReadOps);
        assertEquals(12288, s.physicalBytesRead);
    }

    @Test
    public void eofReadCountsOpButNotBytes()
    {
        ReadIOContext c = new ReadIOContext();
        c.addPhysicalRead(-1); // FileChannel.read returns -1 at EOF
        Snapshot s = c.snapshot();
        assertEquals(1, s.physicalReadOps);
        assertEquals(0, s.physicalBytesRead);
    }

    @Test
    public void hitsAreAccessesMinusMisses()
    {
        ReadIOContext c = new ReadIOContext();
        for (int i = 0; i < 5; i++)
            c.addChunkCacheAccess();
        c.addChunkCacheMiss();
        c.addChunkCacheMiss();
        Snapshot s = c.snapshot();
        assertEquals(5, s.chunkCacheAccesses);
        assertEquals(2, s.chunkCacheMisses);
        assertEquals(3, s.chunkCacheHits());
    }

    @Test
    public void filesAreDeduplicatedAndBounded()
    {
        ReadIOContext c = new ReadIOContext();
        c.addFile("/a/Data.db");
        c.addFile("/a/Data.db"); // duplicate
        c.addFile("/a/Index.db");
        assertEquals(2, c.distinctFilesTouched());

        for (int i = 0; i < ReadIOContext.MAX_TRACKED_FILES * 2; i++)
            c.addFile("/path/file-" + i + ".db");
        // Distinct count keeps growing (overflow is counted) but the retained set is bounded.
        assertEquals(2 + ReadIOContext.MAX_TRACKED_FILES * 2, c.distinctFilesTouched());
    }

    @Test
    public void deltaIsPerFieldDifferenceClampedAtZero()
    {
        ReadIOContext c = new ReadIOContext();
        c.addPhysicalRead(1000);
        c.addDecompressed(4096);
        Snapshot start = c.snapshot();

        c.addPhysicalRead(500);
        c.addDecompressed(2048);
        c.addFile("/x/Data.db");
        Snapshot delta = c.snapshot().minus(start);

        assertEquals(1, delta.physicalReadOps);
        assertEquals(500, delta.physicalBytesRead);
        assertEquals(2048, delta.bytesDecompressed);
        assertEquals(1, delta.filesTouched);

        // minus(null) returns the snapshot unchanged (used when tracking had no prior baseline).
        Snapshot full = c.snapshot();
        assertSame(full, full.minus(null));
    }

    @Test
    public void zeroMinusLargerClampsAtZero()
    {
        Snapshot small = new ReadIOContext().snapshot();
        ReadIOContext big = new ReadIOContext();
        big.addPhysicalRead(9999);
        Snapshot delta = small.minus(big.snapshot());
        assertEquals(0, delta.physicalBytesRead);
        assertEquals(0, delta.physicalReadOps);
    }
}
