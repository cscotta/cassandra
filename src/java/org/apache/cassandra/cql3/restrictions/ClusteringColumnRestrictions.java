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

import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;

import javax.annotation.Nullable;

import com.google.common.collect.RangeSet;

import org.apache.cassandra.cql3.FunctionContext;
import org.apache.cassandra.cql3.Operator;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.db.BufferClusteringBound;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringBound;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.MultiCBuilder;
import org.apache.cassandra.db.Slice;
import org.apache.cassandra.db.Slices;
import org.apache.cassandra.db.filter.IndexHints;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.db.guardrails.Guardrails;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.index.IndexRegistry;
import org.apache.cassandra.schema.ColumnMetadata;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.service.ClientState;

import static org.apache.cassandra.cql3.statements.RequestValidations.checkFalse;
import static org.apache.cassandra.cql3.statements.RequestValidations.invalidRequest;

/**
 * A set of restrictions on the clustering key.
 */
final class ClusteringColumnRestrictions extends RestrictionSetWrapper
{
    /**
     * The composite type.
     */
    private final ClusteringComparator comparator;

    /**
     * <code>true</code> if filtering is allowed for this restriction, <code>false</code> otherwise
     */
    private final boolean allowFiltering;

    private final IPartitioner partitioner;

    public ClusteringColumnRestrictions(TableMetadata table, boolean allowFiltering)
    {
        this(table.comparator, RestrictionSet.empty(), allowFiltering, table.partitioner);
    }

    private ClusteringColumnRestrictions(ClusteringComparator comparator,
                                         RestrictionSet restrictionSet,
                                         boolean allowFiltering,
                                         IPartitioner partitioner)
    {
        super(restrictionSet);
        this.comparator = comparator;
        this.allowFiltering = allowFiltering;
        this.partitioner = partitioner;
    }

    public ClusteringColumnRestrictions mergeWith(Restriction restriction, @Nullable IndexRegistry indexRegistry, IndexHints indexHints) throws InvalidRequestException
    {
        SingleRestriction newRestriction = (SingleRestriction) restriction;
        RestrictionSet newRestrictionSet = restrictions.addRestriction(newRestriction);

        if (!isEmpty() && !allowFiltering && (indexRegistry == null || !newRestriction.hasSupportingIndex(indexRegistry, indexHints)))
        {
            SingleRestriction lastRestriction = restrictions.lastRestriction();
            assert lastRestriction != null;

            ColumnMetadata lastRestrictionStart = lastRestriction.firstColumn();
            ColumnMetadata newRestrictionStart = restriction.firstColumn();

            checkFalse(lastRestriction.isSlice() && newRestrictionStart.position() > lastRestrictionStart.position(),
                       "Clustering column \"%s\" cannot be restricted (preceding column \"%s\" is restricted by a non-EQ relation)",
                       newRestrictionStart.name,
                       lastRestrictionStart.name);

            if (newRestrictionStart.position() < lastRestrictionStart.position() && newRestriction.isSlice())
                throw invalidRequest("PRIMARY KEY column \"%s\" cannot be restricted (preceding column \"%s\" is restricted by a non-EQ relation)",
                                     restrictions.nextColumn(newRestrictionStart).name,
                                     newRestrictionStart.name);
        }

        return new ClusteringColumnRestrictions(this.comparator, newRestrictionSet, allowFiltering, partitioner);
    }

    public NavigableSet<Clustering<?>> valuesAsClustering(QueryOptions options, ClientState state) throws InvalidRequestException
    {
        // fast path, a typical case when a single full restriction is used
        // for example, when we specify a single clustering key (a single row) to insert/update
        if (restrictions.size() == 1 && !restrictions.hasIN())
        {
            SingleRestriction r = restrictions.lastRestriction();
            List<ClusteringElements> values = r.values(options);
            return MultiCBuilder.build(comparator, values);
        }
        MultiCBuilder builder = new MultiCBuilder(comparator);
        for (SingleRestriction r : restrictions)
        {
            List<ClusteringElements> values = r.values(options);
            builder.extend(values);

            // If values is greater than 1 we know that the restriction is an IN
            if (values.size() > 1 && Guardrails.inSelectCartesianProduct.enabled(state))
                Guardrails.inSelectCartesianProduct.guard(builder.buildSize(), "clustering key", false, state);

            if (builder.hasMissingElements())
                break;
        }
        return builder.build();
    }

    public Slices slices(QueryOptions options) throws InvalidRequestException
    {
        Slices slices = singleColumnSlices(options);
        return slices != null ? slices : genericSlices(options);
    }

