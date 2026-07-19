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

import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IoOperationsLogTest
{
    private static final String SAMPLE = "/var/lib/cassandra/data/ks1/tbl1-abcdef0123456789/oa-17-big-Data.db";

    @Test
    public void parsesComponentKeyspaceAndTable()
    {
        assertEquals("Data.db", IoOperationsLog.component(SAMPLE));
        assertEquals("ks1", IoOperationsLog.keyspaceOf(SAMPLE));
        assertEquals("tbl1", IoOperationsLog.tableOf(SAMPLE));

        assertEquals("CompressionInfo.db",
                     IoOperationsLog.component("/d/ks/tbl-id/oa-1-big-CompressionInfo.db"));
        // Robust to unexpected shapes.
        assertEquals("plain.db", IoOperationsLog.component("plain.db"));
        assertEquals("", IoOperationsLog.keyspaceOf("plain.db"));
        assertEquals("", IoOperationsLog.tableOf(null));
    }

    @Test
    public void offWhenLevelOffAndEmitsStructuredLineWhenTrace()
    {
        Logger l = (Logger) LoggerFactory.getLogger("io_operations");
        Level original = l.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        l.addAppender(appender);
        try
        {
            l.setLevel(Level.OFF);
            assertFalse(IoOperationsLog.isEnabled());
            IoOperationsLog.logRead(SAMPLE, 0, 4096); // dropped: level OFF
            assertEquals(0, appender.list.size());

            l.setLevel(Level.TRACE);
            assertTrue(IoOperationsLog.isEnabled());
            IoOperationsLog.logRead(SAMPLE, 128, 4096);
            IoOperationsLog.logWrite(SAMPLE, 65536, 512);
            assertEquals(2, appender.list.size());

            String read = appender.list.get(0).getFormattedMessage();
            assertTrue(read, read.contains("op=r"));
            assertTrue(read, read.contains("ks=ks1"));
            assertTrue(read, read.contains("tbl=tbl1"));
            assertTrue(read, read.contains("comp=Data.db"));
            assertTrue(read, read.contains("off=128"));
            assertTrue(read, read.contains("len=4096"));

            String write = appender.list.get(1).getFormattedMessage();
            assertTrue(write, write.contains("op=w"));
            assertTrue(write, write.contains("off=65536"));
            assertTrue(write, write.contains("len=512"));
        }
        finally
        {
            l.detachAppender(appender);
            l.setLevel(original);
        }
    }
}
