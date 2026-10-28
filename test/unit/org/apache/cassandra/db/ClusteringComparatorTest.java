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

package org.apache.cassandra.db;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.Int32Type;
import org.apache.cassandra.db.marshal.LongType;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.concurrent.ImmediateFuture;
import org.apache.cassandra.utils.memory.MemtablePool;
import org.apache.cassandra.utils.memory.NativeAllocator;
import org.apache.cassandra.utils.memory.NativePool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

public class ClusteringComparatorTest
{
    @Test
    public void clusteringTypesAreSharedNotCopied()
    {
        AbstractType<?>[] types = { LongType.instance, Int32Type.instance };
        ClusteringComparator comparator = new ClusteringComparator(types);

        assertSame(types, comparator.subtypes());
        assertSame(comparator.subtypes(), comparator.subtypes());
        assertSame(LongType.instance, comparator.subtype(0));

        assertEquals(0, new ClusteringComparator().subtypes().length);
        assertSame(new ClusteringComparator().subtypes(), new ClusteringComparator().subtypes());
    }

    @Ignore("Enable this test if mutation detection is enabled in ClusteringComparator")
    @Test
    public void mutationOfTheSharedClusteringTypesIsDetected()
    {
        AbstractType<?>[] types = { LongType.instance, Int32Type.instance };
        ClusteringComparator comparator = new ClusteringComparator(types);

        types[0] = Int32Type.instance;
        try
        {
            comparator.subtypes();
            throw new RuntimeException("mutating the clustering types of a comparator must be detected");
        }
        catch (AssertionError e)
        {
            // expected
        }

        types[0] = LongType.instance;
        assertSame(types, comparator.subtypes());
    }

    @Test
    public void equalsAndHashCodeAreContentBased()
    {
        ClusteringComparator a = new ClusteringComparator(LongType.instance, Int32Type.instance);
        ClusteringComparator b = new ClusteringComparator(LongType.instance, Int32Type.instance);
        ClusteringComparator different = new ClusteringComparator(LongType.instance);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, different);

