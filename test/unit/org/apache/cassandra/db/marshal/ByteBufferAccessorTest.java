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

package org.apache.cassandra.db.marshal;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.utils.ByteBufferUtil;

import static org.quicktheories.QuickTheory.qt;

public class ByteBufferAccessorTest extends ValueAccessorTester
{

    private static final TestNativeDataAllocator allocator = new TestNativeDataAllocator();
    @BeforeClass
    public static void setSetMemoryAllocator()
    {
        NativeAccessor.setNativeMemoryAllocator(allocator);
    }

    @AfterClass
    public static void releaseMemory()
    {
        allocator.close();
    }


    private static byte[] array(int start, int size)
    {
        byte[] a = new byte[size];
        for (int i = 0; i < size; i++)
            a[i] = (byte) (start + i);
        return a;
    }

    private <V> void testCopyFromOffsets(ValueAccessor<V> dstAccessor, int padding1, int padding2)
    {
        ByteBuffer src = leftPad(ByteBuffer.wrap(array(0, 10)), padding1);
        src.position(src.position() + 5);
        Assert.assertEquals(5, src.remaining());

        V dst = leftPad(dstAccessor.allocate(5), padding2);
        ByteBufferAccessor.instance.copyTo(src, 0, dst, dstAccessor, 0, 5);
        Assert.assertArrayEquals(array(5, 5), dstAccessor.toArray(dst));
    }

    /**
     * Test byte buffers with position > 0 are copied correctly
     */
    @Test
    public void testCopyFromOffets()
    {
        qt().forAll(accessors(), bbPadding(), bbPadding())
            .checkAssert(this::testCopyFromOffsets);
    }

    private <V> void testCopyToOffsets(ValueAccessor<V> srcAccessor, int padding1, int padding2)
    {
        byte[] value = array(5, 5);
        V src = leftPad(srcAccessor.allocate(5), padding1);
        ByteArrayAccessor.instance.copyTo(value, 0, src, srcAccessor, 0, value.length);

        ByteBuffer bb = leftPad(ByteBuffer.wrap(new byte[10]), padding2);
        ByteBuffer actual = bb.duplicate();
        bb.position(bb.position() + 5);
        srcAccessor.copyTo(src, 0, bb, ByteBufferAccessor.instance, 0, value.length);

        byte[] expected = new byte[10];
        System.arraycopy(value, 0, expected, 5, 5);
        Assert.assertArrayEquals(srcAccessor.getClass().getSimpleName(), expected, ByteBufferUtil.getArray(actual));
    }

    @Test
    public void testCopyToOffsets()
    {
        qt().forAll(accessors(), bbPadding(), bbPadding())
            .checkAssert(this::testCopyToOffsets);
    }

    /**
     * {@link ByteBufferAccessor#write(ByteBuffer, ByteBuffer)} must copy the remaining bytes of the source
     * into the destination at its current position and advance only the destination's position, for heap and
     * direct buffers alike.
     */
    @Test
    public void testWriteByteBuffer()
    {
        for (boolean srcDirect : new boolean[]{ false, true })
        {
            for (boolean dstDirect : new boolean[]{ false, true })
            {
                ByteBuffer src = srcDirect ? ByteBuffer.allocateDirect(7) : ByteBuffer.allocate(7);
                src.put((byte) 1);
                src.put(array(5, 5));
                src.put((byte) 2);
                src.position(1);
                src.limit(6);

                ByteBuffer dst = dstDirect ? ByteBuffer.allocateDirect(9) : ByteBuffer.allocate(9);
                dst.position(3);
                dst.limit(8);

                ByteBufferAccessor.instance.write(src, dst);

                Assert.assertEquals("the source position must not be consumed", 1, src.position());
                Assert.assertEquals("the destination position must advance by the copied length", 8, dst.position());

                byte[] expected = new byte[]{ 0, 0, 0, 5, 6, 7, 8, 9, 0 };
                byte[] actual = new byte[9];
                dst.duplicate().clear().get(actual);
                Assert.assertArrayEquals(expected, actual);
            }
        }
    }

    @Test
    public void testWriteEmptyValue()
    {
        ByteBuffer src = ByteBuffer.allocate(4);
        src.position(4);
        ByteBuffer dst = ByteBuffer.allocateDirect(4);
        ByteBufferAccessor.instance.write(src, dst);
        Assert.assertEquals(0, dst.position());
    }

    @Test(expected = BufferOverflowException.class)
    public void testWriteBeyondDestination()
    {
        ByteBufferAccessor.instance.write(ByteBuffer.wrap(array(0, 5)), ByteBuffer.allocate(4));
    }

