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
package org.apache.cassandra.tools.compactionvalidator.schema;

/**
 * Metadata for a single column in a generated schema.
 *
 * <p>Instances are immutable.
 */
public final class ColumnInfo
{
    /** Column name as it appears in the CQL statement, e.g. {@code "pk0"}, {@code "ck1"}. */
    public final String name;

    /**
     * CQL type string as it appears in a {@code CREATE TABLE} statement, e.g. {@code "int"},
     * {@code "list<int>"}, {@code "map<text, int>"}, {@code "tuple<int, text>"}.
     */
    public final String cqlType;

    /**
     * For clustering keys only: {@code true} when the clustering order is {@code DESC}.
     * Always {@code false} for partition-key, static, and regular columns.
     */
    public final boolean isDesc;

    /**
     * Creates a {@code ColumnInfo} for a non-clustering column (direction is irrelevant).
     *
     * @param name    column name
     * @param cqlType CQL type string
     */
    public ColumnInfo(String name, String cqlType)
    {
        this(name, cqlType, false);
    }

    /**
     * Creates a {@code ColumnInfo} with an explicit clustering direction.
     *
     * @param name    column name
     * @param cqlType CQL type string
     * @param isDesc  {@code true} for {@code DESC} clustering order
     */
    public ColumnInfo(String name, String cqlType, boolean isDesc)
    {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("column name must not be null or empty");
        if (cqlType == null || cqlType.isEmpty())
            throw new IllegalArgumentException("CQL type must not be null or empty");

        this.name = name;
        this.cqlType = cqlType;
        this.isDesc = isDesc;
    }

    @Override
    public String toString()
    {
        return "ColumnInfo{name='" + name + "', cqlType='" + cqlType + "', isDesc=" + isDesc + '}';
    }
}
