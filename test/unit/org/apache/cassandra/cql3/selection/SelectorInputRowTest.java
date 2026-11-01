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

package org.apache.cassandra.cql3.selection;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.db.rows.BufferCell;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.schema.ColumnMetadata;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class SelectorInputRowTest
{
    private static final byte[] HELLO = "hello".getBytes(StandardCharsets.UTF_8);

    private static ColumnMetadata column;

    @BeforeClass
    public static void setUpClass()
    {
        DatabaseDescriptor.daemonInitialization();
        TableMetadata metadata = TableMetadata.builder("ks", "tbl")
                                              .addPartitionKeyColumn("pk", UTF8Type.instance)
                                              .addRegularColumn("v", UTF8Type.instance)
                                              .build();
        column = metadata.regularColumns().getSimple(0);
    }

    private static Selector.InputRow newRow()
    {
        return new Selector.InputRow(ProtocolVersion.V4, Collections.singletonList(column), false);
    }

    /**
     * A cell whose value owns its backing array is the shape the read path produces (the cells of a response
     * are cloned on the heap), so the result set can take that array over instead of copying it.
     */
    @Test
    public void testValueThatOwnsItsArrayIsShared() throws Exception
    {
        ByteBuffer value = ByteBuffer.allocate(HELLO.length);
        value.put(HELLO).flip();
        Cell<ByteBuffer> cell = BufferCell.live(column, 1L, value);

        Selector.InputRow row = newRow();
        row.add(cell, 0);

        assertArrayEquals(HELLO, row.getValueAsBytes(0));
        assertSame("the value's own array is handed over without a copy", value.array(), row.getValueAsBytes(0));
        assertEquals("hello", ByteBufferUtil.string(row.getValue(0)));
    }

    /**
     * A slice that starts at offset 0 of a larger array satisfies the position and offset requirements of an
     * owned array, but the array is longer than the value, so it must not be handed over.
     */
    @Test
    public void testValueThatStartsASharedArrayIsCopied()
    {
        byte[] shared = "helloyy".getBytes(StandardCharsets.UTF_8);
        ByteBuffer view = ByteBuffer.wrap(shared).duplicate();
        view.limit(HELLO.length);
        ByteBuffer value = view.slice();
        Cell<ByteBuffer> cell = BufferCell.live(column, 1L, value);

        Selector.InputRow row = newRow();
        row.add(cell, 0);

        assertArrayEquals(HELLO, row.getValueAsBytes(0));
        assertNotSame("the array holds more than the value", shared, row.getValueAsBytes(0));
    }

    /**
     * A value that only covers part of a shared array cannot be shared, so it must still be copied.
     */
    @Test
    public void testValueThatSharesItsArrayIsCopied() throws Exception
    {
        byte[] shared = "xxhelloyy".getBytes(StandardCharsets.UTF_8);
        ByteBuffer value = ByteBuffer.wrap(shared);
        value.position(2).limit(2 + HELLO.length);
        Cell<ByteBuffer> cell = BufferCell.live(column, 1L, value);

        Selector.InputRow row = newRow();
        row.add(cell, 0);

        assertArrayEquals(HELLO, row.getValueAsBytes(0));
        assertNotSame("only the value's own bytes may be exposed", shared, row.getValueAsBytes(0));
        assertEquals("hello", ByteBufferUtil.string(row.getValue(0)));
    }
}
