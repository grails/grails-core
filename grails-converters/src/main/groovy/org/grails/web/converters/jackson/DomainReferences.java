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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.bean.BeanSerializerBase;

import org.grails.datastore.mapping.model.PersistentEntity;

/**
 * Writes the references to associated domain class instances: an object of the class name, when it is included, and the
 * id, named as the referenced class names them for the mapper, by its naming strategy, {@code @JsonProperty} and
 * {@code @JsonNaming}. An associated instance of a class with a serializer of its own is written with it.
 *
 * @since 9.0
 */
final class DomainReferences {

    private final Map<Class<?>, String> idNames = new ConcurrentHashMap<>();

    private final Map<Class<?>, String> classNames = new ConcurrentHashMap<>();

    /**
     * Writes an associated instance that is not written in full: a reference to it, or what the rendering writes in its
     * place, unless its class has a serializer of its own, such as one with a {@code @JsonValue}, one annotated
     * {@code @JsonSerialize} or one a module has a serializer for, which writes it, as it does wherever else it is written.
     */
    private void writeAssociated(Object associated, PersistentEntity entity, JsonGenerator generator,
            SerializationContext context, DomainClassRendering rendering) {
        if (associated == null) {
            generator.writeNull();
            return;
        }
        if (rendering.writeReference(associated, entity.getIdentity(), entity)) {
            return;
        }
        boolean proxy = DomainClassRendering.isProxyClass(associated.getClass());
        if (hasSerializerOfItsOwn(proxy ? entity.getJavaClass() : associated.getClass(), context)) {
            Object written = proxy ? rendering.unwrap(associated) : associated;
            if (written == null) {
                // a proxy of an instance that no longer exists
                generator.writeNull();
            }
            else {
                context.writeValue(generator, written);
            }
            return;
        }
        writeReference(associated, entity, generator, context, rendering);
    }

    /**
     * Writes a reference to an instance, such as one reached again while it is being written, or what the rendering
     * writes in its place.
     */
    void write(Object reference, PersistentEntity entity, JsonGenerator generator, SerializationContext context,
            DomainClassRendering rendering) {
        if (reference == null) {
            generator.writeNull();
            return;
        }
        if (rendering.writeReference(reference, entity.getIdentity(), entity)) {
            return;
        }
        writeReference(reference, entity, generator, context, rendering);
    }

    private void writeReference(Object reference, PersistentEntity entity, JsonGenerator generator,
            SerializationContext context, DomainClassRendering rendering) {
        Object id = rendering.referenceId(reference, entity.getIdentity());
        generator.writeStartObject();
        if (rendering.isIncludeClass()) {
            generator.writeStringProperty(name(classNames, context, entity.getJavaClass(), "class"), entity.getName());
        }
        if (id != null) {
            generator.writeName(name(idNames, context, entity.getJavaClass(), "id"));
            // DomainClassMarshaller wrote the id of a reference with the converter's writer, which quotes the string of
            // an id that is not a number, such as a MongoDB ObjectId; the id of an instance itself is written as any
            // other value, by the mapper, as the converter rendered it in Grails 8 as well
            if (id instanceof Number || id instanceof Boolean) {
                context.writeValue(generator, id);
            }
            else {
                generator.writeString(id.toString());
            }
        }
        generator.writeEndObject();
    }

    /**
     * Whether Jackson writes the instances of a domain class with a serializer other than its bean serializer, inside
     * the serializers modules wrap it with.
     */
    private static boolean hasSerializerOfItsOwn(Class<?> type, SerializationContext context) {
        ValueSerializer<?> serializer = context.findValueSerializer(type);
        while (!(serializer instanceof BeanSerializerBase)) {
            ValueSerializer<?> delegatee = serializer.getDelegatee();
            if (delegatee == null || delegatee == serializer) {
                return true;
            }
            serializer = delegatee;
        }
        return false;
    }

    private static String name(Map<Class<?>, String> cache, SerializationContext context, Class<?> type, String property) {
        return cache.computeIfAbsent(type, t -> {
            for (BeanPropertyDefinition definition : context.introspectBeanDescription(context.constructType(t)).findProperties()) {
                if (property.equals(definition.getInternalName())) {
                    return definition.getName();
                }
            }
            return property;
        });
    }