        ClusteringComparator empty1 = new ClusteringComparator();
        ClusteringComparator empty2 = new ClusteringComparator();
        assertEquals(empty1, empty2);
        assertEquals(empty1.hashCode(), empty2.hashCode());
    }

    @Test
    public void compareLong()
    {
        ClusteringComparator comparator = new ClusteringComparator(LongType.instance);
        for (int i=0;i<1000; i++) {
            long l1 = ThreadLocalRandom.current().nextLong();
            long l2 = ThreadLocalRandom.current().nextLong();
            int cmp = comparator.compare(
            Clustering.make(ByteBufferUtil.bytes(l1)),
            Clustering.make(ByteBufferUtil.bytes(l2)));
            assertEquals(Long.compare(l1, l2), cmp == 0?0:cmp<0?-1:1);

        }
    }

    @Test
    public void compareRawLong() throws IOException
    {
        AbstractType<?>[] types = {LongType.instance};
        for (int i=0;i<1000; i++) {
            long l1 = ThreadLocalRandom.current().nextLong();
            long l2 = ThreadLocalRandom.current().nextLong();

            int compare = Long.compare(l1, l2);
            int compareCluster = ClusteringComparator.compare(types, clusteringOfLongAsBuffer(types, l1),
                                                        clusteringOfLongAsBuffer(types, l2));
            assertEquals("FFS: l1=" + l1 + ", l2=" + l2,
                         compare,
                         compareCluster == 0?0:compareCluster<0?-1:1);
            assertEquals("FFS: v1=" + l1 + ", v2=" + l2,
                         compare > 0,
                         compareCluster > 0);
            assertEquals("FFS: v1=" + l1 + ", v2=" + l2,
                         compare < 0,
                         compareCluster < 0);
            assertEquals("FFS: v1=" + l1 + ", v2=" + l2,
                         compare == 0,
                         compareCluster == 0);
        }
    }
    @Test
    public void compareRawInt() throws IOException
    {
        AbstractType<?>[] types = { Int32Type.instance};
        for (int i=0;i<1000; i++) {
            int i1 = ThreadLocalRandom.current().nextInt();
            int i2 = ThreadLocalRandom.current().nextInt();

            int compare = Integer.compare(i1, i2);
            int compareCluster = ClusteringComparator.compare(types, clusteringOfIntAsBuffer(types, i1),
                                                       clusteringOfIntAsBuffer(types, i2));
            assertEquals("FFS: v1=" + i1 + ", v2=" + i2,
                         compare > 0,
                         compareCluster > 0);
            assertEquals("FFS: v1=" + i1 + ", v2=" + i2,
                         compare < 0,
                         compareCluster < 0);
            assertEquals("FFS: v1=" + i1 + ", v2=" + i2,
                         compare == 0,
                         compareCluster == 0);
        }
    }

    /**
     * Two off-heap clusterings must order exactly like the equivalent heap clusterings, component by
     * component as well as overall, including negative values (whose ordering comes from the sign bit),
     * empty values and nulls.
     */
    @Test
    public void nativeComparisonMatchesHeapComparison() throws InterruptedException, java.util.concurrent.TimeoutException
    {
        AbstractType<?>[] types = { Int32Type.instance, Int32Type.instance };
        ClusteringComparator comparator = new ClusteringComparator(types);
        MemtablePool pool = new NativePool(1L << 24, 1L << 24, 1.0f, () -> ImmediateFuture.success(false));
        NativeAllocator allocator = (NativeAllocator) pool.newAllocator("test");
        try
        {
            List<Clustering<ByteBuffer>> heap = new ArrayList<>();
            heap.add(Clustering.make(ByteBufferUtil.bytes(Integer.MIN_VALUE), ByteBufferUtil.bytes(-1)));
            heap.add(Clustering.make(ByteBufferUtil.bytes(Integer.MIN_VALUE), ByteBufferUtil.bytes(0)));
            heap.add(Clustering.make(ByteBufferUtil.bytes(-1), ByteBufferUtil.bytes(-1)));
            heap.add(Clustering.make(ByteBufferUtil.bytes(-1), ByteBufferUtil.bytes(Integer.MAX_VALUE)));
            heap.add(Clustering.make(ByteBufferUtil.bytes(0), ByteBufferUtil.bytes(0)));
            heap.add(Clustering.make(ByteBufferUtil.bytes(1), ByteBufferUtil.bytes(0)));
            heap.add(Clustering.make(ByteBufferUtil.bytes(Integer.MAX_VALUE), ByteBufferUtil.bytes(Integer.MAX_VALUE)));
            heap.add(Clustering.make(ByteBufferUtil.EMPTY_BYTE_BUFFER, ByteBufferUtil.bytes(1)));
            heap.add(Clustering.make(ByteBufferUtil.bytes(1), ByteBufferUtil.EMPTY_BYTE_BUFFER));
            heap.add(Clustering.make(ByteBufferUtil.EMPTY_BYTE_BUFFER, ByteBufferUtil.EMPTY_BYTE_BUFFER));
            heap.add(Clustering.make((ByteBuffer) null, ByteBufferUtil.bytes(-1)));
            heap.add(Clustering.make(ByteBufferUtil.bytes(-1), (ByteBuffer) null));
            heap.add(Clustering.make((ByteBuffer) null, (ByteBuffer) null));

            for (Clustering<ByteBuffer> left : heap)
            {
                NativeClustering nativeLeft = new NativeClustering(allocator, null, left);
                for (Clustering<ByteBuffer> right : heap)
                {
                    NativeClustering nativeRight = new NativeClustering(allocator, null, right);

                    int expected = signum(comparator.compare(left, right));
                    // the ClusteringPrefix overload is the one a b-tree search uses, and the only one that
                    // dispatches to the address-based comparison
                    assertEquals(expected, signum(comparator.compare((ClusteringPrefix<?>) nativeLeft,
                                                                   (ClusteringPrefix<?>) nativeRight)));
                    // the Clustering overload keeps the materializing loop: cross-check that both agree
                    assertEquals(expected, signum(comparator.compare(nativeLeft, nativeRight)));
                    // mixed sides: the heap side of a memtable search and the off-heap rows it is compared with
                    assertEquals(expected, signum(comparator.compare((ClusteringPrefix<?>) left,
                                                                   (ClusteringPrefix<?>) nativeRight)));
                    assertEquals(expected, signum(comparator.compare((ClusteringPrefix<?>) nativeLeft,
                                                                   (ClusteringPrefix<?>) right)));

                    for (int i = 0; i < types.length; i++)
                    {
                        int component = comparator.compareComponent(i, nativeLeft.get(i), nativeLeft.accessor(),
                                                                   nativeRight.get(i), nativeRight.accessor());
                        assertEquals(signum(component),
                                     signum(nativeLeft.compareComponent(i, nativeRight, i, types)));
                    }
                }
            }
        }
        finally
        {
            pool.shutdownAndWait(10, TimeUnit.SECONDS);
        }
    }

    /**
     * The same equivalence for clustering columns whose type does not override the address-based
     * comparison (so the default, view-materializing implementation runs), for clusterings of
     * different sizes, and against bounds.
     */
    @Test
    public void nativeComparisonMatchesHeapComparisonForOtherShapes() throws InterruptedException, java.util.concurrent.TimeoutException
    {
        AbstractType<?>[] types = { LongType.instance, Int32Type.instance };
        ClusteringComparator comparator = new ClusteringComparator(types);
        MemtablePool pool = new NativePool(1L << 24, 1L << 24, 1.0f, () -> ImmediateFuture.success(false));
        NativeAllocator allocator = (NativeAllocator) pool.newAllocator("test");
        try
        {
            List<Clustering<ByteBuffer>> heap = new ArrayList<>();
            heap.add(Clustering.make(ByteBufferUtil.bytes(Long.MIN_VALUE), ByteBufferUtil.bytes(-1)));
            heap.add(Clustering.make(ByteBufferUtil.bytes(-1L), ByteBufferUtil.bytes(-1)));
            heap.add(Clustering.make(ByteBufferUtil.bytes(0L), ByteBufferUtil.bytes(0)));
            heap.add(Clustering.make(ByteBufferUtil.bytes(1L), ByteBufferUtil.bytes(0)));
            heap.add(Clustering.make(ByteBufferUtil.bytes(Long.MAX_VALUE), ByteBufferUtil.bytes(Integer.MAX_VALUE)));
            heap.add(Clustering.make(ByteBufferUtil.EMPTY_BYTE_BUFFER, ByteBufferUtil.bytes(0)));
            heap.add(Clustering.make((ByteBuffer) null, ByteBufferUtil.bytes(0)));
            // shorter than the comparator: exercises the size-mismatch tail
            heap.add(Clustering.make(ByteBufferUtil.bytes(0L)));

            List<ClusteringPrefix<?>> bounds = new ArrayList<>();
            bounds.add(ClusteringBound.create(comparator, true, true,
                                              ByteBufferUtil.bytes(0L), ByteBufferUtil.bytes(0)));
            bounds.add(ClusteringBound.create(comparator, false, false,
                                              ByteBufferUtil.bytes(1L), ByteBufferUtil.bytes(0)));

            for (Clustering<ByteBuffer> left : heap)
            {
                NativeClustering nativeLeft = new NativeClustering(allocator, null, left);
                for (Clustering<ByteBuffer> right : heap)
                {
                    NativeClustering nativeRight = new NativeClustering(allocator, null, right);
                    // the ClusteringPrefix overload is the reference here: unlike compare(Clustering, Clustering)
                    // it also orders clusterings of different sizes
                    int expected = signum(comparator.compare((ClusteringPrefix<?>) left, (ClusteringPrefix<?>) right));
                    assertEquals(expected, signum(comparator.compare((ClusteringPrefix<?>) nativeLeft,
                                                                   (ClusteringPrefix<?>) nativeRight)));
                    assertEquals(expected, signum(comparator.compare((ClusteringPrefix<?>) left,
                                                                   (ClusteringPrefix<?>) nativeRight)));
                    assertEquals(expected, signum(comparator.compare((ClusteringPrefix<?>) nativeLeft,
                                                                   (ClusteringPrefix<?>) right)));
                }

                for (ClusteringPrefix<?> bound : bounds)
                {
                    int expected = signum(comparator.compare((ClusteringPrefix<?>) left, bound));
                    assertEquals(expected, signum(comparator.compare((ClusteringPrefix<?>) nativeLeft, bound)));
                    assertEquals(-expected, signum(comparator.compare((ClusteringPrefix<?>) bound, nativeLeft)));
                }
            }
        }
        finally
        {
            pool.shutdownAndWait(10, TimeUnit.SECONDS);
        }
    }

    private static int signum(int value)
    {
        return value == 0 ? 0 : (value < 0 ? -1 : 1);
    }

    private static ByteBuffer clusteringOfLongAsBuffer(AbstractType<?>[] types, long v1) throws IOException
    {
        Clustering<ByteBuffer> clustering = Clustering.make(ByteBufferUtil.bytes(v1));
        DataOutputBuffer out = new DataOutputBuffer();
        Clustering.serializer.serialize(clustering, out, 0, types);
        return out.asNewBuffer();
    }

    private static ByteBuffer clusteringOfIntAsBuffer(AbstractType<?>[] types, int v1) throws IOException
    {
        Clustering<ByteBuffer> clustering = Clustering.make(ByteBufferUtil.bytes(v1));
        DataOutputBuffer out = new DataOutputBuffer();
        Clustering.serializer.serialize(clustering, out, 0, types);
        return out.asNewBuffer();
    }
}
