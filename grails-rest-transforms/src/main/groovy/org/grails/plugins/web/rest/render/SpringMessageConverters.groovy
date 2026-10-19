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
package org.grails.plugins.web.rest.render

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

import groovy.transform.CompileStatic

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter

/**
 * Resolves the message converters Spring MVC ends up configured with at response time.
 *
 * <p>Renderers need the same converter list, in the same order, that the handler adapter uses.
 * Injecting the adapter to read them forces the whole MVC infrastructure to be created from a
 * renderer bean, which risks circular dependencies and defeats lazy startup. A provider delays
 * that lookup until a response is written and works without deprecated MVC callbacks.</p>
 *
 * @since 9.0
 */
@CompileStatic
class SpringMessageConverters {

    /** Spring's base of the converters of Jackson 2, named so that this class does not depend on it. */
    private static final String JACKSON_2_CONVERTER = 'org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter'

    /**
     * The charset to have a converter write a body in, which a writer then decodes and the response's writer encodes in
     * the response's encoding: UTF-8 for a converter of Jackson 3 or Jackson 2, which writes only UTF encodings and UTF-8
     * for any other it is given, and the response's encoding for any other converter.
     *
     * @param converter a converter
     * @param encoding the encoding of the response
     * @return the charset
     */
    static Charset writeCharset(HttpMessageConverter<?> converter, String encoding) {
        return converter instanceof AbstractJacksonHttpMessageConverter || isJackson2(converter) ?
                StandardCharsets.UTF_8 : Charset.forName(encoding)
    }

    private static boolean isJackson2(HttpMessageConverter<?> converter) {
        for (Class<?> type = converter.getClass(); type != null; type = type.superclass) {
            if (type.name == JACKSON_2_CONVERTER) {
                return true
            }
        }
        return false
    }

    @Autowired(required = false)
    @Qualifier('requestMappingHandlerAdapter')
    ObjectProvider<RequestMappingHandlerAdapter> handlerAdapter

    private volatile List<HttpMessageConverter<?>> converters = List.of()

    void setConverters(List<HttpMessageConverter<?>> converters) {
        // Lightweight web test slices have no MVC handler adapter. They supply their converters
        // here; production always uses the handler adapter's final list when it is available.
        this.converters = Collections.unmodifiableList(converters)
    }

    /**
     * @return the converters MVC is configured with, or an empty list before initialization; read
     * when a response is written so that every configurer's contribution is included
     */
    List<HttpMessageConverter<?>> getConverters() {
        RequestMappingHandlerAdapter adapter = handlerAdapter?.getIfAvailable()
        return adapter == null ? converters : Collections.unmodifiableList(adapter.messageConverters)
    }
}
