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

package org.apache.cassandra.utils.memory;

import java.nio.ByteBuffer;

import org.junit.Test;

import org.apache.cassandra.db.BufferDecoratedKey;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.dht.Murmur3Partitioner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The key half of {@link EnsureOnHeap}: a key that is already backed by a heap array must be returned as is,
 * because cloning it copies the same bytes again for every source of a read.
 */
public class EnsureOnHeapTest
{
    private static final EnsureOnHeap CLONE_TO_HEAP = new EnsureOnHeap.CloneToHeap();

    private static DecoratedKey keyOf(ByteBuffer bytes)
    {
        return Murmur3Partitioner.instance.decorateKey(bytes);
    }

    @Test
    public void testHeapKeyIsReturnedAsIs()
    {
        ByteBuffer bytes = ByteBuffer.wrap(new byte[]{ 1, 2, 3, 4, 5 });
        DecoratedKey key = keyOf(bytes);

        DecoratedKey onHeap = CLONE_TO_HEAP.applyToPartitionKey(key);

        assertSame(key, onHeap);
        assertEquals(key.getToken(), onHeap.getToken());
        assertEquals(key.getKey(), onHeap.getKey());
    }

    @Test
    public void testHeapViewOfALargerArrayIsCopied()
    {
        // a slice keeps the offset of its parent array, so its bytes may be a window into a recyclable buffer
        ByteBuffer slice = ByteBuffer.wrap(new byte[]{ 9, 1, 2, 3, 4, 9 }, 1, 4).slice();
        DecoratedKey key = keyOf(slice);

        DecoratedKey onHeap = CLONE_TO_HEAP.applyToPartitionKey(key);
        assertNotSame(key, onHeap);
        assertSame(onHeap.getKey(), onHeap.getKey());
        assertEquals(key.getToken(), onHeap.getToken());
        assertEquals(key.getKey(), onHeap.getKey());
    }

    @Test
    public void testHeapKeyWithATrimmedLimitIsCopied()
    {
        ByteBuffer bytes = ByteBuffer.wrap(new byte[]{ 1, 2, 3, 4, 5, 6 });
        bytes.limit(4);
        DecoratedKey key = keyOf(bytes);

        assertNotSame(key, CLONE_TO_HEAP.applyToPartitionKey(key));
    }

    @Test
    public void testReadOnlyHeapKeyIsCopied()
    {
        DecoratedKey key = keyOf(ByteBuffer.wrap(new byte[]{ 1, 2, 3 }).asReadOnlyBuffer());

        DecoratedKey onHeap = CLONE_TO_HEAP.applyToPartitionKey(key);
        assertNotSame(key, onHeap);
        assertTrue(onHeap.getKey().hasArray());
        assertEquals(key.getKey(), onHeap.getKey());
    }

    @Test
    public void testOffHeapKeyIsCopiedToTheHeap()
    {
        ByteBuffer bytes = ByteBuffer.allocateDirect(5);
        bytes.put(new byte[]{ 1, 2, 3, 4, 5 });
        bytes.flip();
        DecoratedKey key = keyOf(bytes);

        DecoratedKey onHeap = CLONE_TO_HEAP.applyToPartitionKey(key);

        assertNotSame(key, onHeap);
        assertFalse(key.getKey().hasArray());
        assertTrue(onHeap.getKey().hasArray());
        assertEquals(key.getToken(), onHeap.getToken());
        assertEquals(key.getKey(), onHeap.getKey());

        // the copy must not follow later changes to the source buffer
        bytes.put(0, (byte) 9);
        assertFalse(key.getKey().equals(onHeap.getKey()));
    }

    @Test
    public void testDirectlyConstructedKeyOverAWrappedArrayIsReturnedAsIs()
    {
        ByteBuffer bytes = ByteBuffer.wrap(new byte[]{ 7, 8, 9 });
        DecoratedKey key = new BufferDecoratedKey(Murmur3Partitioner.instance.getToken(bytes), bytes);

        DecoratedKey onHeap = CLONE_TO_HEAP.applyToPartitionKey(key);

        assertSame(key, onHeap);
        assertSame(bytes, onHeap.getKey());
    }
}
