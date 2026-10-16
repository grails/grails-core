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

import java.util.Iterator;
import java.util.Map;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.bean.BeanSerializerBase;
import tools.jackson.databind.util.NameTransformer;

/**
 * A bean serializer that does not write an object again while it is being written, writing what its
 * {@link OnCycle} selects instead.
 *
 * @since 9.0
 */
final class CycleTracking extends ValueSerializer<Object> {

    /** Writes what stands for an object reached again while it is being written. */
    @FunctionalInterface
    interface OnCycle {

        void write(Object value, Cycles.Frame enclosing, JsonGenerator generator, SerializationContext context);
    }

    private final ValueSerializer<Object> delegate;

    private final OnCycle onCycle;

    /**
     * Tracks the bean serializer a serializer wraps, if any.
     *
     * <p>Another module may have wrapped the bean serializer to take over values nested in it, as the JSON converter
     * does to render them itself. The tracker goes inside such a wrapper, since the wrapper hands a value to the converter
     * and the converter writes it again through the mapper: tracked outside, the second write would find the value already
     * being written.</p>
     *
     * @param serializer a serializer, which may wrap a bean serializer
     * @param onCycle what to write for a value reached again
     * @return the serializer, with its bean serializer tracked, or {@code null} if it wraps none
     */
    static ValueSerializer<?> around(ValueSerializer<?> serializer, OnCycle onCycle) {
        if (serializer instanceof CycleTracking tracking) {
            return around(tracking.delegate, onCycle);
        }
        if (serializer instanceof BeanSerializerBase) {
            return new CycleTracking(serializer, onCycle);
        }
        ValueSerializer<?> delegatee = serializer.getDelegatee();
        if (delegatee == null || delegatee == serializer) {
            return null;
        }
        ValueSerializer<?> tracked = around(delegatee, onCycle);
        return tracked != null ? serializer.replaceDelegatee(tracked) : null;
    }

    @SuppressWarnings("unchecked")
    CycleTracking(ValueSerializer<?> delegate, OnCycle onCycle) {
        this.delegate = (ValueSerializer<Object>) delegate;
        this.onCycle = onCycle;
    }

    ValueSerializer<Object> getDelegate() {
        return delegate;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
        Map<Object, Cycles.Frame> instances = Cycles.instances(ctxt);
        Cycles.Frame enclosing = instances.get(value);
        if (enclosing != null) {
            onCycle.write(value, enclosing, gen, ctxt);
            return;
        }
        instances.put(value, Cycles.upcoming(gen));
        try {
            delegate.serialize(value, gen, ctxt);
        }
        finally {
            instances.remove(value);
        }
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        Map<Object, Cycles.Frame> instances = Cycles.instances(ctxt);
        Cycles.Frame enclosing = instances.get(value);
        if (enclosing != null) {
            onCycle.write(value, enclosing, gen, ctxt);
            return;
        }
        instances.put(value, Cycles.upcoming(gen));
        try {
            delegate.serializeWithType(value, gen, ctxt, typeSer);
        }
        finally {
            instances.remove(value);
        }
    }

    @Override
    public void resolve(SerializationContext ctxt) {
        delegate.resolve(ctxt);
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
        ValueSerializer<?> contextual = delegate.createContextual(ctxt, property);
        return contextual == delegate ? this : new CycleTracking(contextual, onCycle);
    }

    @Override
    public ValueSerializer<Object> unwrappingSerializer(NameTransformer unwrapper) {
        // Properties written into an enclosing object have no object of their own to track
        return delegate.unwrappingSerializer(unwrapper);
    }

    @Override
    public boolean isUnwrappingSerializer() {
        return delegate.isUnwrappingSerializer();
    }

    @Override
    public boolean isEmpty(SerializationContext ctxt, Object value) {
        return delegate.isEmpty(ctxt, value);
    }

    @Override
    public boolean usesObjectId() {
        return delegate.usesObjectId();
    }

    @Override
    public Class<?> handledType() {
        return delegate.handledType();
    }

    @Override
    public ValueSerializer<?> getDelegatee() {
        return delegate;
    }

    @Override
    public Iterator<PropertyWriter> properties() {
        return delegate.properties();
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType type) {
        delegate.acceptJsonFormatVisitor(visitor, type);
    }
}
