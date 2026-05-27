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
 * The structural "shape" rolled by {@link SchemaGenerator} for a single run.
 *
 * <p>The shape determines (a) the regular-column count band the schema is drawn
 * from and (b) the per-row NULL fraction used by {@code DataGenerator}. Wide
 * shapes exercise the cursor pipeline's "column subset" encoding path that
 * {@code Columns.serialize} takes when the row contains far fewer cells than
 * the table's regular-column set — a code path that's never reached by
 * narrow-shape schemas because their dense rows always fall into the
 * "all columns present" fast path.
 */
public enum SchemaShape
{
    /** 1-2 PK, 0-2 CK, 0-2 statics, 3-8 regulars. The common case. */
    NARROW,
    /** 1-2 PK, 0-2 CK, 0-32 statics, 64-200 regulars. Forces sparse-row encoding. */
    WIDE
}
