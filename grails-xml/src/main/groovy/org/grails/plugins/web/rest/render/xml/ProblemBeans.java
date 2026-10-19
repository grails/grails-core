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

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.introspect.VirtualAnnotatedMember;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.ser.VirtualBeanPropertyWriter;
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

    private static final Map<ObjectMapper, ObjectMapper> MARKING = Collections.synchronizedMap(new WeakHashMap<>());

    private ProblemBeans() {
    }

    /**
     * @param mapper a mapper
     * @return a copy of it that marks the beans it converts, made once for each mapper
     */
    static ObjectMapper marking(ObjectMapper mapper) {
        return MARKING.computeIfAbsent(mapper, source -> source.rebuild().addModule(new SimpleModule("grails-problem-beans")
                .setSerializerModifier(new Marker())).build());
    }

    private static final class Marker extends ValueSerializerModifier {

        private static final long serialVersionUID = 1L;

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription.Supplier beanDescription,
                List<BeanPropertyWriter> properties) {
            BeanDescription description = beanDescription.get();
            JsonFormat.Value format = beanDescription.findExpectedFormat(description.getBeanClass());
            if (format != null && format.getShape() == JsonFormat.Shape.ARRAY) {
                // a bean written as an array has no names to mark it with
                return properties;
            }
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

    private static final class MarkerWriter extends VirtualBeanPropertyWriter {

        private static final long serialVersionUID = 1L;

        MarkerWriter(BeanPropertyDefinition definition, BeanDescription description, JavaType type) {
            super(definition, description.getClassAnnotations(), type);
        }

        private MarkerWriter(MarkerWriter base, PropertyName name) {
            super(base, name);
        }

        @Override
        protected BeanPropertyWriter _new(PropertyName newName) {
            return new MarkerWriter(this, newName);
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
