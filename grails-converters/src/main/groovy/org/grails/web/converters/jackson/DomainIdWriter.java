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

import java.util.Objects;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.BeanPropertyWriter;

import org.grails.datastore.mapping.model.PersistentProperty;

/**
 * Jackson's own writer of the id of a domain class instance, so that what Jackson applies to a property applies to it,
 * writing the value the rendering of the instance gives the id, as the version and the ids of associated instances are:
 * the JSON converter's domain class marshaller reads them with {@code extractValue}.
 *
 * @since 9.0
 */
final class DomainIdWriter extends BeanPropertyWriter {

    private final PersistentProperty identity;

    private final DomainRendering rendering;

    DomainIdWriter(BeanPropertyWriter writer, PersistentProperty identity, DomainRendering rendering) {
        super(writer);
        this.identity = identity;
        this.rendering = rendering;
    }

    private DomainIdWriter(DomainIdWriter base, PropertyName name) {
        super(base, name);
        this.identity = base.identity;
        this.rendering = base.rendering;
    }

    @Override
    protected BeanPropertyWriter _new(PropertyName newName) {
        return new DomainIdWriter(this, newName);
    }

    @Override
    public void serializeAsProperty(Object bean, JsonGenerator generator, SerializationContext context) throws Exception {
        Object rendered = rendering.of(context, bean).propertyValue(bean, identity);
        if (Objects.equals(rendered, get(bean))) {
            super.serializeAsProperty(bean, generator, context);
        }
        else if (rendered != null) {
            generator.writeName(_name);
            write(rendered, generator, context);
        }
    }

    @Override
    public void serializeAsElement(Object bean, JsonGenerator generator, SerializationContext context) throws Exception {
        Object rendered = rendering.of(context, bean).propertyValue(bean, identity);
        if (Objects.equals(rendered, get(bean))) {
            super.serializeAsElement(bean, generator, context);
        }
        else if (rendered != null) {
            write(rendered, generator, context);
        }
        else {
            generator.writeNull();
        }
    }

    /**
     * Writes the value the rendering gives the id with the serializer of the property, when the value is of its type,
     * and otherwise with the serializer Jackson finds for the value.
     */
    private void write(Object value, JsonGenerator generator, SerializationContext context) {
        ValueSerializer<Object> serializer = _serializer != null && _declaredType.getRawClass().isInstance(value) ?
                _serializer : context.findPrimaryPropertySerializer(value.getClass(), this);
        if (_typeSerializer == null) {
            serializer.serialize(value, generator, context);
        }
        else {
            serializer.serializeWithType(value, generator, context, _typeSerializer);
        }
    }
}
