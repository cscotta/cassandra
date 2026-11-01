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
package org.apache.cassandra.cql3.restrictions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnsExpression;
import org.apache.cassandra.cql3.Operator;
import org.apache.cassandra.cql3.terms.Constants;
import org.apache.cassandra.cql3.terms.Term;
import org.apache.cassandra.cql3.terms.Terms;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.marshal.Int32Type;
import org.apache.cassandra.schema.ColumnMetadata;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.utils.ByteBufferUtil;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the equality restriction predicate of {@link PartitionKeyRestrictions}, which reports the columns of its token
 * restriction when it has one, instead of the columns of the wrapped restriction set.
 */
public class PartitionKeyRestrictionsTest
{
    @BeforeClass
    public static void setupDD()
    {
        DatabaseDescriptor.daemonInitialization();
    }

    @Test
    public void testHasOnlyEqualityRestrictions()
    {
        TableMetadata table = newTableMetadata();
        ByteBuffer value = ByteBufferUtil.bytes(1);

        assertTrue(partitionKeyRestrictions(table).hasOnlyEqualityRestrictions());
        assertTrue(partitionKeyRestrictions(table, eq(table, value)).hasOnlyEqualityRestrictions());
        assertTrue(partitionKeyRestrictions(table, in(table, value)).hasOnlyEqualityRestrictions());

        assertFalse(partitionKeyRestrictions(table, slice(table, Operator.GT, value)).hasOnlyEqualityRestrictions());
        assertFalse(partitionKeyRestrictions(table, slice(table, Operator.LT, value)).hasOnlyEqualityRestrictions());
    }

    /**
     * A token restriction is not a restriction on the partition key column itself, so the predicate has to keep
     * answering for the columns the wrapper reports (the token restriction's ones), which is what the check did when
     * it walked {@link PartitionKeyRestrictions#columns()}.
     */
    @Test
    public void testHasOnlyEqualityRestrictionsWithTokenRestriction()
    {
        TableMetadata table = newTableMetadata();
        ByteBuffer value = ByteBufferUtil.bytes(1);

        // token(pk) > 1 alone does not restrict the partition key column by an equality
        assertFalse(partitionKeyRestrictions(table, token(table, Operator.GT, value)).hasOnlyEqualityRestrictions());

        // a key equality and a token restriction: the token restriction's column is equality restricted
        assertTrue(partitionKeyRestrictions(table, eq(table, value), token(table, Operator.GT, value)).hasOnlyEqualityRestrictions());

        // a key slice stays a slice with a token restriction as well
        assertFalse(partitionKeyRestrictions(table, slice(table, Operator.GT, value),
                                            token(table, Operator.GT, value)).hasOnlyEqualityRestrictions());
    }

    private static TableMetadata newTableMetadata()
    {
        return TableMetadata.builder("keyspace", "test")
                            .addPartitionKeyColumn("pk", Int32Type.instance)
                            .build();
    }

    private static ClusteringComparator comparator(TableMetadata table)
    {
        return table.partitionKeyAsClusteringComparator();
    }

    private static PartitionKeyRestrictions partitionKeyRestrictions(TableMetadata table, Restriction... restrictions)
    {
        PartitionKeyRestrictions partitionKeyRestrictions = new PartitionKeyRestrictions(comparator(table));
        for (Restriction restriction : restrictions)
            partitionKeyRestrictions = partitionKeyRestrictions.mergeWith(restriction);
        return partitionKeyRestrictions;
    }

    private static ColumnMetadata pk(TableMetadata table)
    {
        return table.getColumn(new ColumnIdentifier("pk", false));
    }

    private static Restriction eq(TableMetadata table, ByteBuffer value)
    {
        return new SimpleRestriction(ColumnsExpression.singleColumn(pk(table), table), Operator.EQ, toTerms(value), false);
    }

    private static Restriction in(TableMetadata table, ByteBuffer value)
    {
        return new SimpleRestriction(ColumnsExpression.singleColumn(pk(table), table), Operator.IN, toTerms(value), false);
    }

    private static Restriction slice(TableMetadata table, Operator operator, ByteBuffer value)
    {
        return new SimpleRestriction(ColumnsExpression.singleColumn(pk(table), table), operator, toTerms(value), false);
    }

    private static Restriction token(TableMetadata table, Operator operator, ByteBuffer token)
    {
        ColumnsExpression expression = ColumnsExpression.Raw.token(asList(pk(table).name)).prepare(table);
        return new SimpleRestriction(expression, operator, toTerms(token), false);
    }

    private static Terms toTerms(ByteBuffer... values)
    {
        List<Term> terms = new ArrayList<>(values.length);
        for (ByteBuffer value : values)
            terms.add(new Constants.Value(value));
        return Terms.of(terms);
    }
}
