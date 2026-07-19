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

package org.apache.cassandra.io.util;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import org.apache.cassandra.metrics.ReadIOContext;
import org.apache.cassandra.metrics.ReadIOTracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Exercises the primary read-accounting seam ({@code ChannelProxy.read} -> {@link ReadIOTracker}) end to end
 * against a real file, which is the buffered ({@code standard}) data path used with the chunk cache disabled.
 */
public class ReadIOAccountingTest
{
    @Test
    public void channelProxyReadIsAttributedToTheActiveContext() throws Exception
    {
        Path tmp = Files.createTempFile("readio", ".bin");
        byte[] data = new byte[8192];
        Files.write(tmp, data);

        ChannelProxy cp = new ChannelProxy(tmp.toString());
        ReadIOTracker.setEnabled(true);
        try
        {
            ReadIOTracker.begin();
            ByteBuffer buf = ByteBuffer.allocate(4096);
            int n = cp.read(buf, 0);
            assertEquals(4096, n);

            ReadIOContext ctx = ReadIOTracker.currentOrNull();
            assertNotNull("a context must be bound while tracking is enabled", ctx);
            ReadIOContext.Snapshot s = ctx.snapshot();
            assertEquals(1, s.physicalReadOps);
            assertEquals(4096, s.physicalBytesRead);
            assertEquals(1, s.filesTouched);
        }
        finally
        {
            ReadIOTracker.endAndClear();
            ReadIOTracker.setEnabled(false);
            cp.close();
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void noContextAndNoRecordingWhenDisabled() throws Exception
    {
        Path tmp = Files.createTempFile("readio", ".bin");
        Files.write(tmp, new byte[4096]);

        ChannelProxy cp = new ChannelProxy(tmp.toString());
        ReadIOTracker.setEnabled(false);
        try
        {
            ReadIOTracker.begin(); // no-op when disabled
            assertNull(ReadIOTracker.currentOrNull());
            ByteBuffer buf = ByteBuffer.allocate(4096);
            cp.read(buf, 0); // must not throw and must not record
            assertNull(ReadIOTracker.currentOrNull());
        }
        finally
        {
            ReadIOTracker.endAndClear();
            cp.close();
            Files.deleteIfExists(tmp);
        }
    }
}
