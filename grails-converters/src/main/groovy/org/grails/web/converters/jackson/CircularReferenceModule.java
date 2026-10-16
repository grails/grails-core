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
import java.util.function.Supplier;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.TokenStreamContext;
import tools.jackson.core.Version;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.ser.bean.BeanSerializerBase;
import tools.jackson.databind.util.NameTransformer;

import org.grails.web.converters.Converter.CircularReferenceBehaviour;

/**
 * Applies the JSON converter's circular reference behaviour to the beans a mapper writes property by property, as
 * {@link DomainClassSerializer} applies it to domain class instances. A bean that is reached again while it is being
 * written would otherwise be written until Jackson's nesting limit fails the response.
 *
 * <p>Both share the instances being written, so a cycle that passes through domain class instances and plain beans is
 * found at whichever it first repeats.</p>
 *
 * @since 9.0
 */
final class CircularReferenceModule extends JacksonModule {

    private final Supplier<CircularReferenceBehaviour> behaviour;

    CircularReferenceModule(Supplier<CircularReferenceBehaviour> behaviour) {
        this.behaviour = behaviour;
    }

    @Override
    public String getModuleName() {
        return "grails-circular-references";
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addSerializerModifier(new ValueSerializerModifier() {
            @Override
            public ValueSerializer<?> modifySerializer(SerializationConfig config, BeanDescription.Supplier beanDescription,
                    ValueSerializer<?> serializer) {
                return serializer instanceof BeanSerializerBase ? new Tracking(serializer, behaviour) : serializer;
            }
        });
    }

    private static final class Tracking extends ValueSerializer<Object> {

        private final ValueSerializer<Object> delegate;

        private final Supplier<CircularReferenceBehaviour> behaviour;

        @SuppressWarnings("unchecked")
        private Tracking(ValueSerializer<?> delegate, Supplier<CircularReferenceBehaviour> behaviour) {
            this.delegate = (ValueSerializer<Object>) delegate;
            this.behaviour = behaviour;
        }

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
            Map<Object, DomainClassSerializer.Frame> instances = DomainClassSerializer.renderingInstances(ctxt);
            DomainClassSerializer.Frame enclosing = instances.get(value);
            if (enclosing != null) {
                DomainClassSerializer.writeCircularReference(value, enclosing, gen, behaviour.get());
                return;
            }
            instances.put(value, upcoming(gen));
            try {
                delegate.serialize(value, gen, ctxt);
            }
            finally {
                instances.remove(value);
            }
        }

        @Override
        public void serializeWithType(Object value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
            Map<Object, DomainClassSerializer.Frame> instances = DomainClassSerializer.renderingInstances(ctxt);
            DomainClassSerializer.Frame enclosing = instances.get(value);
            if (enclosing != null) {
                DomainClassSerializer.writeCircularReference(value, enclosing, gen, behaviour.get());
                return;
            }
            instances.put(value, upcoming(gen));
            try {
                delegate.serializeWithType(value, gen, ctxt, typeSer);
            }
            finally {
                instances.remove(value);
            }
        }

        /**
         * Where the object about to be written will be: a level below the context it is written into, at the position
         * the context is about to move to.
         */
        private static DomainClassSerializer.Frame upcoming(JsonGenerator gen) {
            TokenStreamContext current = gen.streamWriteContext();
            StringBuilder path = new StringBuilder();
            for (TokenStreamContext context = current; context != null && !context.inRoot(); context = context.getParent()) {
                int index = context == current ? context.getEntryCount() : context.getCurrentIndex();
                path.insert(0, context.inArray() ? "[" + index + "]" : "." + context.currentName());
            }
            return new DomainClassSerializer.Frame(current.getNestingDepth() + 1, path.toString());
        }

        @Override
        public void resolve(SerializationContext ctxt) {
            delegate.resolve(ctxt);
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
            ValueSerializer<?> contextual = delegate.createContextual(ctxt, property);
            return contextual == delegate ? this : new Tracking(contextual, behaviour);
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
}
