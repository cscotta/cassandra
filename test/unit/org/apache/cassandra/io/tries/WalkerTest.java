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

package org.apache.cassandra.io.tries;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.LongSupplier;
import java.util.function.LongToIntFunction;
import java.util.stream.IntStream;

import org.agrona.collections.IntArrayList;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import org.apache.cassandra.io.sstable.format.Version;
import org.apache.cassandra.io.sstable.format.bti.BtiFormat;
import org.apache.cassandra.io.util.ChannelProxy;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.io.util.Rebufferer;
import org.apache.cassandra.io.util.TailOverridingRebufferer;
import org.apache.cassandra.utils.bytecomparable.ByteComparable;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"unchecked", "RedundantSuppression"})
public class WalkerTest extends AbstractTrieTestBase
{
    @Test
    public void testWalker() throws IOException
    {
        DataOutputBuffer buf = new DataOutputBufferPaged();
        IncrementalTrieWriter<Integer> builder = makeTrie(buf);
        long rootPos = builder.complete();

        Rebufferer source = new ByteBufRebufferer(buf.asNewBuffer());

        Walker<?> it = new Walker<>(source, rootPos);

        DataOutputBuffer dumpBuf = new DataOutputBuffer();
        Version sstableVersion = new BtiFormat(null).getLatestVersion();
        it.dumpTrie(new PrintStream(dumpBuf), (buf1, payloadPos, payloadFlags, version) -> String.format("%d/%d", payloadPos, payloadFlags), sstableVersion);
        logger.info("Trie dump: \n{}", new String(dumpBuf.getData()));
        logger.info("Trie toString: {}", it);

        it.goMax(rootPos);
        assertEquals(12, it.payloadFlags());
        assertEquals(TrieNode.Types.PAYLOAD_ONLY.ordinal, it.nodeTypeOrdinal());
        assertEquals(1, it.nodeSize());
        assertFalse(it.hasChildren());

        it.goMin(rootPos);
        assertEquals(1, it.payloadFlags());
        assertEquals(TrieNode.Types.PAYLOAD_ONLY.ordinal, it.nodeTypeOrdinal());
        assertEquals(1, it.nodeSize());
        assertFalse(it.hasChildren());

        assertEquals(-1, it.follow(source("151")));
        assertEquals(2, it.payloadFlags());

        assertEquals('3', it.follow(source("135")));

        assertEquals('3', it.followWithGreater(source("135")));
        it.goMin(it.greaterBranch);
        assertEquals(2, it.payloadFlags());

        assertEquals('3', it.followWithLesser(source("135")));
        it.goMax(it.lesserBranch);
        assertEquals(1, it.payloadFlags());

        assertEquals(3, (Object) it.prefix(source("155"), (walker, payloadPosition, payloadFlags) -> payloadFlags));
        assertNull(it.prefix(source("516"), (walker, payloadPosition, payloadFlags) -> payloadFlags));
        assertEquals(5, (Object) it.prefix(source("5151"), (walker, payloadPosition, payloadFlags) -> payloadFlags));
        assertEquals(1, (Object) it.prefix(source("1151"), (walker, payloadPosition, payloadFlags) -> payloadFlags));

        assertEquals(3, (Object) it.prefixAndNeighbours(source("155"), (walker, payloadPosition, payloadFlags) -> payloadFlags));
        assertNull(it.prefixAndNeighbours(source("516"), (walker, payloadPosition, payloadFlags) -> payloadFlags));
        assertEquals(5, (Object) it.prefixAndNeighbours(source("5151"), (walker, payloadPosition, payloadFlags) -> payloadFlags));
        assertEquals(1, (Object) it.prefixAndNeighbours(source("1151"), (walker, payloadPosition, payloadFlags) -> payloadFlags));

        assertEquals(3, (Object) it.prefixAndNeighbours(source("1555"), (walker, payloadPosition, payloadFlags) -> payloadFlags));
        it.goMax(it.lesserBranch);
        assertEquals(2, it.payloadFlags());
        it.goMin(it.greaterBranch);
        assertEquals(4, it.payloadFlags());
    }

    @Test
    public void testIteratorWithoutBounds() throws IOException
    {
        DataOutputBuffer buf = new DataOutputBufferPaged();
        IncrementalTrieWriter<Integer> builder = makeTrie(buf);
        long rootPos = builder.complete();

        int[] expected = IntStream.range(1, 13).toArray();
        checkIterates(buf.asNewBuffer(), rootPos, expected);

        checkIterates(buf.asNewBuffer(), rootPos, null, null, false, expected);
        checkIterates(buf.asNewBuffer(), rootPos, null, null, true, expected);
    }

