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
package org.grails.web.databinding.bindingsource

import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

import groovy.transform.CompileStatic
import groovy.json.JsonException
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import jakarta.annotation.PostConstruct

import tools.jackson.core.JacksonException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectReader
import tools.jackson.databind.json.JsonMapper

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext

import grails.databinding.CollectionDataBindingSource
import grails.databinding.DataBindingSource
import grails.databinding.SimpleMapDataBindingSource
import grails.web.mime.MimeType
import org.grails.core.lifecycle.ShutdownOperations
import org.grails.databinding.bindingsource.DataBindingSourceCreationException
import org.grails.web.json.JSONObject

/**
 * Creates DataBindingSource objects from JSON in the request body
 *
 * @since 2.3
 * @author Jeff Brown
 * @author Graeme Rocher
 *
 * @see DataBindingSource
 * @see org.grails.databinding.bindingsource.DataBindingSourceCreator
 */
@CompileStatic
@Slf4j
class JsonDataBindingSourceCreator extends AbstractRequestBodyDataBindingSourceCreator {

    private static final Pattern INDEX_PATTERN = ~/^(\S+)\[(\d+)\]$/

    // Resolved when a request body is first parsed rather than injected. Injecting it pulls
    // Jackson's auto-configuration into this bean's graph, and MimeTypesConfiguration depends on
    // this creator, so Boot's mapper would be built before GORM has initialized.
    @Autowired(required = false)
    private ObjectProvider<JsonMapper> jsonMapperProvider

    @Autowired(required = false)
    private ApplicationContext applicationContext

    /**
     * Whether request bodies are parsed with the application's Jackson mapper, as {@code grails.databinding.json.jackson}
     * sets it, rather than with the legacy parser.
     *
     * @since 9.0
     */
    @Value('${grails.databinding.json.jackson:false}')
    boolean useJackson = false

    /**
     * Parses request bodies on the legacy path; an application bean replaces it, as on Grails 8.
     *
     * @deprecated Scheduled for removal in Grails 11, with legacy JSON request parsing. Configure the application's
     * Jackson mapper, which {@code grails.databinding.json.jackson} parses request bodies with.
     */
    @Deprecated(since = '9.0', forRemoval = true)
    @Autowired(required = false)
    JsonSlurper jsonSlurper = new JsonSlurper()

    // The JSON, HAL and JSON API creators share the setting, so the notice is logged once per
    // application rather than once per creator
    private static final AtomicBoolean LEGACY_PARSING_REPORTED = new AtomicBoolean()

    static {
        ShutdownOperations.addOperation({ LEGACY_PARSING_REPORTED.set(false) } as Runnable, true)
    }

    /**
     * Logs, once for each application, that request bodies are parsed by the legacy parser, unless {@link #useJackson}
     * has the application's Jackson mapper parse them; private, so that a method of a subclass of the same name is not
     * called in its place.
     */
    @PostConstruct
    private void reportLegacyParsing() {
        if (!useJackson && LEGACY_PARSING_REPORTED.compareAndSet(false, true)) {
            log.warn('Legacy JSON request parsing is deprecated for removal in Grails 11. ' +
                    'Set grails.databinding.json.jackson=true to opt into Jackson; it becomes the default in Grails 10.')
        }
    }

    private volatile JsonMapper resolvedJsonMapper

    /**
     * @return the mapper request bodies are parsed with when {@link #useJackson} is set: the one
     *         {@link #setJsonMapper} sets, or else the application's {@code JsonMapper}, found when a body is first
     *         parsed: its only or primary one, or else Spring Boot's {@code jacksonJsonMapper}; or a default
     *         {@code JsonMapper} where the application has none of those, as when it has several and none is primary;
     *         bodies are read with a copy of it without its default typing where it has any
     * @since 9.0
     */
    JsonMapper getJsonMapper() {
        JsonMapper mapper = this.resolvedJsonMapper
        if (mapper == null) {
            mapper = jsonMapperProvider?.getIfUnique()
            if (mapper == null && applicationContext?.containsBean('jacksonJsonMapper')) {
                mapper = applicationContext.getBean('jacksonJsonMapper', JsonMapper)
            }
            mapper = mapper ?: JsonMapper.builder().build()
            this.resolvedJsonMapper = mapper
        }
        return mapper
    }