    /**
     * Builds the slices of a restriction set that only constrains the first clustering column with a slice, for
     * instance {@code c1 >= ? AND c1 <= ?} on an ascending column.
     * <p>
     * For a single ascending column the restricted set is always one slice whose bounds are the restriction bounds,
     * so it can be built directly. {@link #genericSlices(QueryOptions)} starts from a Guava {@code RangeSet}
     * containing every clustering and removes the restricted ranges from it, which costs a tree insertion, a tree
     * removal and several temporary objects per execution. Reversed columns, token restrictions and multi-column
     * restrictions can produce several ranges (the CQL order and the internal clustering order differ) and are left
     * to the generic path.
     *
     * @param options the query options
     * @return the slices, or {@code null} if this restriction set has to be handled by {@link #genericSlices(QueryOptions)}
     */
    @Nullable
    private Slices singleColumnSlices(QueryOptions options)
    {
        if (restrictions.size() != 1)
            return null;

        SingleRestriction restriction = restrictions.lastRestriction();
        if (restriction == null || !restriction.isSlice() || restriction.isMultiColumn() || restriction.isOnToken())
            return null;

        ColumnMetadata column = restriction.firstColumn();
        if (column.position() != 0 || column.type.isReversed())
            return null;

        List<SimpleRestriction> parts;
        if (restriction instanceof SimpleRestriction)
            parts = Collections.singletonList((SimpleRestriction) restriction);
        else if (restriction instanceof MergedRestriction)
            parts = ((MergedRestriction) restriction).underlyingRestrictions();
        else
            return null;

        ClusteringBound<?> start = null;
        ClusteringBound<?> end = null;
        for (int i = 0, m = parts.size(); i < m; i++)
        {
            SimpleRestriction part = parts.get(i);
            List<ClusteringElements> elements = part.boundElements(options);
            switch (part.operator())
            {
                case GT:
                case GTE:
                    if (start != null || elements.size() != 1)
                        return null;
                    start = elements.get(0).toBound(true, part.operator() == Operator.GTE);
                    break;
                case LT:
                case LTE:
                    if (end != null || elements.size() != 1)
                        return null;
                    end = elements.get(0).toBound(false, part.operator() == Operator.LTE);
                    break;
                case BETWEEN:
                    if (start != null || end != null || elements.size() != 2)
                        return null;
                    start = elements.get(0).toBound(true, true);
                    end = elements.get(1).toBound(false, true);
                    break;
                default:
                    return null;
            }
        }

        Slice slice = Slice.make(start == null ? BufferClusteringBound.BOTTOM : start,
                                 end == null ? BufferClusteringBound.TOP : end);
        if (slice.isEmpty(comparator))
            return Slices.NONE;

        Slices.Builder builder = new Slices.Builder(comparator, 1);
        builder.add(slice);
        return builder.build();
    }

    /**
     * Builds the slices by restricting a range spanning all the clusterings, the generic implementation for every
     * restriction shape that {@link #singleColumnSlices(QueryOptions)} does not handle.
     *
     * @param options the query options
     * @return the slices
     */
    Slices genericSlices(QueryOptions options) throws InvalidRequestException
    {
        MultiCBuilder builder = new MultiCBuilder(comparator);
        if (restrictions.isEmpty()) // to avoid an iterator allocation for restrictions
            return builder.buildSlices();

        int keyPosition = 0;
        for (SingleRestriction r : restrictions)
        {
            if (handleInFilter(r, keyPosition))
                break;

            if (r.isSlice())
            {
                RangeSet<ClusteringElements> rangeSet = ClusteringElements.all();
                r.restrict(rangeSet, options, partitioner);
                return builder.extend(rangeSet).buildSlices();
            }

            builder.extend(r.values(options));

            if (builder.hasMissingElements())
                break;

            keyPosition = r.lastColumn().position() + 1;
        }

        // Everything was an equal (or there was nothing)
        return builder.buildSlices();
    }

    /**
     * Checks if any of the underlying restriction is a slice restrictions.
     *
     * @return <code>true</code> if any of the underlying restriction is a slice restrictions,
     * <code>false</code> otherwise
     */
    public boolean hasSlice()
    {
        for (SingleRestriction restriction : restrictions)
        {
            if (restriction.isSlice())
                return true;
        }
        return false;
    }

    /**
     * Checks if underlying restrictions would require filtering
     *
     * @return <code>true</code> if any underlying restrictions require filtering, <code>false</code>
     * otherwise
     */
    public boolean needFiltering()
    {
        int position = 0;

        for (SingleRestriction restriction : restrictions)
        {
            if (handleInFilter(restriction, position))
                return true;

            if (!restriction.isSlice())
                position = restriction.lastColumn().position() + 1;
        }
        return false;
    }

    @Override
    public void addToRowFilter(RowFilter filter,
                               IndexRegistry indexRegistry,
                               FunctionContext context,
                               IndexHints indexHints) throws InvalidRequestException
    {
        int position = 0;

        for (SingleRestriction restriction : restrictions)
        {
            // We ignore all the clustering columns that can be handled by slices.
            if (handleInFilter(restriction, position) || restriction.hasSupportingIndex(indexRegistry, indexHints))
            {
                restriction.addToRowFilter(filter, indexRegistry, context, indexHints);
                continue;
            }

            if (!restriction.isSlice())
                position = restriction.lastColumn().position() + 1;
        }
    }

    private boolean handleInFilter(SingleRestriction restriction, int index)
    {
        return restriction.needsFilteringOrIndexing() || index != restriction.firstColumn().position();
    }
}
