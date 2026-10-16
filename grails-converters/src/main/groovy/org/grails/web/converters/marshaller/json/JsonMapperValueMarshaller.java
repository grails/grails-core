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
package org.grails.web.converters.marshaller.json;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import grails.converters.JSON;
import org.grails.web.converters.configuration.ConvertersConfigurationHolder;
import org.grails.web.converters.exceptions.ConverterException;
import org.grails.web.converters.marshaller.ObjectMarshaller;
import org.grails.web.json.JSONException;
import org.grails.web.json.JsonMapperValue;

/**
 * Renders a value with the {@link org.grails.web.json.JsonMapperSupport JsonMapper} that {@link JSON} writes through,
 * as Spring Boot renders it, when the mapper has a dedicated serializer for a single value of its type: dates and
 * times, {@code Month}, {@code UUID}, {@code Locale}, {@code byte[]}, types with a {@code @JsonValue} and the types
 * that Jackson modules registered with the application serialize
 * (see {@link org.grails.web.json.JsonMapperSupport#rendersValue(Class)}).
 *
 * <p>It also writes collections, maps, arrays, records and {@code Optional}, whose values it renders with the converter.
 * It is registered after {@code ProxyUnwrappingMarshaller} and the domain class marshaller, and ahead of the enum and
 * bean marshallers, so those never see such a value. Any marshaller an application registers takes precedence for the
 * types it supports.
 *
 * <p>A value nested in a value the mapper writes, such as a property that a Jackson module's serializer writes with
 * {@code JsonGenerator#writePOJO}, is rendered by the converter as any other value is, so marshallers apply to it.
 * Only the values the converter would render with the mapper anyway are left to the mapper.
 *
 * @since 9.0
 */
public class JsonMapperValueMarshaller implements ObjectMarshaller<JSON> {

    public boolean supports(Object object) {
        return isStructure(object) || ConvertersConfigurationHolder.getJsonMapperSupport().rendersValue(object.getClass());
    }

    /**
     * Whether the mapper writes the object as a structure of other values: a collection, a map, an array, a record or an
     * {@code Optional}. The converter renders each value in it, so that its marshallers apply to them.
     */
    private static boolean isStructure(Object object) {
        return object instanceof Collection || object instanceof Map || object.getClass().isArray() ||
                object.getClass().isRecord() || object instanceof Optional;
    }

    public void marshalObject(Object object, JSON converter) throws ConverterException {
        write(object, converter);
    }

    /**
     * Writes a value with the converter's mapper, and renders the values nested in it with the converter.
     */
    static void write(Object value, JSON converter) throws ConverterException {
        try {
            converter.getWriter().value(new JsonMapperValue(value, converter.getJsonMapperSupport(),
                    nested -> writeNested(nested, converter)));
        }
        catch (JSONException e) {
            throw converterException(e);
        }
    }

    private static boolean writeNested(Object nested, JSON converter) {
        // A single value is left to the mapper, but a structure goes back through the converter, which keeps track of the
        // objects it is rendering, so that one that contains itself is found
        if (converter.lookupObjectMarshaller(nested) instanceof JsonMapperValueMarshaller && !isStructure(nested)) {
            return false;
        }
        converter.getWriter().writeNested(() -> converter.convertAnother(nested));
        return true;
    }

    /**
     * A failure to render a nested value reaches here wrapped by the serializers it was nested in; it is reported as
     * the converter reported it.
     */
    private static ConverterException converterException(JSONException e) {
        for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof ConverterException converterException) {
                return converterException;
            }
        }
        return new ConverterException(e);
    }
}
