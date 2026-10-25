/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.db.marshal;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.rows.ArrayCell;
import org.apache.cassandra.db.rows.BufferCell;
import org.apache.cassandra.db.rows.CellPath;
import org.apache.cassandra.db.rows.ComplexColumnData;
import org.apache.cassandra.schema.ColumnMetadata;
import org.apache.cassandra.serializers.CollectionSerializer;
import org.apache.cassandra.serializers.ListSerializer;
import org.apache.cassandra.serializers.MapSerializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.serializers.SetSerializer;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.TimeUUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CollectionTypeTest
{
    @Test
    public void testListComparison()
    {
        ListType<String> lt = ListType.getInstance(UTF8Type.instance, true);

        ByteBuffer[] lists = new ByteBuffer[] {
            ByteBufferUtil.EMPTY_BYTE_BUFFER,
            lt.decompose(ImmutableList.<String>of()),
            lt.decompose(ImmutableList.of("aa")),
            lt.decompose(ImmutableList.of("bb")),
            lt.decompose(ImmutableList.of("bb", "cc")),
            lt.decompose(ImmutableList.of("bb", "dd"))
        };

        for (int i = 0; i < lists.length; i++)
            assertEquals(lt.compare(lists[i], lists[i]), 0);

        for (int i = 0; i < lists.length-1; i++)
        {
            for (int j = i+1; j < lists.length; j++)
            {
                assertEquals(String.format("compare(lists[%d], lists[%d])", i, j), -1, lt.compare(lists[i], lists[j]));
                assertEquals(String.format("compare(lists[%d], lists[%d])", j, i),  1, lt.compare(lists[j], lists[i]));
            }
        }
    }

    @Test
    public void testSetComparison()
    {
        SetType<String> st = SetType.getInstance(UTF8Type.instance, true);

        ByteBuffer[] sets = new ByteBuffer[] {
            ByteBufferUtil.EMPTY_BYTE_BUFFER,
            st.decompose(ImmutableSet.<String>of()),
            st.decompose(ImmutableSet.of("aa")),
            st.decompose(ImmutableSet.of("bb")),
            st.decompose(ImmutableSet.of("bb", "cc")),
            st.decompose(ImmutableSet.of("bb", "dd"))
        };

        for (int i = 0; i < sets.length; i++)
            assertEquals(st.compare(sets[i], sets[i]), 0);

        for (int i = 0; i < sets.length-1; i++)
        {
            for (int j = i+1; j < sets.length; j++)
            {
                assertEquals(String.format("compare(sets[%d], sets[%d])", i, j), -1, st.compare(sets[i], sets[j]));
                assertEquals(String.format("compare(sets[%d], sets[%d])", j, i),  1, st.compare(sets[j], sets[i]));
            }
        }
    }

    @Test
    public void testMapComparison()
    {
        MapType<String, String> mt = MapType.getInstance(UTF8Type.instance, UTF8Type.instance, true);

        ByteBuffer[] maps = new ByteBuffer[] {
            ByteBufferUtil.EMPTY_BYTE_BUFFER,
            mt.decompose(ImmutableMap.<String, String>of()),
            mt.decompose(ImmutableMap.of("aa", "val1")),
            mt.decompose(ImmutableMap.of("aa", "val2")),
            mt.decompose(ImmutableMap.of("bb", "val1")),
            mt.decompose(ImmutableMap.of("bb", "val1", "cc", "val3")),
            mt.decompose(ImmutableMap.of("bb", "val1", "dd", "val3")),
            mt.decompose(ImmutableMap.of("bb", "val1", "dd", "val4"))
        };

        for (int i = 0; i < maps.length; i++)
            assertEquals(mt.compare(maps[i], maps[i]), 0);

        for (int i = 0; i < maps.length-1; i++)
        {
            for (int j = i+1; j < maps.length; j++)
            {
                assertEquals(String.format("compare(maps[%d], maps[%d])", i, j), mt.compare(maps[i], maps[j]), -1);
                assertEquals(String.format("compare(maps[%d], maps[%d])", j, i), mt.compare(maps[j], maps[i]), 1);
            }
        }
    }

    @Test
    public void listSerDerTest()
    {
        ListSerializer<String> sls = ListType.getInstance(UTF8Type.instance, true).getSerializer();
        ListSerializer<Integer> ils = ListType.getInstance(Int32Type.instance, true).getSerializer();

        List<String> sl = Arrays.asList("Foo", "Bar");
        List<Integer> il = Arrays.asList(3, 1, 5);

        ByteBuffer sb = sls.serialize(sl);
        ByteBuffer ib = ils.serialize(il);

        assertEquals(sls.deserialize(sb), sl);
        assertEquals(ils.deserialize(ib), il);

        sls.validate(sb);
        ils.validate(ib);

        // string list with integer list type
        assertInvalid(ils, sb);
        // non list value
        assertInvalid(sls, UTF8Type.instance.getSerializer().serialize("foo"));
    }

    @Test
    public void setSerDerTest()
    {
        SetSerializer<String> sss = SetType.getInstance(UTF8Type.instance, true).getSerializer();
        SetSerializer<Integer> iss = SetType.getInstance(Int32Type.instance, true).getSerializer();

        Set<String> ss = new HashSet(){{ add("Foo"); add("Bar"); }};
        Set<Integer> is = new HashSet(){{ add(3); add(1); add(5); }};

        ByteBuffer sb = sss.serialize(ss);
        ByteBuffer ib = iss.serialize(is);

        assertEquals(sss.deserialize(sb), ss);
        assertEquals(iss.deserialize(ib), is);

        sss.validate(sb);
        iss.validate(ib);

        // string set with integer set type
        assertInvalid(iss, sb);
        // non set value
        assertInvalid(sss, UTF8Type.instance.getSerializer().serialize("foo"));
    }

    @Test
    public void setMapDerTest()
    {
        MapSerializer<String, String> sms = MapType.getInstance(UTF8Type.instance, UTF8Type.instance, true).getSerializer();
        MapSerializer<Integer, Integer> ims = MapType.getInstance(Int32Type.instance, Int32Type.instance, true).getSerializer();

        Map<String, String> sm = new HashMap(){{ put("Foo", "xxx"); put("Bar", "yyy"); }};
        Map<Integer, Integer> im = new HashMap(){{ put(3, 0); put(1, 8); put(5, 2); }};

        ByteBuffer sb = sms.serialize(sm);
        ByteBuffer ib = ims.serialize(im);

        assertEquals(sms.deserialize(sb), sm);
        assertEquals(ims.deserialize(ib), im);

        sms.validate(sb);
        ims.validate(ib);

        // string map with integer map type
        assertInvalid(ims, sb);
        // non map value
        assertInvalid(sms, UTF8Type.instance.getSerializer().serialize("foo"));

        MapSerializer<Integer, String> sims = MapType.getInstance(Int32Type.instance, UTF8Type.instance, true).getSerializer();
        MapSerializer<String, Integer> isms = MapType.getInstance(UTF8Type.instance, Int32Type.instance, true).getSerializer();

        // only key are invalid
        assertInvalid(isms, sb);
        // only values are invalid
        assertInvalid(sims, sb);
    }

    /**
     * The byte array flavoured native protocol serialization must produce the exact bytes of the buffer flavoured
     * one, for both buffer backed and array backed cells (the two representations the read path deserializes).
     */
    @Test
    public void testSerializeForNativeProtocolAsByteArrays()
    {
        // list<int>: the path is the element timeuuid and the cell value is the packed element
        CollectionType<?> list = ListType.getInstance(Int32Type.instance, true);
        List<ByteBuffer> listElements = Arrays.asList(Int32Type.instance.decompose(7),
                                                      Int32Type.instance.decompose(8),
                                                      Int32Type.instance.decompose(9));
        assertNativeProtocolPacking(list,
                                    ColumnMetadata.regularColumn("ks", "t", "l", list, ColumnMetadata.NO_UNIQUE_ID),
                                    timeuuidPaths(3),
                                    listElements,
                                    listElements);

        // set<text>: the path holds the element and the cell value is empty
        CollectionType<?> set = SetType.getInstance(UTF8Type.instance, true);
        List<ByteBuffer> setElements = Arrays.asList(UTF8Type.instance.decompose("aa"),
                                                    UTF8Type.instance.decompose("bb"),
                                                    UTF8Type.instance.decompose("cc"));
        assertNativeProtocolPacking(set,
                                    ColumnMetadata.regularColumn("ks", "t", "s", set, ColumnMetadata.NO_UNIQUE_ID),
                                    setElements,
                                    Arrays.asList(ByteBufferUtil.EMPTY_BYTE_BUFFER,
                                                  ByteBufferUtil.EMPTY_BYTE_BUFFER,
                                                  ByteBufferUtil.EMPTY_BYTE_BUFFER),
                                    setElements);

        // map<text,text>: the path holds the key and the cell value holds the value
        CollectionType<?> map = MapType.getInstance(UTF8Type.instance, UTF8Type.instance, true);
        List<ByteBuffer> keys = Arrays.asList(UTF8Type.instance.decompose("k1"), UTF8Type.instance.decompose("k2"));
        List<ByteBuffer> values = Arrays.asList(UTF8Type.instance.decompose("v1"), UTF8Type.instance.decompose("v2"));
        assertNativeProtocolPacking(map,
                                    ColumnMetadata.regularColumn("ks", "t", "m", map, ColumnMetadata.NO_UNIQUE_ID),
                                    keys,
                                    values,
                                    Arrays.asList(keys.get(0), values.get(0), keys.get(1), values.get(1)));
    }

    /**
     * Builds a multi-cell collection with the given element paths and values, packs it through both native protocol
     * entry points and with both cell implementations, and checks that the protocol bytes carry the expected elements.
     */
    private void assertNativeProtocolPacking(CollectionType<?> type,
                                             ColumnMetadata column,
                                             List<ByteBuffer> paths,
                                             List<ByteBuffer> values,
                                             List<ByteBuffer> expectedElements)
    {
        ComplexColumnData bufferCells = collectionColumnData(column, paths, values, false);
        ComplexColumnData arrayCells = collectionColumnData(column, paths, values, true);

        byte[] packed = type.serializeForNativeProtocolAsByteArrays(bufferCells.iterator());
        assertArrayEquals(ByteBufferUtil.getArray(type.serializeForNativeProtocol(bufferCells.iterator())), packed);
        // and, in case the two packing methods ever drift apart, compare against the packing method this one replaces
        assertArrayEquals(type.getSerializer().pack(type.serializedValuesAsByteArrays(bufferCells.iterator()), ByteArrayAccessor.instance),
                          packed);
        // the cell implementation must not matter, only the values the cells carry
        assertArrayEquals(packed, type.serializeForNativeProtocolAsByteArrays(arrayCells.iterator()));
        assertEquals(expectedElements, unpack(type, packed));

        // an empty collection still carries its (zero) size
        byte[] packedEmpty = type.serializeForNativeProtocolAsByteArrays(emptyCollection(column).iterator());
        assertArrayEquals(new byte[4], packedEmpty);
        assertArrayEquals(ByteBufferUtil.getArray(type.serializeForNativeProtocol(emptyCollection(column).iterator())),
                          packedEmpty);
        assertEquals(Collections.emptyList(), unpack(type, packedEmpty));
    }

    /**
     * The single pass packing of already serialized values must produce exactly the bytes of the generic packing,
     * including for a null element (a deleted collection element) and for a collection without elements.
     */
    @Test
    public void testPackAsByteArrayMatchesPack()
    {
        CollectionSerializer<?> serializer = ListType.getInstance(Int32Type.instance, true).getSerializer();
        byte[] one = ByteBufferUtil.getArray(Int32Type.instance.decompose(1));
        byte[] three = ByteBufferUtil.getArray(Int32Type.instance.decompose(3));
        List<byte[]> withNull = new ArrayList<>();
        withNull.add(one);
        withNull.add(null);
        withNull.add(three);

        for (List<byte[]> values : Arrays.asList(Collections.<byte[]>emptyList(),
                                                 Collections.singletonList(one),
                                                 Arrays.asList(one, three),
                                                 withNull))
            assertArrayEquals(serializer.pack(values, ByteArrayAccessor.instance),
                              serializer.packAsByteArray(values));
    }

    private static List<ByteBuffer> unpack(CollectionType<?> type, byte[] packed)
    {
        return type.getSerializer().unpack(ByteBuffer.wrap(packed));
    }

    /**
     * The cell paths of a list are timeuuids, so a list needs distinct ordered paths that its comparator accepts.
     */
    private static List<ByteBuffer> timeuuidPaths(int count)
    {
        List<ByteBuffer> paths = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
            paths.add(TimeUUIDType.instance.decompose(TimeUUID.Generator.nextTimeUUID()));
        return paths;
    }

    private static ComplexColumnData collectionColumnData(ColumnMetadata column,
                                                          List<ByteBuffer> paths,
                                                          List<ByteBuffer> values,
                                                          boolean arrayCells)
    {
        ComplexColumnData.Builder builder = ComplexColumnData.builder();
        builder.newColumn(column);
        for (int i = 0; i < paths.size(); i++)
        {
            CellPath path = CellPath.create(paths.get(i));
            ByteBuffer value = values.get(i);
            builder.addCell(arrayCells
                            ? ArrayCell.live(column, 1L, ByteBufferUtil.getArray(value), path)
                            : BufferCell.live(column, 1L, value, path));
        }
        return builder.build();
    }

    private static ComplexColumnData emptyCollection(ColumnMetadata column)
    {
        ComplexColumnData.Builder builder = ComplexColumnData.builder();
        builder.newColumn(column);
        // a live empty complex column is not buildable, so mark it deleted instead
        builder.addComplexDeletion(DeletionTime.build(1L, 0));
        return builder.build();
    }

    private void assertInvalid(TypeSerializer<?> type, ByteBuffer value)
    {
        try {
            type.validate(value);
            fail("Value " + ByteBufferUtil.bytesToHex(value) + " shouldn't be valid for type " + type);
        } catch (MarshalException e) {
            // ok, that's what we want
        }
    }
}