    @Test(expected = ReadOnlyBufferException.class)
    public void testWriteToReadOnlyDestination()
    {
        ByteBufferAccessor.instance.write(ByteBuffer.wrap(array(0, 2)), ByteBuffer.allocate(4).asReadOnlyBuffer());
    }

    /**
     * A read-only heap buffer has no accessible array and no address; the copy must not treat it as direct.
     */
    @Test
    public void testWriteFromReadOnlyHeapValue()
    {
        ByteBuffer value = ByteBuffer.wrap(array(5, 5)).asReadOnlyBuffer();
        ByteBuffer out = ByteBuffer.allocate(5);
        ByteBufferAccessor.instance.write(value, out);
        Assert.assertEquals(5, out.position());
        out.flip();
        Assert.assertArrayEquals(array(5, 5), ByteBufferUtil.getArray(out));
        Assert.assertEquals(0, value.position());
    }

    /**
     * {@link ByteBuffer#put(ByteBuffer)} rejects a read-only destination before looking at the source.
     */
    @Test
    public void testWriteToReadOnlyDestinationRejectedBeforeBoundsChecks()
    {
        ByteBuffer[] destinations = new ByteBuffer[]{ ByteBuffer.allocate(4).asReadOnlyBuffer(),
                                                      ByteBuffer.allocateDirect(4).asReadOnlyBuffer() };
        for (ByteBuffer out : destinations)
        {
            try
            {
                ByteBufferAccessor.instance.write(ByteBuffer.allocate(0), out);
                Assert.fail("expected ReadOnlyBufferException for an empty value");
            }
            catch (ReadOnlyBufferException expected)
            {
            }

            try
            {
                ByteBufferAccessor.instance.write(ByteBuffer.wrap(array(0, 5)), out);
                Assert.fail("expected ReadOnlyBufferException when the value does not fit");
            }
            catch (ReadOnlyBufferException expected)
            {
            }
        }
    }

    /**
     * Source and destination may be views of the same heap array; the copy must behave like System.arraycopy.
     */
    @Test
    public void testWriteSharedHeapArrayIsOverlapSafe()
    {
        byte[] array = new byte[]{ 0, 1, 2, 3, 4, 5, 6, 7 };
        ByteBuffer src = ByteBuffer.wrap(array);
        src.limit(4);
        ByteBuffer out = ByteBuffer.wrap(array);
        out.position(2);
        out.limit(6);

        ByteBufferAccessor.instance.write(src, out);

        Assert.assertArrayEquals(new byte[]{ 0, 1, 0, 1, 2, 3, 6, 7 }, array);
        Assert.assertEquals(6, out.position());
        Assert.assertEquals(0, src.position());
    }

    /**
     * Views with a non-zero arrayOffset must copy the intended range.
     */
    @Test
    public void testWriteWithArrayOffsets()
    {
        byte[] srcBacking = new byte[16];
        for (int i = 0; i < srcBacking.length; i++)
            srcBacking[i] = (byte) (10 + i);
        ByteBuffer srcBase = ByteBuffer.wrap(srcBacking);
        srcBase.position(4);
        srcBase.limit(9);
        ByteBuffer src = srcBase.slice();
        Assert.assertEquals(4, src.arrayOffset());
        Assert.assertEquals(5, src.remaining());

        byte[] dstBacking = new byte[12];
        ByteBuffer dstBase = ByteBuffer.wrap(dstBacking);
        dstBase.position(3);
        dstBase.limit(10);
        ByteBuffer dst = dstBase.slice();
        Assert.assertEquals(3, dst.arrayOffset());

        ByteBufferAccessor.instance.write(src, dst);

        Assert.assertEquals(5, dst.position());
        dst.flip();
        Assert.assertArrayEquals(new byte[]{ 14, 15, 16, 17, 18 }, ByteBufferUtil.getArray(dst));
        Assert.assertEquals(0, src.position());
    }

    /**
     * {@link org.apache.cassandra.utils.memory.ByteBufferCloner#clone} relies on out's mark surviving the copy.
     */
    @Test
    public void testWritePreservesDestinationMark()
    {
        ByteBuffer out = ByteBuffer.allocate(8);
        out.position(2);
        out.mark();
        ByteBufferAccessor.instance.write(ByteBuffer.wrap(array(5, 3)), out);
        out.reset();
        Assert.assertEquals(2, out.position());
        Assert.assertEquals(6, out.remaining());
        Assert.assertArrayEquals(new byte[]{ 5, 6, 7, 0, 0, 0 }, ByteBufferUtil.getArray(out));
    }
}
