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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotationIntrospectorPair;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.introspect.NopAnnotationIntrospector;
import tools.jackson.databind.introspect.VirtualAnnotatedMember;
import tools.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.AnyGetterWriter;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.BeanSerializerBuilder;
import tools.jackson.databind.ser.FilterProvider;
import tools.jackson.databind.ser.PropertyFilter;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.ser.VirtualBeanPropertyWriter;
import tools.jackson.databind.ser.impl.FilteredBeanPropertyWriter;
import tools.jackson.databind.util.NameTransformer;
import tools.jackson.databind.util.SimpleBeanPropertyDefinition;

import org.springframework.http.ProblemDetail;

/**
 * A copy of a mapper whose bean serializers write a marker ahead of the properties of each bean, so that a problem
 * converted to maps says which of its maps were beans: Jackson's XML converter writes the properties of a bean in no
 * namespace, and the entries of a map in that of the element the map is in.
 *
 * @since 9.0
 */
final class ProblemBeans {

    /**
     * The start of the key of the marker, which no property of a bean has. The key names the class of the bean too, so that
     * the marker of a bean another unwraps without a prefix differs from the marker of the other, as Jackson requires.
     */
    static final String MARKER = ProblemBeans.class.getName() + "#bean:";

    /**
     * An element of the array a bean is written as, where its format asks for one, so that the array is not taken for a
     * collection, which a property of a bean wraps: the first, unless a serializer modifier of another module of the
     * mapper orders the properties of the bean.
     */
    static final String ARRAY_MARKER = ProblemBeans.class.getName() + "#array";

    /** The keys of the markers written before and after the extension members of a problem, which its any-getter writes. */
    static final String ANY_START = MARKER + "any";

    static final String ANY_END = MARKER + "/any";

    private static final Map<ObjectMapper, ObjectMapper> MARKING = Collections.synchronizedMap(new WeakHashMap<>());

    private ProblemBeans() {
    }

    /**
     * @param key a key of a converted map
     * @return whether it is the key of the marker of a bean
     */
    static boolean isMarker(Object key) {
        return key instanceof String && ((String) key).startsWith(MARKER);
    }

    /**
     * @param mapper a mapper
     * @return a copy of it that marks the beans it converts, made once for each mapper
     */
    static ObjectMapper marking(ObjectMapper mapper) {
        return MARKING.computeIfAbsent(mapper, source -> {
            MapperBuilder<?, ?> builder = source.rebuild().addModule(new MarkingModule());
            FilterProvider filters = source.serializationConfig().getFilterProvider();
            if (filters != null) {
                // the property filters of a bean filter its properties, never the marker
                builder.filterProvider(new MarkerKeepingFilters(filters));
            }
            return builder.build();
        });
    }

    /**
     * Marks the beans and, set up after the modules of the mapper, makes the names a property includes of the bean it
     * holds include the marker of that bean.
     */
    private static final class MarkingModule extends SimpleModule {

        private static final long serialVersionUID = 1L;

        MarkingModule() {
            super("grails-problem-beans");
            setSerializerModifier(new Marker());
        }

        @Override
        public void setupModule(SetupContext context) {
            super.setupModule(context);
            // set up last, so that the introspector holds those every other module of the mapper adds
            MapperBuilder<?, ?> builder = (MapperBuilder<?, ?>) context.getOwner();
            AnnotationIntrospector introspector = builder.annotationIntrospector();
            if (introspector != null) {
                builder.annotationIntrospector(new MarkerIncluding(introspector));
            }
        }
    }

    /**
     * The introspector of a mapper, except that the names a property includes, with {@code @JsonIncludeProperties}, of
     * the bean it holds include the marker of that bean, whatever its class, and that a property that unwraps a bean
     * leaves out the bean's marker.
     */
    private static final class MarkerIncluding extends AnnotationIntrospectorPair {

        private static final long serialVersionUID = 1L;

