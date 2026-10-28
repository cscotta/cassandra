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
import java.util.Objects;

import org.apache.commons.lang3.mutable.MutableInt;

import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.functions.ArgumentDeserializer;
import org.apache.cassandra.cql3.terms.Constants;
import org.apache.cassandra.cql3.terms.Term;
import org.apache.cassandra.serializers.Int32Serializer;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.serializers.TypeSerializer;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.bytecomparable.ByteComparable;
import org.apache.cassandra.utils.bytecomparable.ByteSource;
import org.apache.cassandra.utils.bytecomparable.ByteSourceInverse;
import org.apache.cassandra.utils.memory.BigEndianMemoryUtil;

public class Int32Type extends NumberType<Integer>
{
    public static final Int32Type instance = new Int32Type();

    private static final ByteBuffer MASKED_VALUE = instance.decompose(0);

    Int32Type()
    {
        super(ComparisonType.CUSTOM, 4);
    } // singleton

    @Override
    public boolean allowsEmpty()
    {
        return true;
    }

    @Override
    public boolean isEmptyValueMeaningless()
    {
        return true;
    }

    public <VL, VR> int compareCustom(VL left, ValueAccessor<VL> accessorL, VR right, ValueAccessor<VR> accessorR)
    {
        if (accessorL.isEmpty(left) || accessorR.isEmpty(right))
            return Boolean.compare(accessorR.isEmpty(right), accessorL.isEmpty(left));

        int diff = accessorL.getByte(left, 0) - accessorR.getByte(right, 0);
        if (diff != 0)
            return diff;

        return ValueAccessor.compare(left, accessorL, right, accessorR);
    }

    /**
     * Every valid int is four bytes, so the ordering of two such values is the ordering of their
     * (sign-extended, big-endian) decodes. Other lengths are not valid ints and keep the byte-wise
     * comparison of {@link #compareCustom}, so ordering of any malformed content is unchanged.
     * <p>
     * Int values are always stored big-endian, as the off-heap and networking paths assume; for a
     * little-endian heap buffer the {@code super} paths remain the authority.
     */
    @Override
    public int compareNative(long addressLeft, int lengthLeft, long addressRight, int lengthRight)
    {
        if (lengthLeft == 0 || lengthRight == 0)
            return Boolean.compare(lengthRight == 0, lengthLeft == 0);

        if (lengthLeft == 4 && lengthRight == 4)
            return Integer.compare(BigEndianMemoryUtil.getInt(addressLeft), BigEndianMemoryUtil.getInt(addressRight));

        return super.compareNative(addressLeft, lengthLeft, addressRight, lengthRight);
    }

    @Override
    public <V> int compareNativeTo(long leftAddress, int leftLength, V right, ValueAccessor<V> rightAccessor)
    {
        boolean rightIsEmpty = rightAccessor.isEmpty(right);
        if (leftLength == 0 || rightIsEmpty)
            return Boolean.compare(rightIsEmpty, leftLength == 0);

        if (leftLength == 4 && rightAccessor.size(right) == 4)
            return Integer.compare(BigEndianMemoryUtil.getInt(leftAddress), rightAccessor.getInt(right, 0));

        return super.compareNativeTo(leftAddress, leftLength, right, rightAccessor);
    }

    @Override
    public <V> int compareToNative(V left, ValueAccessor<V> leftAccessor, long rightAddress, int rightLength)
    {
        boolean leftIsEmpty = leftAccessor.isEmpty(left);
        if (leftIsEmpty || rightLength == 0)
            return Boolean.compare(rightLength == 0, leftIsEmpty);

        if (leftAccessor.size(left) == 4 && rightLength == 4)
            return Integer.compare(leftAccessor.getInt(left, 0), BigEndianMemoryUtil.getInt(rightAddress));

        return super.compareToNative(left, leftAccessor, rightAddress, rightLength);
    }

    @Override
    public <V> ByteSource asComparableBytes(ValueAccessor<V> accessor, V data, ByteComparable.Version version)
    {
        return ByteSource.optionalSignedFixedLengthNumber(accessor, data);
    }

