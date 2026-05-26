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
package org.apache.cassandra.tools.compactionvalidator;

/**
 * Lightweight value object describing one SSTable for progress / TUI events.
 * Crosses the {@link ProgressTap} boundary, so it must not depend on Cassandra
 * runtime types — only the file basename, size, and level number.
 */
public final class SstableInfo
{
    public final String filename;
    public final long sizeBytes;
    public final int level;

    public SstableInfo(String filename, long sizeBytes, int level)
    {
        this.filename = filename;
        this.sizeBytes = sizeBytes;
        this.level = level;
    }

    @Override
    public String toString()
    {
        return filename + "(" + sizeBytes + "B, L" + level + ")";
    }
}
