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

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.util.NameTransformer;

/**
 * Writes a persistent property of a domain class instance as Jackson writes it, when the rendering of the write and its
 * projection include the property.
 *
 * @since 9.0
 */
final class DomainPropertyWriter extends BeanPropertyWriter {

    private final BeanPropertyWriter delegate;

    private final String property;

    private final DomainRendering rendering;

    DomainPropertyWriter(BeanPropertyWriter delegate, String property, DomainRendering rendering) {
        super(delegate);
        this.delegate = delegate;
        this.property = property;
        this.rendering = rendering;
    }

    @Override
    public BeanPropertyWriter rename(NameTransformer transformer) {
        BeanPropertyWriter renamed = delegate.rename(transformer);
        return renamed == delegate ? this : new DomainPropertyWriter(renamed, property, rendering);
    }

    @Override
    public void assignSerializer(ValueSerializer<Object> serializer) {
        delegate.assignSerializer(serializer);
    }

    @Override
    public void assignNullSerializer(ValueSerializer<Object> serializer) {
        delegate.assignNullSerializer(serializer);
    }

    @Override
    public void serializeAsProperty(Object bean, JsonGenerator generator, SerializationContext context) throws Exception {
        if (included(bean, context)) {
            delegate.serializeAsProperty(bean, generator, context);
        }
    }

    @Override
    public void serializeAsElement(Object bean, JsonGenerator generator, SerializationContext context) throws Exception {
        if (included(bean, context)) {
            delegate.serializeAsElement(bean, generator, context);
        }
        else {
            delegate.serializeAsOmittedElement(bean, generator, context);
        }
    }

    @Override
    public void depositSchemaProperty(JsonObjectFormatVisitor visitor, SerializationContext context) {
        delegate.depositSchemaProperty(visitor, context);
    }

    private boolean included(Object bean, SerializationContext context) {
        return rendering.of(context).includes(bean, property) && DomainWrites.projected(context, bean, property, getName());
    }
}
