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
package org.apache.cassandra.db.rows;

import java.util.Iterator;

import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.schema.TableMetadata;

/**
 * The {@link UnfilteredRowIterator} of a partition that has no range tombstones: the rows are all there is, so all
 * that is left to do is to apply the column selection and the partition deletion to them. This is the state
 * {@link RowAndDeletionMergeIterator} is in when its range tombstone source is empty, without the merge state.
 */
public final class RowsOnlyIterator extends AbstractUnfilteredRowIterator
{
    private final ColumnFilter selection;
    private final Iterator<Row> rows;

    public RowsOnlyIterator(TableMetadata metadata,
                            DecoratedKey partitionKey,
                            DeletionTime partitionLevelDeletion,
                            ColumnFilter selection,
                            Row staticRow,
                            boolean isReversed,
                            EncodingStats stats,
                            Iterator<Row> rows)
    {
        super(metadata, partitionKey, partitionLevelDeletion, selection.fetchedColumns(), staticRow, isReversed, stats);
        this.selection = selection;
        this.rows = rows;
    }

    @Override
    protected Unfiltered computeNext()
    {
        while (rows.hasNext())
        {
            // a row can be entirely shadowed by the partition deletion, in which case it must not be returned
            Row next = rows.next().filter(selection, partitionLevelDeletion(), false, metadata());
            if (next != null)
                return next;
        }
        return endOfData();
    }
}