    @Override
    public <V> V fromComparableBytes(ValueAccessor<V> accessor, ByteSource.Peekable comparableBytes, ByteComparable.Version version)
    {
        return ByteSourceInverse.getOptionalSignedFixedLength(accessor, comparableBytes, 4);
    }

    public ByteBuffer fromString(String source) throws MarshalException
    {
        // Return an empty ByteBuffer for an empty string.
        if (source.isEmpty())
            return ByteBufferUtil.EMPTY_BYTE_BUFFER;

        int int32Type;

        try
        {
            int32Type = Integer.parseInt(source);
        }
        catch (Exception e)
        {
            throw new MarshalException(String.format("Unable to make int from '%s'", source), e);
        }

        return decompose(int32Type);
    }

    @Override
    public Term fromJSONObject(Object parsed) throws MarshalException
    {
        try
        {
            if (parsed instanceof String)
                return new Constants.Value(fromString((String) parsed));

            Number parsedNumber = (Number) parsed;
            if (!(parsedNumber instanceof Integer))
                throw new MarshalException(String.format("Expected an int value, but got a %s: %s", parsed.getClass().getSimpleName(), parsed));

            return new Constants.Value(getSerializer().serialize(parsedNumber.intValue()));
        }
        catch (ClassCastException exc)
        {
            throw new MarshalException(String.format(
                    "Expected an int value, but got a %s: %s", parsed.getClass().getSimpleName(), parsed));
        }
    }

    @Override
    public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion)
    {
        return Objects.toString(getSerializer().deserialize(buffer), "\"\"");
    }

    public CQL3Type asCQL3Type()
    {
        return CQL3Type.Native.INT;
    }

    public TypeSerializer<Integer> getSerializer()
    {
        return Int32Serializer.instance;
    }

    @Override
    public ArgumentDeserializer getArgumentDeserializer()
    {
        return new NumberArgumentDeserializer<MutableInt>(new MutableInt())
        {
            @Override
            protected void setMutableValue(MutableInt mutable, ByteBuffer buffer)
            {
                mutable.setValue(ByteBufferUtil.toInt(buffer));
            }
        };
    }

    @Override
    public ByteBuffer add(Number left, Number right)
    {
        return ByteBufferUtil.bytes(left.intValue() + right.intValue());
    }

    @Override
    public ByteBuffer substract(Number left, Number right)
    {
        return ByteBufferUtil.bytes(left.intValue() - right.intValue());
    }

    @Override
    public ByteBuffer multiply(Number left, Number right)
    {
        return ByteBufferUtil.bytes(left.intValue() * right.intValue());
    }

    @Override
    public ByteBuffer divide(Number left, Number right)
    {
        return ByteBufferUtil.bytes(left.intValue() / right.intValue());
    }

    @Override
    public ByteBuffer mod(Number left, Number right)
    {
        return ByteBufferUtil.bytes(left.intValue() % right.intValue());
    }

    @Override
    public ByteBuffer negate(Number input)
    {
        return ByteBufferUtil.bytes(-input.intValue());
    }

    @Override
    public ByteBuffer abs(Number input)
    {
        return ByteBufferUtil.bytes(Math.abs(input.intValue()));
    }

    @Override
    public ByteBuffer exp(Number input)
    {
        return ByteBufferUtil.bytes((int) Math.exp(input.intValue()));
    }

    @Override
    public ByteBuffer log(Number input)
    {
        return ByteBufferUtil.bytes((int) Math.log(input.intValue()));
    }

    @Override
    public ByteBuffer log10(Number input)
    {
        return ByteBufferUtil.bytes((int) Math.log10(input.intValue()));
    }

    @Override
    public ByteBuffer round(Number input)
    {
        return ByteBufferUtil.bytes(input.intValue());
    }

    @Override
    public ByteBuffer getMaskedValue()
    {
        return MASKED_VALUE;
    }
}
