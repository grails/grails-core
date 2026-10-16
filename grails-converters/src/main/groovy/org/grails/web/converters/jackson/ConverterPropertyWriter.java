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

import java.util.function.Function;
import java.util.function.Predicate;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.util.NameTransformer;

/**
 * Writes a property of a bean as Jackson writes it, when the converter writing it includes the property.
 *
 * @since 9.0
 */
final class ConverterPropertyWriter extends BeanPropertyWriter {

    private final BeanPropertyWriter delegate;

    private final String property;

    ConverterPropertyWriter(BeanPropertyWriter delegate, String property) {
        super(delegate);
        this.delegate = delegate;
        this.property = property;
    }

    @Override
    public BeanPropertyWriter rename(NameTransformer transformer) {
        BeanPropertyWriter renamed = delegate.rename(transformer);
        return renamed == delegate ? this : new ConverterPropertyWriter(renamed, property);
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

    @SuppressWarnings("unchecked")
    private boolean included(Object bean, SerializationContext context) {
        if (context.getAttribute(ConverterJsonModule.PROPERTY_FILTERS) instanceof Function<?, ?> filters) {
            Predicate<String> filter = ((Function<Object, Predicate<String>>) filters).apply(bean);
            return filter == null || filter.test(property);
        }
        return true;
    }
}
