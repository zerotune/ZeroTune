/*
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

package plangeneratorflink.operators.window;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.WindowAssigner;
import org.apache.flink.streaming.api.windowing.triggers.CountTrigger;
import org.apache.flink.streaming.api.windowing.triggers.Trigger;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;

import java.util.Collection;
import java.util.Collections;

/**
 * A {@link WindowAssigner} that assigns all elements to the same {@link GlobalWindow}. Difference
 * to the GlobalWindows class: It uses a CountTrigger with the slideSize. Attention: It needs a
 * evictor to be called with CountEvictor.of(size) to work properly.
 *
 * <p>Use this if you want to use a {@link Trigger} and {@link
 * org.apache.flink.streaming.api.windowing.evictors.Evictor} to do flexible, policy based windows.
 */
@PublicEvolving
public class SlidingCountWindows extends WindowAssigner<Object, GlobalWindow> {
    private static final long serialVersionUID = 1L;

    private final long size;
    private final long slide;

    private SlidingCountWindows(long size, long slide) {
        this.size = size;
        this.slide = slide;
    }

    /**
     * Creates a new {@code GlobalWindows} {@link WindowAssigner} that assigns all elements to the
     * same {@link GlobalWindow}.
     *
     * @return The global window policy.
     */
    public static SlidingCountWindows of(long size, long slide) {
        return new SlidingCountWindows(size, slide);
    }

    @Override
    public Collection<GlobalWindow> assignWindows(
            Object element, long timestamp, WindowAssignerContext context) {
        return Collections.singletonList(GlobalWindow.get());
    }

    @Override
    public Trigger<Object, GlobalWindow> getDefaultTrigger(StreamExecutionEnvironment env) {
        return CountTrigger.of(slide);
    }

    @Override
    public String toString() {
        return "SlidingCountWindows()";
    }

    @Override
    public TypeSerializer<GlobalWindow> getWindowSerializer(ExecutionConfig executionConfig) {
        return new GlobalWindow.Serializer();
    }

    @Override
    public boolean isEventTime() {
        return false;
    }
}