    /**
     * @param jsonMapper the mapper to parse request bodies with, in the place of the application's
     * @since 9.0
     */
    void setJsonMapper(JsonMapper jsonMapper) {
        this.resolvedJsonMapper = jsonMapper
    }

    /**
     * Reads untyped JSON values. Decimals are read as {@link BigDecimal} so that binding a
     * fractional value to a BigDecimal property keeps the digits the request sent; reading them
     * as doubles first would round them before the binder ever saw them. A body is read as it is, without a root value,
     * which the mapper would otherwise expect for {@code Object} where it unwraps root values, and its arrays as lists and
     * its whole numbers as the smallest of {@code Integer}, {@code Long} and {@code BigInteger} that holds them, as the
     * legacy parser reads them, whatever the mapper's features say, and without the type ids the mapper's default typing
     * would have a body carry, so that a body binds without them and cannot name a class to create.
     *
     * @since 9.0
     */
    protected ObjectReader untypedReader() {
        return bodyMapper().reader().forType(Object).withoutRootName()
                .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .without(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, DeserializationFeature.USE_BIG_INTEGER_FOR_INTS,
                        DeserializationFeature.USE_LONG_FOR_INTS)
    }

    /** The mapper request bodies are read with, and the application's mapper it is derived from. */
    private volatile Tuple2<JsonMapper, JsonMapper> derivedBodyMapper

    /**
     * The application's mapper, or, where it has default typing, a copy of it without, derived once for each mapper, so
     * that a mapper of the application's own class without it reads as it does.
     */
    private JsonMapper bodyMapper() {
        JsonMapper source = getJsonMapper()
        Tuple2<JsonMapper, JsonMapper> derived = derivedBodyMapper
        if (derived == null || !derived.v1.is(source)) {
            boolean typing = source.deserializationConfig().getDefaultTyper(source.constructType(Object)) != null
            derived = new Tuple2<JsonMapper, JsonMapper>(source, typing ? source.rebuild().deactivateDefaultTyping().build() : source)
            derivedBodyMapper = derived
        }
        derived.v2
    }

    @Override
    MimeType[] getMimeTypes() {
        [MimeType.JSON, MimeType.TEXT_JSON] as MimeType[]
    }

    @Override
    DataBindingSource createDataBindingSource(MimeType mimeType, Class bindingTargetType, Object bindingSource) {
        if (bindingSource instanceof Map) {
            return new SimpleMapDataBindingSource(createJsonMap(bindingSource))
        }
        else if (bindingSource instanceof JSONObject) {
            return new SimpleMapDataBindingSource((JSONObject) bindingSource)
        }
        else {
            return super.createDataBindingSource(mimeType, bindingTargetType, bindingSource)
        }
    }

    @Override
    protected CollectionDataBindingSource createCollectionBindingSource(Reader reader) {

        Object jsonElement = parse(reader)
        def dataBindingSources = jsonElement.collect { element ->
            if (element instanceof Map) {
                new SimpleMapDataBindingSource(createJsonMap(element))
            }
            else {
                new SimpleMapDataBindingSource(Collections.emptyMap())
            }
        }
        return new CollectionDataBindingSource() {
            List<DataBindingSource> getDataBindingSources() {
                (List<DataBindingSource>) dataBindingSources
            }
        }
    }

    @Override
    protected DataBindingSource createBindingSource(Reader reader) {
        final Object jsonElement = parse(reader)

        if (jsonElement instanceof Map) {
            return new SimpleMapDataBindingSource(createJsonMap(jsonElement))
        }
        else {
            return new SimpleMapDataBindingSource(Collections.emptyMap())
        }

    }

    protected Map createJsonMap(Object jsonElement) {
        (Map) jsonElement
    }

    private Object parse(Reader reader) {
        return useJackson ? untypedReader().readValue(reader) : jsonSlurper.parse(reader)
    }

    @Override
    protected DataBindingSourceCreationException createBindingSourceCreationException(Exception e) {
        if (e instanceof JacksonException || e instanceof JsonException) {
            return new InvalidRequestBodyException(e)
        }
        return super.createBindingSourceCreationException(e)
    }
}