        MarkerIncluding(AnnotationIntrospector introspector) {
            super(introspector, NopAnnotationIntrospector.instance);
        }

        @Override
        public JsonIncludeProperties.Value findPropertyInclusionByName(MapperConfig<?> config, Annotated annotated) {
            JsonIncludeProperties.Value inclusion = _primary.findPropertyInclusionByName(config, annotated);
            if (inclusion == null || inclusion.getIncluded() == null || unwrapsBean(config, annotated)) {
                return inclusion;
            }
            return new MarkerInclusion(inclusion);
        }

        // the properties of a bean a property unwraps are written into the bean that holds it, which has its own marker,
        // so that the bean's own would conflict with that of another bean of its class unwrapped there
        @Override
        public JsonIgnoreProperties.Value findPropertyIgnoralByName(MapperConfig<?> config, Annotated annotated) {
            JsonIgnoreProperties.Value ignoral = _primary.findPropertyIgnoralByName(config, annotated);
            return unwrapsBean(config, annotated) ? new MarkerIgnoral(ignoral == null ? JsonIgnoreProperties.Value.empty() : ignoral) :
                    ignoral;
        }

        /**
         * Whether the property unwraps the bean it holds: one declared as a container, a reference, such as an
         * {@code Optional}, or {@code Object} may hold a value Jackson writes as it is, whose elements take the inclusions
         * and ignorals of the property, and keep their markers.
         */
        private boolean unwrapsBean(MapperConfig<?> config, Annotated annotated) {
            if (!(annotated instanceof AnnotatedMember member) || _primary.findUnwrappingNameTransformer(config, member) == null) {
                return false;
            }
            JavaType type = member.getType();
            return !type.isContainerType() && !type.isReferenceType() && !type.isJavaLangObject();
        }
    }

    private static final class MarkerIgnoral extends JsonIgnoreProperties.Value {

        private static final long serialVersionUID = 1L;

        MarkerIgnoral(JsonIgnoreProperties.Value ignoral) {
            // ignored for serialization alone, as this mapper only writes
            super(new MarkersIgnored(ignoral.findIgnoredForSerialization()), ignoral.getIgnoreUnknown(), false,
                    ignoral.getAllowSetters(), ignoral.getMerge());
        }
    }

    /** Names that ignore the marker of every bean as well. */
    private static final class MarkersIgnored extends HashSet<String> {

        private static final long serialVersionUID = 1L;

        MarkersIgnored(Set<String> names) {
            super(names);
        }

        @Override
        public boolean contains(Object name) {
            return isMarker(name) || super.contains(name);
        }
    }

    private static final class MarkerInclusion extends JsonIncludeProperties.Value {

        private static final long serialVersionUID = 1L;

        MarkerInclusion(JsonIncludeProperties.Value inclusion) {
            super(new MarkersIncluded(inclusion.getIncluded()), inclusion.getOrdered());
        }
    }

    /** Names that include the marker of every bean as well. */
    private static final class MarkersIncluded extends HashSet<String> {

        private static final long serialVersionUID = 1L;

        MarkersIncluded(Set<String> names) {
            super(names);
        }

        @Override
        public boolean contains(Object name) {
            return isMarker(name) || super.contains(name);
        }
    }

    private static final class Marker extends ValueSerializerModifier {

        private static final long serialVersionUID = 1L;

        // once the properties are filtered, so that the inclusions and ignorals of the bean's class keep the marker;
        // those of a property holding the bean keep it through MarkerIncluding
        @Override
        public List<BeanPropertyWriter> orderProperties(SerializationConfig config, BeanDescription.Supplier beanDescription,
                List<BeanPropertyWriter> properties) {
            BeanDescription description = beanDescription.get();
            if (properties.isEmpty() && description.findAnyGetter() == null) {
                // a bean without properties is an empty value, which Jackson leaves out where the inclusion says so,
                // unless an any-getter writes its entries
                return properties;
            }
            JavaType type = config.getTypeFactory().constructType(Boolean.class);
            String name = MARKER + description.getBeanClass().getName();
            BeanPropertyDefinition definition = SimpleBeanPropertyDefinition.construct(config,
                    new VirtualAnnotatedMember(description.getClassInfo(), description.getBeanClass(), name, type),
                    PropertyName.construct(name));
            List<BeanPropertyWriter> marked = new ArrayList<>(properties.size() + 1);
            marked.add(new MarkerWriter(definition, description, type));
            marked.addAll(properties);
            return marked;
        }

