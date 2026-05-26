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
package org.apache.cassandra.tools.compactionvalidator.tui;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;

/**
 * Common contract for all panels rendered by {@link TuiManager}.  Panels are
 * stateful (they accumulate state from {@link TuiEvent}s) and stateless in
 * rendering (they redraw their entire region on every frame).
 *
 * <p>{@code onEvent} is called from the render thread; {@code render} is also
 * called from the render thread.  Implementations therefore do not need to be
 * thread-safe — but they must be lightweight, since both methods run on the
 * fps-paced loop.
 */
public interface TuiPanel
{
    /**
     * Update internal state from {@code event}.  Implementations should
     * inspect the runtime type and ignore events they do not care about.
     *
     * @param event the event to apply
     */
    void onEvent(TuiEvent event);

    /**
     * Render this panel's current state into the given graphics buffer at
     * {@code origin} occupying {@code size} cells.  The implementation must
     * not draw outside that rectangle.
     *
     * @param graphics drawing surface
     * @param origin   top-left corner of this panel's drawing region
     * @param size     dimensions of this panel's drawing region
     */
    void render(TextGraphics graphics, TerminalPosition origin, TerminalSize size);
}