    /**
     * Writes an association as references, or in full when the rendering is deep, with the property of the association,
     * so that what it is annotated with, such as {@code @JsonIgnoreProperties}, applies to the instances, as it does to
     * any bean.
     */
    ValueSerializer<Object> serializer(PersistentEntity associated, boolean toMany, DomainRendering rendering,
            BeanProperty property) {
        return new ReferenceSerializer(this, associated, toMany, rendering, property);
    }

    private static final class ReferenceSerializer extends ValueSerializer<Object> {

        private final DomainReferences references;

        private final PersistentEntity associated;

        private final boolean toMany;

        private final DomainRendering rendering;

        private final BeanProperty property;

        private ReferenceSerializer(DomainReferences references, PersistentEntity associated, boolean toMany,
                DomainRendering rendering, BeanProperty property) {
            this.references = references;
            this.associated = associated;
            this.toMany = toMany;
            this.rendering = rendering;
            this.property = property;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext context, BeanProperty property) {
            return property == this.property ? this : new ReferenceSerializer(references, associated, toMany, rendering, property);
        }

        @Override
        public void serialize(Object value, JsonGenerator generator, SerializationContext context) {
            DomainClassRendering current = rendering.ofOwner(context, generator);
            if (deep(current, context)) {
                Object unwrapped = current.unwrap(value);
                if (unwrapped == null) {
                    // a proxy of an instance that no longer exists
                    generator.writeNull();
                    return;
                }
                Object written = DomainWrites.copied(unwrapped);
                serializer(written, context).serialize(written, generator, context);
            }
            else if (!toMany) {
                references.writeAssociated(value, associated, generator, context, current);
            }
            else if (value instanceof Collection<?> collection) {
                generator.writeStartArray();
                for (Object reference : collection) {
                    references.writeAssociated(reference, associated, generator, context, current);
                }
                generator.writeEndArray();
            }
            else if (value instanceof Map<?, ?> map) {
                generator.writeStartObject();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    writeKey(entry.getKey(), generator, context);
                    references.writeAssociated(entry.getValue(), associated, generator, context, current);
                }
                generator.writeEndObject();
            }
            else {
                context.writeValue(generator, value);
            }
        }

        @Override
        public void serializeWithType(Object value, JsonGenerator generator, SerializationContext context,
                TypeSerializer typeSerializer) {
            DomainClassRendering current = rendering.ofOwner(context, generator);
            if (deep(current, context) && !toMany) {
                // In full, an instance is written with its type id; a reference carries none
                Object unwrapped = current.unwrap(value);
                if (unwrapped == null) {
                    generator.writeNull();
                    return;
                }
                Object written = DomainWrites.copied(unwrapped);
                context.findPrimaryPropertySerializer(written.getClass(), property).serializeWithType(written, generator,
                        context, typeSerializer);
            }
            else {
                serialize(value, generator, context);
            }
        }

        /** Whether associated instances are written in full: for every write, or for this one. */
        private static boolean deep(DomainClassRendering rendering, SerializationContext context) {
            return rendering.isDeep() || Boolean.TRUE.equals(context.getAttribute(DomainClassJacksonModule.DEEP));
        }

        /**
         * The serializer of an association written in full: of the type it is declared with, for a collection or map, so
         * that its instances are written with the type ids their declared type asks for, as Jackson writes them.
         */
        private ValueSerializer<Object> serializer(Object written, SerializationContext context) {
            if (toMany && property != null && property.getType().getRawClass().isInstance(written)) {
                return context.findPrimaryPropertySerializer(property.getType(), property);
            }
            return context.findPrimaryPropertySerializer(written.getClass(), property);
        }

        @Override
        public boolean isEmpty(SerializationContext context, Object value) {
            if (value instanceof Collection<?> collection) {
                return collection.isEmpty();
            }
            return value instanceof Map<?, ?> map ? map.isEmpty() : value == null;
        }

        private static void writeKey(Object key, JsonGenerator generator, SerializationContext context) {
            if (key == null) {
                generator.writeName("null");
            }
            else if (key instanceof String name) {
                generator.writeName(name);
            }
            else {
                context.findKeySerializer(key.getClass(), null).serialize(key, generator, context);
            }
        }
    }
}