        // the entries of the any-getter of a problem are marked, so that its extension members, which Spring's converter
        // writes in its namespace, are told from the properties of a subclass of the same name, and from the entries of
        // the any-getter of a bean it unwraps, which it writes in none
        @Override
        public BeanSerializerBuilder updateBuilder(SerializationConfig config, BeanDescription.Supplier beanDescription,
                BeanSerializerBuilder builder) {
            List<BeanPropertyWriter> properties = new ArrayList<>(builder.getProperties());
            BeanPropertyWriter[] filtered = builder.getFilteredProperties() == null ? null :
                    builder.getFilteredProperties().clone();
            for (int i = 0; i < properties.size(); i++) {
                if (properties.get(i) instanceof AnyGetterWriter anyGetter && !(anyGetter instanceof MarkedAnyGetter) &&
                        ProblemDetail.class.isAssignableFrom(anyGetter.getMember().getDeclaringClass())) {
                    MarkedAnyGetter marked = new MarkedAnyGetter(anyGetter);
                    properties.set(i, marked);
                    if (filtered != null && filtered[i] != null) {
                        filtered[i] = filtered[i] == anyGetter ? marked :
                                FilteredBeanPropertyWriter.constructViewBased(marked, anyGetter.getViews());
                    }
                }
            }
            builder.setProperties(properties);
            builder.setFilteredProperties(filtered);
            return builder;
        }
    }

    /** An any-getter that writes a marker before and after its entries, and is otherwise the one it marks. */
    private static final class MarkedAnyGetter extends AnyGetterWriter {

        private static final long serialVersionUID = 1L;

        private final AnyGetterWriter anyGetter;

        MarkedAnyGetter(AnyGetterWriter anyGetter) {
            super(anyGetter, null, anyGetter.getMember(), null);
            this.anyGetter = anyGetter;
        }

        @Override
        public void fixAccess(SerializationConfig config) {
            anyGetter.fixAccess(config);
        }

        @Override
        public void resolve(SerializationContext context) {
            anyGetter.resolve(context);
        }

        @Override
        public void getAndSerialize(Object bean, JsonGenerator generator, SerializationContext context) throws Exception {
            generator.writeName(ANY_START);
            generator.writeBoolean(true);
            anyGetter.getAndSerialize(bean, generator, context);
            generator.writeName(ANY_END);
            generator.writeBoolean(true);
        }

        @Override
        public void getAndFilter(Object bean, JsonGenerator generator, SerializationContext context, PropertyFilter filter)
                throws Exception {
            generator.writeName(ANY_START);
            generator.writeBoolean(true);
            anyGetter.getAndFilter(bean, generator, context, filter);
            generator.writeName(ANY_END);
            generator.writeBoolean(true);
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
            // by its name, since a view the mapper writes in wraps it
            if (isMarker(writer.getName())) {
                writer.serializeAsProperty(pojo, generator, context);
            }
            else {
                filter.serializeAsProperty(pojo, generator, context, writer);
            }
        }

        @Override
        public void serializeAsElement(Object elementValue, JsonGenerator generator, SerializationContext context,
                PropertyWriter writer) throws Exception {
            if (!isMarker(writer.getName())) {
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
            // in every view, so that a view the mapper writes in, which leaves out the properties of no view, keeps it
            super(definition, description.getClassAnnotations(), type, null, null, null, definition.findInclusion(),
                    new Class<?>[] {Object.class});
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
