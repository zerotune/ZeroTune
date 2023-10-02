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

package org.apache.flink.table.runtime.groupwindow;

import org.apache.flink.annotation.Internal;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.TimestampType;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Window start property.
 *
 * @deprecated The POJOs in this package are used to represent the deprecated Group Window feature.
 *     Currently, they also used to configure Python operators.
 */
@Deprecated
@Internal
@JsonTypeName("WindowStart")
public class WindowStart extends AbstractWindowProperty {

    @JsonCreator
    public WindowStart(@JsonProperty(FIELD_NAME_REFERENCE) WindowReference reference) {
        super(reference);
    }

    @Override
    public LogicalType getResultType() {
        return new TimestampType(false, 3);
    }

    @Override
    public String toString() {
        return String.format("start(%s)", reference);
    }
}
