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
package org.grails.orm.hibernate.cfg.domainbinding.binder;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.property.access.internal.PropertyAccessStrategyCompositeUserTypeImpl;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.usertype.CompositeUserType;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;

import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;
import org.grails.orm.hibernate.cfg.ColumnConfig;
import org.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.grails.orm.hibernate.cfg.PersistentEntityNamingStrategy;
import org.grails.orm.hibernate.cfg.PropertyConfig;
import org.grails.orm.hibernate.cfg.domainbinding.hibernate.HibernatePersistentProperty;
import org.grails.orm.hibernate.cfg.domainbinding.util.BackticksRemover;
import org.grails.orm.hibernate.cfg.domainbinding.util.ColumnNameForPropertyAndPathFetcher;
import org.grails.orm.hibernate.cfg.domainbinding.util.CreateKeyForProps;
import org.grails.orm.hibernate.cfg.domainbinding.util.DefaultColumnNameFetcher;

/**
 * Binds a property mapped with a Hibernate {@link CompositeUserType} as a {@link Component}.
 * <p>
 * Hibernate resolves the type of a {@link BasicValue} as a {@link org.hibernate.type.BasicType} or a
 * {@link org.hibernate.usertype.UserType} only. A composite user type spans one column per attribute of its
 * {@linkplain CompositeUserType#embeddable() embeddable class}, so it is a component whose type name is the
 * user type, as Hibernate binds a {@link org.hibernate.annotations.CompositeType}: one property per attribute,
 * in alphabetical attribute order, read through {@link CompositeUserType#getPropertyValue(Object, int)} with the
 * index of the attribute in that order and assembled through
 * {@link CompositeUserType#instantiate(org.hibernate.metamodel.spi.ValueAccess)}. The columns of the property
 * mapping are assigned to the attributes in the same order; an attribute without a configured column is named
 * after the property and the attribute. The component class is the {@linkplain CompositeUserType#returnedClass()
 * value class} of the user type, not its embeddable class. A unique property has one unique key across all of its
 * columns, together with the columns of its uniqueness group, rather than a unique key per column.
 *
 * @since 8.0.1
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class CompositeUserTypeBinder {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeUserTypeBinder.class);
    private static final String UNDERSCORE = "_";

    private final MetadataBuildingContext metadataBuildingContext;
    private final PersistentEntityNamingStrategy namingStrategy;
    private final ColumnConfigToColumnBinder columnConfigToColumnBinder;
    private final ColumnBinder columnBinder;
    private final CreateKeyForProps createKeyForProps;
    private final BackticksRemover backticksRemover;

    public CompositeUserTypeBinder(
            MetadataBuildingContext metadataBuildingContext,
            PersistentEntityNamingStrategy namingStrategy,
            JdbcEnvironment jdbcEnvironment) {
        this(
                metadataBuildingContext,
                namingStrategy,
                new ColumnConfigToColumnBinder(),
                new ColumnBinder(namingStrategy, jdbcEnvironment.getDialect()),
                new CreateKeyForProps(new ColumnNameForPropertyAndPathFetcher(
                        namingStrategy, new DefaultColumnNameFetcher(namingStrategy), new BackticksRemover())),
                new BackticksRemover());
    }

    protected CompositeUserTypeBinder(
            MetadataBuildingContext metadataBuildingContext,
            PersistentEntityNamingStrategy namingStrategy,
            ColumnConfigToColumnBinder columnConfigToColumnBinder,
            ColumnBinder columnBinder,
            CreateKeyForProps createKeyForProps,
            BackticksRemover backticksRemover) {
        this.metadataBuildingContext = metadataBuildingContext;
        this.namingStrategy = namingStrategy;
        this.columnConfigToColumnBinder = columnConfigToColumnBinder;
        this.columnBinder = columnBinder;
        this.createKeyForProps = createKeyForProps;
        this.backticksRemover = backticksRemover;
    }

    /**
     * Binds the property as a component of its composite user type.
     *
     * @param property the property, mapped with a composite user type
     * @param parentProperty the embedded property that holds the property, or {@code null}
     * @param path the path of the property within its entity
     * @return the component
     * @throws MappingException when the property maps more columns than its embeddable class has attributes
     */
    public Component bindCompositeUserType(
            @NonNull HibernatePersistentProperty property,
            HibernatePersistentProperty parentProperty,
            String path) {
        PersistentClass owner = property.getPersistentClass();
        Class<? extends CompositeUserType<?>> userTypeClass = property.getCompositeUserType();
        if (userTypeClass == null) {
            throw new MappingException("Property [" + property.getName() + "] of class [" + owner.getClassName() +
                    "] is not mapped with a " + CompositeUserType.class.getSimpleName());
        }
        CompositeUserType<?> userType = instantiate(userTypeClass);
        Class<?> embeddable = userType.embeddable();
        PropertyConfig propertyConfig = property.getHibernateMappedForm();
        Table table = property.getTable();

        Component component = new Component(metadataBuildingContext, owner);
        component.setRoleName(GrailsHibernateUtil.qualify(
                property.getHibernateOwner().getJavaClass().getName(), property.getNameForPropertyAndPath(path)));
        component.setTypeName(userTypeClass.getName());
        // The component describes the value class, as Hibernate's own binding of a composite type does: the
        // embeddable class only supplies the attributes, so whole-value queries and parameters see the value class.
        component.setComponentClassName(userType.returnedClass().getName());

        Properties typeParams = propertyConfig.getTypeParams();
        if (typeParams != null && !typeParams.isEmpty()) {
            LOG.warn("Property [{}] of class [{}] sets typeParams, which Hibernate does not pass to the composite " +
                    "user type [{}]", property.getName(), owner.getClassName(), userTypeClass.getName());
        }

        List<Attribute> attributes = attributesOf(embeddable);
        List<ColumnConfig> columns = Optional.ofNullable(propertyConfig.getColumns()).orElseGet(List::of);
        if (columns.size() > attributes.size()) {
            throw new MappingException(String.format(
                    "Property [%s] of class [%s] maps %d columns, but the embeddable class [%s] of its composite " +
                            "user type [%s] has %d attributes: %s",
                    property.getName(), owner.getClassName(), columns.size(), embeddable.getName(),
                    userTypeClass.getName(), attributes.size(), attributes.stream().map(Attribute::name).toList()));
        }

        List<String> attributeNames = new ArrayList<>(attributes.size());
        List<Type> attributeTypes = new ArrayList<>(attributes.size());
        List<String> columnNames = new ArrayList<>(attributes.size());
        PropertyAccessStrategy accessStrategy =
                new PropertyAccessStrategyCompositeUserTypeImpl(userType, attributeNames, attributeTypes);
        for (int i = 0; i < attributes.size(); i++) {
            Attribute attribute = attributes.get(i);
            ColumnConfig columnConfig = withName(
                    i < columns.size() ? columns.get(i) : null, defaultColumnName(property, path, attribute.name()));

            BasicValue value = new BasicValue(metadataBuildingContext, table);
            value.setTypeUsingReflection(embeddable.getName(), attribute.name());

            Column column = new Column();
            columnConfigToColumnBinder.bindColumnConfigToColumn(column, columnConfig, propertyConfig);
            columnBinder.bindColumnWithoutUniqueness(
                    property, parentProperty, column, columnConfig, path, table, userTypeClass.getName());
            if (table != null) {
                table.addColumn(column);
            }
            value.addColumn(column);
            columnNames.add(column.getName());

            Property attributeProperty = new Property();
            attributeProperty.setName(attribute.name());
            attributeProperty.setValue(value);
            attributeProperty.setOptional(column.isNullable());
            attributeProperty.setPropertyAccessStrategy(accessStrategy);
            component.addProperty(attributeProperty);

            attributeNames.add(attribute.name());
            attributeTypes.add(attribute.type());
        }
        if (table != null) {
            createKeyForProps.createKeyForColumns(property, path, table, columnNames);
        }
        component.sortProperties();
        return component;
    }

    /**
     * Instantiates the user type the way Hibernate does when it resolves the component type: as a managed
     * bean when CDI extensions are allowed, through its default constructor otherwise.
     */
    private CompositeUserType<?> instantiate(Class<? extends CompositeUserType<?>> userTypeClass) {
        if (metadataBuildingContext.getBuildingOptions().isAllowExtensionsInCdi()) {
            return metadataBuildingContext.getBootstrapContext()
                    .getManagedBeanRegistry()
                    .getBean(userTypeClass)
                    .getBeanInstance();
        }
        return BeanUtils.instantiateClass(userTypeClass);
    }

    /**
     * The persistent attributes of the embeddable class, in alphabetical order: the components of a record, or
     * the fields and the readable bean properties of any other class, less the static, transient and synthetic
     * ones, the ones every Groovy object has, the ones the class lists in {@code transients} and the ones backed
     * by a transient field. A setter is not required, nor is an accessor at all: Hibernate never writes the
     * embeddable class, it reads the attributes through {@link CompositeUserType#getPropertyValue(Object, int)}.
     * The type of an attribute is the type of its field, or of its getter when it has no field, which is how
     * Hibernate resolves it.
     */
    private static List<Attribute> attributesOf(Class<?> embeddable) {
        if (embeddable.isRecord()) {
            return Arrays.stream(embeddable.getRecordComponents())
                    .map(component -> new Attribute(component.getName(), component.getGenericType()))
                    .sorted(Comparator.comparing(Attribute::name))
                    .toList();
        }
        Map<String, Type> attributes = new TreeMap<>();
        ReflectionUtils.doWithFields(embeddable,
                field -> attributes.putIfAbsent(field.getName(), field.getGenericType()),
                field -> !field.isSynthetic() && !Modifier.isStatic(field.getModifiers()) &&
                        !Modifier.isTransient(field.getModifiers()));
        for (PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(embeddable)) {
            Method getter = descriptor.getReadMethod();
            if (getter != null && !Modifier.isStatic(getter.getModifiers())) {
                attributes.putIfAbsent(descriptor.getName(), getter.getGenericReturnType());
            }
        }
        Collection<?> transients = Optional.ofNullable(
                ClassPropertyFetcher.getStaticPropertyValue(embeddable, GormProperties.TRANSIENT, Collection.class))
                .orElseGet(List::of);
        attributes.keySet().removeIf(name -> ClassPropertyFetcher.EXCLUDED_PROPERTIES.contains(name) ||
                transients.contains(name) || hasTransientField(embeddable, name));
        return attributes.entrySet().stream()
                .map(entry -> new Attribute(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** Whether the property is backed by a {@code transient} field, which makes it transient however it is read. */
    private static boolean hasTransientField(Class<?> embeddable, String name) {
        Field field = ReflectionUtils.findField(embeddable, name);
        return field != null && Modifier.isTransient(field.getModifiers());
    }

    /**
     * The configured column with a name: the configuration itself when it names its column, otherwise a copy of
     * it (or a new configuration) named after the property and the attribute.
     */
    private static ColumnConfig withName(ColumnConfig columnConfig, String defaultName) {
        if (columnConfig != null && columnConfig.getName() != null && !columnConfig.getName().isEmpty()) {
            return columnConfig;
        }
        ColumnConfig named = new ColumnConfig();
        if (columnConfig != null) {
            named.setSqlType(columnConfig.getSqlType());
            named.setEnumType(columnConfig.getEnumType());
            named.setIndex(columnConfig.getIndex());
            named.setUnique(columnConfig.getUnique());
            named.setLength(columnConfig.getLength());
            named.setPrecision(columnConfig.getPrecision());
            named.setScale(columnConfig.getScale());
            named.setDefaultValue(columnConfig.getDefaultValue());
            named.setComment(columnConfig.getComment());
            named.setRead(columnConfig.getRead());
            named.setWrite(columnConfig.getWrite());
        }
        named.setName(defaultName);
        return named;
    }

    /**
     * The default name of the column of an attribute: the column name of the property, prefixed with the path of
     * the property when it belongs to an embedded component, followed by the column name of the attribute.
     */
    private String defaultColumnName(HibernatePersistentProperty property, String path, String attribute) {
        String prefix = backticksRemover.apply(namingStrategy.resolveColumnName(property.getName()));
        if (GrailsHibernateUtil.isNotEmpty(path)) {
            prefix = backticksRemover.apply(namingStrategy.resolveColumnName(path)) + UNDERSCORE + prefix;
        }
        return prefix + UNDERSCORE + backticksRemover.apply(namingStrategy.resolveColumnName(attribute));
    }

    /** A persistent attribute of the embeddable class: its name and its Java type. */
    private record Attribute(String name, Type type) {
    }
}