    @Test
    public void testIteratorWithBounds() throws IOException
    {
        DataOutputBuffer buf = new DataOutputBufferPaged();
        IncrementalTrieWriter<Integer> builder = makeTrie(buf);
        long rootPos = builder.complete();

        checkIterates(buf.asNewBuffer(), rootPos, "151", "515", false, 3, 4, 5);
        checkIterates(buf.asNewBuffer(), rootPos, "15151", "51515", false, 3, 4, 5);
        checkIterates(buf.asNewBuffer(), rootPos, "705", "73", false, 9, 10, 11);
        checkIterates(buf.asNewBuffer(), rootPos, "7051", "735", false, 10, 11);
        checkIterates(buf.asNewBuffer(), rootPos, "70", "737", false, 9, 10, 11, 12);
        checkIterates(buf.asNewBuffer(), rootPos, "7054", "735", false, 10, 11);

        checkIterates(buf.asNewBuffer(), rootPos, null, "515", false, 1, 2, 3, 4, 5);
        checkIterates(buf.asNewBuffer(), rootPos, null, "51515", false, 1, 2, 3, 4, 5);
        checkIterates(buf.asNewBuffer(), rootPos, "151", null, false, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        checkIterates(buf.asNewBuffer(), rootPos, "15151", null, false, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    }

    @Test
    public void testIteratorWithBoundsAndAdmitPrefix() throws IOException
    {
        DataOutputBuffer buf = new DataOutputBufferPaged();
        IncrementalTrieWriter<Integer> builder = makeTrie(buf);
        long rootPos = builder.complete();

        checkIterates(buf.asNewBuffer(), rootPos, "151", "515", true, 2, 3, 4, 5);
        checkIterates(buf.asNewBuffer(), rootPos, "15151", "51515", true, 2, 3, 4, 5);
        checkIterates(buf.asNewBuffer(), rootPos, "705", "73", true, 8, 9, 10, 11);
        checkIterates(buf.asNewBuffer(), rootPos, "7051", "735", true, 9, 10, 11);
        checkIterates(buf.asNewBuffer(), rootPos, "70", "737", true, 8, 9, 10, 11, 12);
        // Note: 7054 has 70 as prefix, but we don't include 70 because a clearly smaller non-prefix entry 7051
        // exists between the bound and the prefix
        checkIterates(buf.asNewBuffer(), rootPos, "7054", "735", true, 10, 11);


        checkIterates(buf.asNewBuffer(), rootPos, null, "515", true, 1, 2, 3, 4, 5);
        checkIterates(buf.asNewBuffer(), rootPos, null, "51515", true, 1, 2, 3, 4, 5);
        checkIterates(buf.asNewBuffer(), rootPos, "151", null, true, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        checkIterates(buf.asNewBuffer(), rootPos, "15151", null, true, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        checkIterates(buf.asNewBuffer(), rootPos, "7054", null, true, 10, 11, 12);
    }

    private void checkIterates(ByteBuffer buffer, long rootPos, String from, String to, boolean admitPrefix, int... expected)
    {
        Rebufferer source = new ByteBufRebufferer(buffer);
        ValueIterator<?> it = new ValueIterator<>(source, rootPos, source(from), source(to), admitPrefix);
        checkReturns(from + "-->" + to, it::nextPayloadedNode, pos -> getPayloadFlags(buffer, (int) pos), expected);

        ReverseValueIterator<?> rit = new ReverseValueIterator<>(source, rootPos, source(from), source(to), admitPrefix);
        reverse(expected);
        checkReturns(from + "<--" + to, rit::nextPayloadedNode, pos -> getPayloadFlags(buffer, (int) pos), expected);
        reverse(expected);  // return array in its original form if reused
    }

    private void checkIterates(ByteBuffer buffer, long rootPos, int... expected)
    {
        Rebufferer source = new ByteBufRebufferer(buffer);
        ValueIterator<?> it = new ValueIterator<>(source, rootPos);
        checkReturns("Forward", it::nextPayloadedNode, pos -> getPayloadFlags(buffer, (int) pos), expected);

        ReverseValueIterator<?> rit = new ReverseValueIterator<>(source, rootPos);
        reverse(expected);
        checkReturns("Reverse", rit::nextPayloadedNode, pos -> getPayloadFlags(buffer, (int) pos), expected);
        reverse(expected);  // return array in its original form if reused
    }

    private void reverse(int[] expected)
    {
        final int size = expected.length;
        for (int i = 0; i < size / 2; ++i)
        {
            int t = expected[i];
            expected[i] = expected[size - 1 - i];
            expected[size - i - 1] = t;
        }
    }

    private int getPayloadFlags(ByteBuffer buffer, int pos)
    {
        return TrieNode.at(buffer, pos).payloadFlags(buffer, pos);
    }

    private void checkReturns(String testCase, LongSupplier supplier, LongToIntFunction mapper, int... expected)
    {
        IntArrayList list = new IntArrayList();
        while (true)
        {
            long pos = supplier.getAsLong();
            if (pos == Walker.NONE)
                break;
            list.add(mapper.applyAsInt(pos));
        }
        assertArrayEquals(testCase + ": " + list + " != " + Arrays.toString(expected), expected, list.toIntArray());
    }

    @Test
    public void testPartialTail() throws IOException
    {
        DataOutputBuffer buf = new DataOutputBufferPaged();
        IncrementalTrieWriter<Integer> builder = makeTrie(buf);
        IncrementalTrieWriter.PartialTail ptail = builder.makePartialRoot();
        long rootPos = builder.complete();
        try (Rebufferer source = new ByteBufRebufferer(buf.asNewBuffer());
             Rebufferer partialSource = new TailOverridingRebufferer(new ByteBufRebufferer(buf.asNewBuffer()), ptail.cutoff(), ptail.tail());
             ValueIterator<?> it = new ValueIterator<>(new ByteBufRebufferer(buf.asNewBuffer()), rootPos, source("151"), source("515"), true);
             ValueIterator<?> tailIt = new ValueIterator<>(new TailOverridingRebufferer(new ByteBufRebufferer(buf.asNewBuffer()), ptail.cutoff(), ptail.tail()), ptail.root(), source("151"), source("515"), true))
        {
            while (true)
            {
                long i1 = it.nextPayloadedNode();
                long i2 = tailIt.nextPayloadedNode();
                if (i1 == -1 || i2 == -1)
                    break;

                Rebufferer.BufferHolder bh1 = source.rebuffer(i1);
                Rebufferer.BufferHolder bh2 = partialSource.rebuffer(i2);

                int f1 = TrieNode.at(bh1.buffer(), (int) (i1 - bh1.offset())).payloadFlags(bh1.buffer(), (int) (i1 - bh1.offset()));
                int f2 = TrieNode.at(bh2.buffer(), (int) (i2 - bh2.offset())).payloadFlags(bh2.buffer(), (int) (i2 - bh2.offset()));
                assertEquals(f1, f2);

                bh2.release();
                bh1.release();
            }
        }
    }

    @Test
    public void testBigTrie() throws IOException
    {
        DataOutputBuffer buf = new DataOutputBufferPaged();
        IncrementalTrieWriter<Integer> builder = newTrieWriter(serializer, buf);
        payloadSize = 0;
        makeBigTrie(builder);
        builder.reset();
        payloadSize = 200;
        makeBigTrie(builder);

        long rootPos = builder.complete();
        Rebufferer source = new ByteBufRebufferer(buf.asNewBuffer());
        ValueIterator<?> it = new ValueIterator<>(source, rootPos);

        while (true)
        {
            long i1 = it.nextPayloadedNode();
            if (i1 == -1)
                break;

            TrieNode node = TrieNode.at(buf.asNewBuffer(), (int) i1);
            assertNotEquals(0, node.payloadFlags(buf.asNewBuffer(), (int) i1));
        }
    }


    /**
     * A walker only reads at absolute positions, so it must take the buffer holder's shared buffer (the mapped
     * region's own buffer) instead of asking for a private copy that a position-mutating reader would need.
     */
    @Test
    public void testWalkerReadsTheSharedBuffer() throws IOException
    {
        DataOutputBuffer buf = new DataOutputBufferPaged();
        long rootPos = makeTrie(buf).complete();

        try (Walker<?> walker = new Walker<>(new SharedOnlyRebufferer(buf.asNewBuffer()), rootPos))
        {
            walker.goMin(rootPos);
            assertEquals(1, walker.payloadFlags());
            walker.goMax(rootPos);
            assertEquals(12, walker.payloadFlags());
        }
    }

    /**
     * The walker must take the shared buffer in {@code go()} as well, i.e. not only when it is created. The first
     * window only covers the root node, so walking the trie from the root has to re-buffer.
     */
    @Test
    public void testWalkerReadsTheSharedBufferAfterRebuffer() throws IOException
    {
        DataOutputBuffer buf = new DataOutputBufferPaged();
        long rootPos = makeTrie(buf).complete();

        RootOnlyFirstWindowRebufferer source = new RootOnlyFirstWindowRebufferer(buf.asNewBuffer(), rootPos);
        try (Walker<?> walker = new Walker<>(source, rootPos))
        {
            walker.goMin(rootPos);
            assertEquals(1, walker.payloadFlags());
            walker.goMax(rootPos);
            assertEquals(12, walker.payloadFlags());
        }
        assertTrue("the walk must re-buffer, not only use the buffer from the constructor", source.rebufferCalls > 1);
    }

    /**
     * Serves a window that contains just the root node on the first call, and the whole trie after that. It only
     * implements {@link Rebufferer.BufferHolder#sharedBuffer()}, so moving off the root fails unless the walker asks for
     * the shared buffer there too.
     */
    private static class RootOnlyFirstWindowRebufferer implements Rebufferer
    {
        private final ByteBuffer data;
        private final int root;
        int rebufferCalls;

        RootOnlyFirstWindowRebufferer(ByteBuffer data, long root)
        {
            this.data = data;
            this.root = (int) root;
        }

        public BufferHolder rebuffer(long position)
        {
            if (++rebufferCalls == 1)
            {
                ByteBuffer rootOnly = data.duplicate();
                rootOnly.position(root).limit(data.limit());
                return new WindowHolder(root, rootOnly.slice());
            }

            return new WindowHolder(0, data);
        }

        public ChannelProxy channel()
        {
            return null;
        }

        public long fileLength()
        {
            return data.limit();
        }

        public double getCrcCheckChance()
        {
            return 0;
        }

        public void close()
        {
        }

        public void closeReader()
        {
        }

        private static class WindowHolder implements Rebufferer.BufferHolder
        {
            private final long offset;
            private final ByteBuffer window;

            WindowHolder(long offset, ByteBuffer window)
            {
                this.offset = offset;
                this.window = window;
            }

            public ByteBuffer buffer()
            {
                throw new AssertionError("the walker must not ask for a private copy of the buffer");
            }

            public ByteBuffer sharedBuffer()
            {
                return window;
            }

            public long offset()
            {
                return offset;
            }

            public void release()
            {
            }
        }
    }

    private static class SharedOnlyRebufferer implements Rebufferer, Rebufferer.BufferHolder
    {
        private final ByteBuffer buffer;

        SharedOnlyRebufferer(ByteBuffer buffer)
        {
            this.buffer = buffer;
        }

        public ByteBuffer buffer()
        {
            throw new AssertionError("the walker must not ask for a private copy of the buffer");
        }

        public ByteBuffer sharedBuffer()
        {
            return buffer;
        }

        public long offset()
        {
            return 0;
        }

        public void release()
        {
        }

        public BufferHolder rebuffer(long position)
        {
            return this;
        }

        public ChannelProxy channel()
        {
            return null;
        }

        public long fileLength()
        {
            return buffer.remaining();
        }

        public double getCrcCheckChance()
        {
            return 0;
        }

        public void close()
        {
        }

        public void closeReader()
        {
        }
    }

    private IncrementalTrieWriter<Integer> makeTrie(DataOutputBuffer out) throws IOException
    {
        IncrementalTrieWriter<Integer> builder = newTrieWriter(serializer, out);
        dump = true;
        builder.add(source("115"), 1);
        builder.add(source("151"), 2);
        builder.add(source("155"), 3);
        builder.add(source("511"), 4);
        builder.add(source("515"), 5);
        builder.add(source("551"), 6);
        builder.add(source("555555555555555555555555555555555555555555555555555555555555555555"), 7);

        builder.add(source("70"), 8);
        builder.add(source("7051"), 9);
        builder.add(source("717"), 10);
        builder.add(source("73"), 11);
        builder.add(source("737"), 12);
        return builder;
    }

    private void makeBigTrie(IncrementalTrieWriter<Integer> builder) throws IOException
    {
        dump = false;
        for (int shift = 0; shift < 8; shift++)
            for (long i = 1; i < 80; i++)
                builder.add(longSource(i, shift * 8, 100), (int) (i % 7) + 1);
    }

    private ByteComparable longSource(long l, int shift, int size)
    {
        String s = StringUtils.leftPad(toBase(l), 8, '0');
        s = StringUtils.rightPad(s, 8 + shift, '0');
        s = StringUtils.leftPad(s, size, '0');
        return source(s);
    }
}
