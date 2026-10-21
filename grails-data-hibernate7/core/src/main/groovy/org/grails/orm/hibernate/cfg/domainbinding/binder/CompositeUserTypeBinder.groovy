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
package org.grails.orm.hibernate.cfg.domainbinding.binder

import java.beans.PropertyDescriptor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.RecordComponent
import java.lang.reflect.Type

import groovy.transform.CompileStatic
import org.hibernate.MappingException
import org.hibernate.boot.spi.MetadataBuildingContext
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment
import org.hibernate.mapping.BasicValue
import org.hibernate.mapping.Column
import org.hibernate.mapping.Component
import org.hibernate.mapping.PersistentClass
import org.hibernate.mapping.Property
import org.hibernate.mapping.Table
import org.hibernate.property.access.internal.PropertyAccessStrategyCompositeUserTypeImpl
import org.hibernate.property.access.spi.PropertyAccessStrategy
import org.hibernate.usertype.CompositeUserType
import org.jspecify.annotations.NonNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.BeanUtils
import org.springframework.util.ReflectionUtils

import org.grails.datastore.mapping.model.config.GormProperties
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.grails.orm.hibernate.cfg.ColumnConfig
import org.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.grails.orm.hibernate.cfg.PersistentEntityNamingStrategy
import org.grails.orm.hibernate.cfg.PropertyConfig
import org.grails.orm.hibernate.cfg.domainbinding.hibernate.HibernatePersistentProperty
import org.grails.orm.hibernate.cfg.domainbinding.util.BackticksRemover

/**
 * Binds a property mapped with a Hibernate {@link CompositeUserType} as a {@link Component} of that type, the way
 * Hibernate's own annotation binder binds {@code @CompositeType}: one property per attribute of the user type's
 * {@linkplain CompositeUserType#embeddable() embeddable class}, in alphabetical attribute order, each a basic value
 * typed by reflection on the embeddable class and accessed through Hibernate's composite-user-type property access.
 * The columns of the property mapping are assigned to the attributes in that order; an attribute without a configured
 * column is named {@code <property>_<attribute>}.
 *
 * @since 8.0.1
 */
