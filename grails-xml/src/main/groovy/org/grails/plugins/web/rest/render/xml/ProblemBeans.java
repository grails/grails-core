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
package org.grails.plugins.web.rest.render.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.introspect.VirtualAnnotatedMember;
import tools.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.FilterProvider;
import tools.jackson.databind.ser.PropertyFilter;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.ser.VirtualBeanPropertyWriter;
import tools.jackson.databind.util.NameTransformer;
import tools.jackson.databind.util.SimpleBeanPropertyDefinition;

/**
 * A copy of a mapper whose bean serializers write a marker ahead of the properties of each bean, so that a problem
 * converted to maps says which of its maps were beans: Jackson's XML converter writes the properties of a bean in no
 * namespace, and the entries of a map in that of the element the map is in.
 *
 * @since 9.0
 */
final class ProblemBeans {

    /** The key of the marker, which no property of a bean has. */
    static final String MARKER = ProblemBeans.class.getName() + "#bean";

    /**
     * The first element of the array a bean is written as, where its format asks for one, so that the array is not taken
     * for a collection, which a property of a bean wraps.
     */
    static final String ARRAY_MARKER = ProblemBeans.class.getName() + "#array";

    private static final Map<ObjectMapper, ObjectMapper> MARKING = Collections.synchronizedMap(new WeakHashMap<>());

    private ProblemBeans() {
    }

    /**
     * @param mapper a mapper
     * @return a copy of it that marks the beans it converts, made once for each mapper
     */
    static ObjectMapper marking(ObjectMapper mapper) {
        return MARKING.computeIfAbsent(mapper, source -> {
            MapperBuilder<?, ?> builder = source.rebuild().addModule(new SimpleModule("grails-problem-beans")
                    .setSerializerModifier(new Marker()));
            FilterProvider filters = source.serializationConfig().getFilterProvider();
            if (filters != null) {
                // the property filters of a bean filter its properties, never the marker
                builder.filterProvider(new MarkerKeepingFilters(filters));
            }
            return builder.build();
        });
    }

    private static final class Marker extends ValueSerializerModifier {

        private static final long serialVersionUID = 1L;

        // once the properties are filtered, so that the inclusions and ignorals of the bean keep the marker
        @Override
        public List<BeanPropertyWriter> orderProperties(SerializationConfig config, BeanDescription.Supplier beanDescription,
                List<BeanPropertyWriter> properties) {
            BeanDescription description = beanDescription.get();
            JavaType type = config.getTypeFactory().constructType(Boolean.class);
            BeanPropertyDefinition definition = SimpleBeanPropertyDefinition.construct(config,
                    new VirtualAnnotatedMember(description.getClassInfo(), description.getBeanClass(), MARKER, type),
                    PropertyName.construct(MARKER));
            List<BeanPropertyWriter> marked = new ArrayList<>(properties.size() + 1);
            marked.add(new MarkerWriter(definition, description, type));
            marked.addAll(properties);
            return marked;
        }
    }

    private static final class MarkerKeepingFilters extends FilterProvider {

        private static final long serialVersionUID = 1L;

        private final FilterProvider filters;

        MarkerKeepingFilters(FilterProvider filters) {
            this.filters = filters;
        }

        @Override
        public FilterProvider snapshot() {
            return new MarkerKeepingFilters(filters.snapshot());
        }

        @Override
        public PropertyFilter findPropertyFilter(SerializationContext context, Object filterId, Object valueToFilter) {
            PropertyFilter filter = filters.findPropertyFilter(context, filterId, valueToFilter);
            return filter == null ? null : new MarkerKeepingFilter(filter);
        }
    }

    private static final class MarkerKeepingFilter implements PropertyFilter {

        private final PropertyFilter filter;

        MarkerKeepingFilter(PropertyFilter filter) {
            this.filter = filter;
        }

        @Override
        public PropertyFilter snapshot() {
            return new MarkerKeepingFilter(filter.snapshot());
        }

        @Override
        public void serializeAsProperty(Object pojo, JsonGenerator generator, SerializationContext context,
                PropertyWriter writer) throws Exception {
            if (writer instanceof MarkerWriter) {
                writer.serializeAsProperty(pojo, generator, context);
            }
            else {
                filter.serializeAsProperty(pojo, generator, context, writer);
            }
        }

        @Override
        public void serializeAsElement(Object elementValue, JsonGenerator generator, SerializationContext context,
                PropertyWriter writer) throws Exception {
            if (!(writer instanceof MarkerWriter)) {
                filter.serializeAsElement(elementValue, generator, context, writer);
            }
        }

        @Override
        public void depositSchemaProperty(PropertyWriter writer, JsonObjectFormatVisitor visitor, SerializationContext context) {
            filter.depositSchemaProperty(writer, visitor, context);
        }
    }

    private static final class MarkerWriter extends VirtualBeanPropertyWriter {

        private static final long serialVersionUID = 1L;

        MarkerWriter(BeanPropertyDefinition definition, BeanDescription description, JavaType type) {
            super(definition, description.getClassAnnotations(), type);
            this.silent = false;
        }

        /** Whether it writes nothing, as the marker of a bean unwrapped into another, which has its own. */
        private final boolean silent;

        private MarkerWriter(MarkerWriter base, PropertyName name, boolean silent) {
            super(base, name);
            this.silent = silent;
        }

        @Override
        protected BeanPropertyWriter _new(PropertyName newName) {
            return new MarkerWriter(this, newName, silent);
        }

        // the marker of a bean unwrapped with a prefix or suffix takes its name, so that it does not conflict with the
        // marker of the bean it is unwrapped into, and writes nothing
        @Override
        public BeanPropertyWriter rename(NameTransformer transformer) {
            return new MarkerWriter(this, PropertyName.construct(transformer.transform(getName())), true);
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator generator, SerializationContext context) throws Exception {
            if (!silent) {
                super.serializeAsProperty(bean, generator, context);
            }
        }

        // a bean written as an array, as its format asks, has no names to mark it with: the array is marked instead
        @Override
        public void serializeAsElement(Object bean, JsonGenerator generator, SerializationContext context) {
            if (!silent) {
                generator.writeString(ARRAY_MARKER);
            }
        }

        @Override
        protected Object value(Object bean, JsonGenerator generator, SerializationContext context) {
            return Boolean.TRUE;
        }

        @Override
        public VirtualBeanPropertyWriter withConfig(MapperConfig<?> config, AnnotatedClass declaringClass,
                BeanPropertyDefinition propDef, JavaType type) {
            return this;
        }
    }
}
