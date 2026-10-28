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

import java.nio.ByteBuffer;

import org.junit.Test;

import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.memory.MemoryUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Int32TypeTest
{
    private static final Int32Type type = Int32Type.instance;

    private static long allocate(ByteBuffer value)
    {
        long address = MemoryUtil.allocate(value.remaining() + 1);
        MemoryUtil.setBytes(address, value);
        return address;
    }

    /**
     * {@code compareNative} must sort every pair of valid ints exactly as the materialized-view path does,
     * including the negatives whose ordering the byte-wise comparison derives from the sign bit.
     */
    @Test
    public void testCompareNativeMatchesMaterializedViews()
    {
        int[] values = { Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -2000000000, -65536, -32769, -32768, -256, -129,
                         -128, -2, -1, 0, 1, 2, 127, 128, 255, 256, 32767, 32768, 65535, 65536,
                         2000000000, Integer.MAX_VALUE - 1, Integer.MAX_VALUE };
        long[] addresses = new long[values.length];
        for (int i = 0; i < values.length; i++)
            addresses[i] = allocate(ByteBufferUtil.bytes(values[i]));

        try
        {
            for (int i = 0; i < values.length; i++)
            {
                for (int j = 0; j < values.length; j++)
                {
                    int views = type.compare(new AddressBasedNativeData(addresses[i], 4), NativeAccessor.instance,
                                             new AddressBasedNativeData(addresses[j], 4), NativeAccessor.instance);
                    int nativeCompare = type.compareNative(addresses[i], 4, addresses[j], 4);
                    // the reference is the numeric order, the views must agree with it, and so must the fast path
                    assertEquals(Integer.signum(Integer.compare(values[i], values[j])), Integer.signum(views));
                    assertEquals(Integer.signum(views), Integer.signum(nativeCompare));
                }
            }
        }
        finally
        {
            for (long address : addresses)
                MemoryUtil.free(address);
        }
    }

    @Test
    public void testCompareNativeHandlesEmptyValues()
    {
        long empty = MemoryUtil.allocate(1);
        long value = allocate(ByteBufferUtil.bytes(1));
        try
        {
            // an empty int is not meaningless: it sorts before every value, as compareCustom says
            int views = type.compare(new AddressBasedNativeData(empty, 0), NativeAccessor.instance,
                                     new AddressBasedNativeData(value, 4), NativeAccessor.instance);
            assertTrue(views < 0);
            assertEquals(Integer.signum(views), Integer.signum(type.compareNative(empty, 0, value, 4)));
            assertEquals(Integer.signum(views), -Integer.signum(type.compareNative(value, 4, empty, 0)));
            assertEquals(0, type.compareNative(empty, 0, empty, 0));
        }
        finally
        {
            MemoryUtil.free(empty);
            MemoryUtil.free(value);
        }
    }

    /**
     * The mixed off-heap/heap entry points must agree with the materialized-view path in both
     * directions, for every accessor a value can be held with.
     */
    @Test
    public void testCompareWithNativeMatchesMaterializedViews()
    {
        int[] values = { Integer.MIN_VALUE, -2000000000, -65536, -32769, -128, -1, 0, 1, 127, 128, 32767, 32768, 65536, 2000000000, Integer.MAX_VALUE };
        for (int leftValue : values)
        {
            ByteBuffer leftBuffer = ByteBufferUtil.bytes(leftValue);
            byte[] leftArray = toArray(leftBuffer);
            for (int rightValue : values)
            {
                long rightAddress = allocate(ByteBufferUtil.bytes(rightValue));
                try
                {
                    AddressBasedNativeData right = new AddressBasedNativeData(rightAddress, 4);

                    int expected = type.compare(leftBuffer, ByteBufferAccessor.instance, right, NativeAccessor.instance);
                    assertEquals(Integer.signum(Integer.compare(leftValue, rightValue)), Integer.signum(expected));
                    // heap on the left, off-heap on the right: the same orientation as `expected`
                    assertEquals(Integer.signum(expected), Integer.signum(type.compareToNative(leftBuffer, ByteBufferAccessor.instance, rightAddress, 4)));
                    assertEquals(Integer.signum(expected), Integer.signum(type.compareToNative(leftArray, ByteArrayAccessor.instance, rightAddress, 4)));
                    // off-heap on the left, heap on the right: the reverse orientation, so `expected` negated
                    assertEquals(-Integer.signum(expected), Integer.signum(type.compareNativeTo(rightAddress, 4, leftBuffer, ByteBufferAccessor.instance)));
                    assertEquals(-Integer.signum(expected), Integer.signum(type.compareNativeTo(rightAddress, 4, leftArray, ByteArrayAccessor.instance)));
                }
                finally
                {
                    MemoryUtil.free(rightAddress);
                }
            }
        }
    }

    /**
     * Empty values on either side of a mixed comparison keep the ordering {@code compareCustom}
     * gives them, through both the fast path and the materialized fallback.
     */
    @Test
    public void testCompareWithNativeHandlesEmptyValues()
    {
        long value = allocate(ByteBufferUtil.bytes(-3));
        try
        {
            ByteBuffer emptyBuffer = ByteBufferUtil.EMPTY_BYTE_BUFFER;
            byte[] emptyArray = new byte[0];

            AddressBasedNativeData valueView = new AddressBasedNativeData(value, 4);
            int reference = type.compare(emptyBuffer, ByteBufferAccessor.instance, valueView, NativeAccessor.instance);
            assertTrue(reference < 0);
            assertEquals(-1, Integer.signum(type.compareToNative(emptyBuffer, ByteBufferAccessor.instance, value, 4)));
            assertEquals(-1, Integer.signum(type.compareToNative(emptyArray, ByteArrayAccessor.instance, value, 4)));
            assertEquals(1, Integer.signum(type.compareNativeTo(value, 4, emptyBuffer, ByteBufferAccessor.instance)));
            assertEquals(1, Integer.signum(type.compareNativeTo(value, 4, emptyArray, ByteArrayAccessor.instance)));
            // empty on the off-heap side sorts before a value, exactly as the materialized path says
            int emptyNative = type.compare(new AddressBasedNativeData(value, 0), NativeAccessor.instance, valueView, NativeAccessor.instance);
            assertTrue(emptyNative < 0);
            assertEquals(Integer.signum(emptyNative), Integer.signum(type.compareNativeTo(value, 0, ByteBufferUtil.bytes(7), ByteBufferAccessor.instance)));
        }
        finally
        {
            MemoryUtil.free(value);
        }
    }

    private static byte[] toArray(ByteBuffer buffer)
    {
        byte[] array = new byte[buffer.remaining()];
        buffer.duplicate().get(array);
        return array;
    }

    /**
     * A length that is not four cannot be a valid int; such content keeps the byte-wise ordering of
     * {@code compareCustom} instead of being decoded as an int.
     */
    @Test
    public void testCompareNativeFallsBackForMalformedLengths()
    {
        long four = allocate(ByteBufferUtil.bytes(0x7f000000));
        long five = allocate(ByteBuffer.wrap(new byte[] { 0x7f, 0x00, 0x00, 0x00, 0x01 }));
        try
        {
            int views = type.compare(new AddressBasedNativeData(four, 4), NativeAccessor.instance,
                                     new AddressBasedNativeData(five, 5), NativeAccessor.instance);
            assertEquals(Integer.signum(views), Integer.signum(type.compareNative(four, 4, five, 5)));
        }
        finally
        {
            MemoryUtil.free(four);
            MemoryUtil.free(five);
        }
    }
}