@CompileStatic
class CompositeUserTypeBinder {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeUserTypeBinder)
    private static final String UNDERSCORE = '_'

    private final MetadataBuildingContext metadataBuildingContext
    private final PersistentEntityNamingStrategy namingStrategy
    private final ColumnConfigToColumnBinder columnConfigToColumnBinder
    private final ColumnBinder columnBinder
    private final BackticksRemover backticksRemover

    CompositeUserTypeBinder(
            MetadataBuildingContext metadataBuildingContext,
            PersistentEntityNamingStrategy namingStrategy,
            JdbcEnvironment jdbcEnvironment) {
        this(
                metadataBuildingContext,
                namingStrategy,
                new ColumnConfigToColumnBinder(),
                new ColumnBinder(namingStrategy, jdbcEnvironment.dialect),
                new BackticksRemover())
    }

    protected CompositeUserTypeBinder(
            MetadataBuildingContext metadataBuildingContext,
            PersistentEntityNamingStrategy namingStrategy,
            ColumnConfigToColumnBinder columnConfigToColumnBinder,
            ColumnBinder columnBinder,
            BackticksRemover backticksRemover) {
        this.metadataBuildingContext = metadataBuildingContext
        this.namingStrategy = namingStrategy
        this.columnConfigToColumnBinder = columnConfigToColumnBinder
        this.columnBinder = columnBinder
        this.backticksRemover = backticksRemover
    }

    Component bindCompositeUserType(
            @NonNull HibernatePersistentProperty property,
            HibernatePersistentProperty parentProperty,
            String path) {
        PersistentClass owner = property.persistentClass
        Class<? extends CompositeUserType<?>> userTypeClass = property.compositeUserType
        if (userTypeClass == null) {
            throw new MappingException("Property [${property.name}] of class [${owner.className}] is not mapped with a " +
                    CompositeUserType.simpleName)
        }
        CompositeUserType<?> userType = instantiate(userTypeClass)
        Class<?> embeddable = userType.embeddable()
        PropertyConfig propertyConfig = property.hibernateMappedForm
        Table table = property.table

        Component component = new Component(metadataBuildingContext, owner)
        component.roleName = GrailsHibernateUtil.qualify(
                property.hibernateOwner.javaClass.name, property.getNameForPropertyAndPath(path))
        component.typeName = userTypeClass.name
        component.componentClassName = embeddable.name

        Properties typeParams = propertyConfig.typeParams
        if (typeParams != null && !typeParams.isEmpty()) {
            LOG.warn('Property [{}] of class [{}] sets typeParams, which Hibernate does not pass to the composite ' +
                    'user type [{}]', property.name, owner.className, userTypeClass.name)
        }

        List<Attribute> attributes = attributesOf(embeddable)
        List<ColumnConfig> columns = propertyConfig.columns ?: []
        if (columns.size() > attributes.size()) {
            throw new MappingException(String.format(
                    'Property [%s] of class [%s] maps %d columns, but the embeddable class [%s] of its composite ' +
                            'user type [%s] has %d attributes: %s',
                    property.name, owner.className, columns.size(), embeddable.name,
                    userTypeClass.name, attributes.size(), attributes*.name))
        }

        List<String> attributeNames = new ArrayList<>(attributes.size())
        List<Type> attributeTypes = new ArrayList<>(attributes.size())
        PropertyAccessStrategy accessStrategy =
                new PropertyAccessStrategyCompositeUserTypeImpl(userType, attributeNames, attributeTypes)
        for (int i = 0; i < attributes.size(); i++) {
            Attribute attribute = attributes.get(i)
            ColumnConfig columnConfig = withName(
                    i < columns.size() ? columns.get(i) : null, defaultColumnName(property, path, attribute.name))

            BasicValue value = new BasicValue(metadataBuildingContext, table)
            value.setTypeUsingReflection(embeddable.name, attribute.name)
            Column column = new Column()
            columnConfigToColumnBinder.bindColumnConfigToColumn(column, columnConfig, propertyConfig)
            columnBinder.bindColumn(
                    property, parentProperty, column, columnConfig, path, table, userTypeClass.name)
            if (table != null) {
                table.addColumn(column)
            }
            value.addColumn(column)

            Property attributeProperty = new Property()
            attributeProperty.name = attribute.name
            attributeProperty.value = value
            attributeProperty.optional = column.nullable
            attributeProperty.propertyAccessStrategy = accessStrategy
            component.addProperty(attributeProperty)
            attributeNames.add(attribute.name)
            attributeTypes.add(attribute.type)
        }
        component.sortProperties()
        return component
    }

    private CompositeUserType<?> instantiate(Class<? extends CompositeUserType<?>> userTypeClass) {
        if (metadataBuildingContext.buildingOptions.isAllowExtensionsInCdi()) {
            return metadataBuildingContext.bootstrapContext
                    .managedBeanRegistry
                    .getBean(userTypeClass)
                    .beanInstance
        }
        return BeanUtils.instantiateClass(userTypeClass)
    }

    /**
     * The attributes of the embeddable class: the components of a record, or the fields and the readable bean properties of
     * any other class, less the static, transient and synthetic ones, the ones every Groovy object has, the ones the class
     * lists in {@code transients} and the ones backed by a transient field. A setter is not required, nor is an accessor at
     * all: Hibernate never writes the embeddable class, it reads the attributes through
     * {@link CompositeUserType#getPropertyValue(Object, int)}. The type of an attribute is the type of its field, or of its
     * getter when it has no field, which is how Hibernate resolves it. Alphabetical, the order of that index.
     */
    private static List<Attribute> attributesOf(Class<?> embeddable) {
        if (embeddable.isRecord()) {
            return embeddable.recordComponents
                    .collect { RecordComponent component -> new Attribute(component.name, component.genericType) }
                    .sort { Attribute a, Attribute b -> a.name <=> b.name }
        }
        Map<String, Type> attributes = new TreeMap<String, Type>()
        ReflectionUtils.doWithFields(embeddable,
                { Field field -> attributes.putIfAbsent(field.name, field.genericType) },
                { Field field ->
                    !field.synthetic && !Modifier.isStatic(field.modifiers) && !Modifier.isTransient(field.modifiers)
                })
        for (PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(embeddable)) {
            Method getter = descriptor.readMethod
            if (getter != null && !Modifier.isStatic(getter.modifiers)) {
                attributes.putIfAbsent(descriptor.name, getter.genericReturnType)
            }
        }
        Collection<?> transients =
                ClassPropertyFetcher.getStaticPropertyValue(embeddable, GormProperties.TRANSIENT, Collection) ?: []
        attributes.keySet().removeIf { String name ->
            ClassPropertyFetcher.EXCLUDED_PROPERTIES.contains(name) || transients.contains(name) || hasTransientField(embeddable, name)
        }
        return attributes.collect { String name, Type type -> new Attribute(name, type) }
    }

    /** Whether the property is backed by a {@code transient} field, which makes it transient however it is read. */
    private static boolean hasTransientField(Class<?> embeddable, String name) {
        Field field = ReflectionUtils.findField(embeddable, name)
        return field != null && Modifier.isTransient(field.modifiers)
    }

    private static ColumnConfig withName(ColumnConfig columnConfig, String defaultName) {
        if (columnConfig != null && columnConfig.name != null && !columnConfig.name.isEmpty()) {
            return columnConfig
        }
        ColumnConfig named = new ColumnConfig()
        if (columnConfig != null) {
            named.sqlType = columnConfig.sqlType
            named.enumType = columnConfig.enumType
            named.index = columnConfig.index
            named.unique = columnConfig.unique
            named.length = columnConfig.length
            named.precision = columnConfig.precision
            named.scale = columnConfig.scale
            named.defaultValue = columnConfig.defaultValue
            named.comment = columnConfig.comment
            named.read = columnConfig.read
            named.write = columnConfig.write
        }
        named.name = defaultName
        return named
    }

    private String defaultColumnName(HibernatePersistentProperty property, String path, String attribute) {
        String prefix = backticksRemover.apply(namingStrategy.resolveColumnName(property.name))
        if (GrailsHibernateUtil.isNotEmpty(path)) {
            prefix = backticksRemover.apply(namingStrategy.resolveColumnName(path)) + UNDERSCORE + prefix
        }
        return prefix + UNDERSCORE + backticksRemover.apply(namingStrategy.resolveColumnName(attribute))
    }

    /** An attribute of the embeddable class: its name and the generic type of its getter or record component. */
    @CompileStatic
    private static class Attribute {

        final String name
        final Type type

        Attribute(String name, Type type) {
            this.name = name
            this.type = type
        }
    }
}
