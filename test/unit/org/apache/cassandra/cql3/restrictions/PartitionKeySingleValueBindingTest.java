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

import org.junit.Test;

import org.apache.cassandra.cql3.CQLTester;
import org.apache.cassandra.exceptions.InvalidRequestException;

/**
 * Tests the contract of {@link SingleRestriction#bindSingleColumnValue}: the single column {@code EQ} fast path must
 * select exactly the partition keys that the general {@link SingleRestriction#values} path selects, and every other
 * restriction shape (IN, slices, multi column, token) must keep using the general path.
 */
public class PartitionKeySingleValueBindingTest extends CQLTester
{
    @Test
    public void testSingleColumnEquality() throws Throwable
    {
        createTable("CREATE TABLE %s (pk text PRIMARY KEY, v int)");

        execute("INSERT INTO %s (pk, v) VALUES (?, ?)", "a", 1);
        execute("INSERT INTO %s (pk, v) VALUES (?, ?)", "b", 2);
        execute("INSERT INTO %s (pk, v) VALUES (?, ?)", "missing", 3);
        execute("DELETE FROM %s WHERE pk = ?", "missing");

        // bound value on the fast path
        assertRows(execute("SELECT v FROM %s WHERE pk = ?", "a"), row(1));
        assertRows(execute("SELECT v FROM %s WHERE pk = ?", "b"), row(2));
        // deleted row
        assertEmpty(execute("SELECT v FROM %s WHERE pk = ?", "missing"));
        // unknown partition
        assertEmpty(execute("SELECT v FROM %s WHERE pk = ?", "unknown"));

        // constants and bound values must agree
        assertRows(execute("SELECT v FROM %s WHERE pk = 'a'"), row(1));

        // IN does not use the single value path
        assertRowsIgnoringOrder(execute("SELECT v FROM %s WHERE pk IN (?, ?)", "a", "b"), row(1), row(2));
        assertRowsIgnoringOrder(execute("SELECT v FROM %s WHERE pk IN ('a', 'b')"), row(1), row(2));

        // updates and deletes through the same fast path
        execute("UPDATE %s SET v = ? WHERE pk = ?", 4, "a");
        assertRows(execute("SELECT v FROM %s WHERE pk = ?", "a"), row(4));
        execute("DELETE FROM %s WHERE pk = ?", "a");
        assertEmpty(execute("SELECT v FROM %s WHERE pk = ?", "a"));
    }

    @Test
    public void testNullAndUnsetPartitionKeyValues() throws Throwable
    {
        createTable("CREATE TABLE %s (pk text PRIMARY KEY, v int)");

        // a null bound value must be rejected the same way the general path rejects it, it is never a valid key
        assertInvalidThrow(InvalidRequestException.class, "SELECT v FROM %s WHERE pk = ?", (String) null);
        assertInvalidThrow(InvalidRequestException.class, "INSERT INTO %s (pk, v) VALUES (?, ?)", (String) null, 1);
        assertInvalidThrow(InvalidRequestException.class, "DELETE FROM %s WHERE pk = ?", (String) null);

        // and an unset bound value as well
        assertInvalidThrow(InvalidRequestException.class, "SELECT v FROM %s WHERE pk = ?", unset());
    }

    @Test
    public void testCompositePartitionKey() throws Throwable
    {
        createTable("CREATE TABLE %s (pk1 int, pk2 text, v int, PRIMARY KEY ((pk1, pk2)))");

        execute("INSERT INTO %s (pk1, pk2, v) VALUES (?, ?, ?)", 1, "a", 1);
        execute("INSERT INTO %s (pk1, pk2, v) VALUES (?, ?, ?)", 1, "", 2);

        assertRows(execute("SELECT v FROM %s WHERE pk1 = ? AND pk2 = ?", 1, "a"), row(1));
        assertRows(execute("SELECT v FROM %s WHERE pk1 = ? AND pk2 = ?", 1, ""), row(2));
        assertEmpty(execute("SELECT v FROM %s WHERE pk1 = ? AND pk2 = ?", 1, "unknown"));
    }

    @Test
    public void testTokenRestriction() throws Throwable
    {
        createTable("CREATE TABLE %s (pk text PRIMARY KEY, v int)");

        execute("INSERT INTO %s (pk, v) VALUES (?, ?)", "a", 1);

        assertRows(execute("SELECT v FROM %s WHERE token(pk) = token(?)", "a"), row(1));
    }

    @Test
    public void testOversizedPartitionKeyStillRejected() throws Throwable
    {
        createTable("CREATE TABLE %s (pk text PRIMARY KEY, v int)");

        // one byte over the limit of the partition key serialization
        String oversized = new String(new char[0xFFFF + 1]).replace('\0', 'a');
        assertInvalidThrow(InvalidRequestException.class, "SELECT v FROM %s WHERE pk = ?", oversized);
        assertInvalidThrow(InvalidRequestException.class, "INSERT INTO %s (pk, v) VALUES (?, ?)", oversized, 1);

        // the largest key that is still accepted
        String largest = new String(new char[0xFFFF]).replace('\0', 'a');
        execute("INSERT INTO %s (pk, v) VALUES (?, ?)", largest, 1);
        assertRows(execute("SELECT v FROM %s WHERE pk = ?", largest), row(1));
    }
}
