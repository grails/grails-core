/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.grails.web.converters.jackson;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.AnyGetterWriter;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.jdk.MapSerializer;

/**
 * Writes the entries of a persistent map property annotated {@code @JsonAnyGetter} into the object of the instance, in
 * the place of the property, as Jackson writes those of any bean, whatever name a naming strategy gives the property.
 * Jackson writes such a property in its place when the names match, and else writes the property and appends the
 * entries.
 *
 * @since 9.0
 */
final class DomainAnyGetterWriter extends BeanPropertyWriter {

    private DomainAnyGetterWriter(BeanPropertyWriter writer) {
        super(writer);
    }

    /**
     * @param writer the writer of a persistent property, or the one Jackson put in its place for a {@code @JsonAnyGetter}
     * @return a writer of its entries, if it is annotated {@code @JsonAnyGetter}, or else the writer
     */
    static BeanPropertyWriter of(BeanPropertyWriter writer) {
        JsonAnyGetter anyGetter = writer.getAnnotation(JsonAnyGetter.class);
        return writer instanceof AnyGetterWriter || anyGetter != null && anyGetter.enabled() ?
                new DomainAnyGetterWriter(writer) : writer;
    }

    @Override
    public void serializeAsProperty(Object bean, JsonGenerator generator, SerializationContext context) throws Exception {
        Object value = get(bean);
        if (value == null) {
            return;
        }
        if (!(value instanceof Map<?, ?> map)) {
            context.reportBadDefinition(getType(), "Value of the @JsonAnyGetter property '%s' is not a java.util.Map but %s"
                    .formatted(getName(), value.getClass().getName()));
            return;
        }
        ValueSerializer<Object> serializer = _serializer;
        if (serializer == null) {
            serializer = _dynamicSerializers.serializerFor(value.getClass());
            if (serializer == null) {
                serializer = _findAndAddDynamic(_dynamicSerializers, value.getClass(), context);
            }
        }
        MapSerializer entries = entries(serializer);
        if (entries != null) {
            entries.serializeWithoutTypeInfo(map, generator, context);
        }
        else {
            // as Jackson writes it with a serializer the property names
            serializer.serialize(value, generator, context);
        }
    }

    /** The map serializer inside the serializers that wrap it, such as the JSON converter's. */
    private static MapSerializer entries(ValueSerializer<?> serializer) {
        for (ValueSerializer<?> current = serializer; current != null; current = current.getDelegatee()) {
            if (current instanceof MapSerializer map) {
                return map;
            }
            if (current.getDelegatee() == current) {
                return null;
            }
        }
        return null;
    }
}
