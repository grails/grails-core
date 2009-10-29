/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.orm.hibernate.cfg;


import grails.util.GrailsUtil;
import groovy.lang.Closure;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.exceptions.GrailsDomainException;
import org.codehaus.groovy.grails.orm.hibernate.persister.entity.GroovyAwareJoinedSubclassEntityPersister;
import org.codehaus.groovy.grails.orm.hibernate.persister.entity.GroovyAwareSingleTableEntityPersister;
import org.codehaus.groovy.grails.orm.hibernate.validation.UniqueConstraint;
import org.codehaus.groovy.grails.plugins.orm.hibernate.HibernatePluginSupport;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.MappingException;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.cfg.Mappings;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.SecondPass;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.mapping.*;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Table;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.StringHelper;

import java.sql.Types;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Handles the binding Grails domain classes and properties to the Hibernate runtime meta model.
 * Based on the HbmBinder code in Hibernate core and influenced by AnnotationsBinder.
 *
 * @author Graeme Rocher
 * @since 0.1
 *        <p/>
 *        Created: 06-Jul-2005
 */
public final class GrailsDomainBinder {

    private static final String FOREIGN_KEY_SUFFIX = "_id";
    private static final Log LOG = LogFactory.getLog(GrailsDomainBinder.class);
    private static final NamingStrategy namingStrategy = ImprovedNamingStrategy.INSTANCE;
    private static final String STRING_TYPE = "string";
    private static final String EMPTY_PATH = "";
    private static final char UNDERSCORE = '_';
    private static final String CASCADE_ALL = "all";
    private static final String CASCADE_SAVE_UPDATE = "save-update";
    private static final String CASCADE_MERGE = "merge";
    private static final String CASCADE_NONE = "none";
    private static final String BACKTICK = "`";

    private static final Map<Class, Mapping> MAPPING_CACHE = new HashMap<Class, Mapping>();
    private static final String ENUM_TYPE_CLASS = "org.hibernate.type.EnumType";
    private static final String ENUM_CLASS_PROP = "enumClass";
    private static final String ENUM_TYPE_PROP = "type";
    private static final String DEFAULT_ENUM_TYPE = "default";


    /**
     * A Collection type, for the moment only Set is supported
     *
     * @author Graeme
     */
    abstract static class CollectionType {

        private Class clazz;

        public abstract Collection create(GrailsDomainClassProperty property, PersistentClass owner,
                                          String path, Mappings mappings) throws MappingException;

        CollectionType(Class clazz) {
            this.clazz = clazz;
        }

        public String toString() {
            return clazz.getName();
        }

        private static CollectionType SET = new CollectionType(Set.class) {

            public Collection create(GrailsDomainClassProperty property, PersistentClass owner, String path, Mappings mappings) throws MappingException {
                org.hibernate.mapping.Set coll = new org.hibernate.mapping.Set(owner);
                coll.setCollectionTable(owner.getTable());
                bindCollection(property, coll, owner, mappings, path);
                return coll;
            }

        };

        private static CollectionType LIST = new CollectionType(List.class) {

            public Collection create(GrailsDomainClassProperty property, PersistentClass owner, String path, Mappings mappings) throws MappingException {
                org.hibernate.mapping.List coll = new org.hibernate.mapping.List(owner);
                coll.setCollectionTable(owner.getTable());
                bindCollection(property, coll, owner, mappings, path);
                return coll;
            }
        };

        private static CollectionType MAP = new CollectionType(Map.class) {

            public Collection create(GrailsDomainClassProperty property, PersistentClass owner, String path, Mappings mappings) throws MappingException {
                org.hibernate.mapping.Map map = new org.hibernate.mapping.Map(owner);
                bindCollection(property, map, owner, mappings, path);

                return map;
            }
        };

        private static final Map INSTANCES = new HashMap();

        static {
            INSTANCES.put(Set.class, SET);
            INSTANCES.put(SortedSet.class, SET);
            INSTANCES.put(List.class, LIST);
            INSTANCES.put(Map.class, MAP);
        }

        public static CollectionType collectionTypeForClass(Class clazz) {
            return (CollectionType) INSTANCES.get(clazz);
        }
    }


    /**
     * Second pass class for grails relationships. This is required as all
     * persistent classes need to be loaded in the first pass and then relationships
     * established in the second pass compile
     *
     * @author Graeme
     */
    static class GrailsCollectionSecondPass implements SecondPass {

        protected static final long serialVersionUID = -5540526942092611348L;
        protected GrailsDomainClassProperty property;
        protected Mappings mappings;
        protected Collection collection;

        public GrailsCollectionSecondPass(GrailsDomainClassProperty property, Mappings mappings, Collection coll) {
            this.property = property;
            this.mappings = mappings;
            this.collection = coll;
        }

        public void doSecondPass(Map persistentClasses, Map inheritedMetas) throws MappingException {
            bindCollectionSecondPass(this.property, mappings, persistentClasses, collection, inheritedMetas);
            createCollectionKeys();
        }

        private void createCollectionKeys() {
            collection.createAllKeys();

            if (LOG.isDebugEnabled()) {
                String msg = "Mapped collection key: " + columns(collection.getKey());
                if (collection.isIndexed())
                    msg += ", index: " + columns(((IndexedCollection) collection).getIndex());
                if (collection.isOneToMany()) {
                    msg += ", one-to-many: "
                            + ((OneToMany) collection.getElement()).getReferencedEntityName();
                } else {
                    msg += ", element: " + columns(collection.getElement());
                }
                LOG.debug(msg);
            }
        }

        private static String columns(Value val) {
            StringBuilder columns = new StringBuilder();
            Iterator iter = val.getColumnIterator();
            while (iter.hasNext()) {
                columns.append(((Selectable) iter.next()).getText());
                if (iter.hasNext()) columns.append(", ");
            }
            return columns.toString();
        }

        public void doSecondPass(Map persistentClasses) throws MappingException {
            bindCollectionSecondPass(this.property, mappings, persistentClasses, collection, Collections.EMPTY_MAP);
            createCollectionKeys();
        }

    }

    static class ListSecondPass extends GrailsCollectionSecondPass {
        public ListSecondPass(GrailsDomainClassProperty property, Mappings mappings, Collection coll) {
            super(property, mappings, coll);
        }


        public void doSecondPass(Map persistentClasses, Map inheritedMetas) throws MappingException {
            bindListSecondPass(this.property, mappings, persistentClasses, (org.hibernate.mapping.List) collection, inheritedMetas);
        }

        public void doSecondPass(Map persistentClasses) throws MappingException {
            bindListSecondPass(this.property, mappings, persistentClasses, (org.hibernate.mapping.List) collection, Collections.EMPTY_MAP);
        }
    }

    static class MapSecondPass extends GrailsCollectionSecondPass {

        public MapSecondPass(GrailsDomainClassProperty property, Mappings mappings, Collection coll) {
            super(property, mappings, coll);
        }


        public void doSecondPass(Map persistentClasses, Map inheritedMetas) throws MappingException {
            bindMapSecondPass(this.property, mappings, persistentClasses, (org.hibernate.mapping.Map) collection, inheritedMetas);
        }


        public void doSecondPass(Map persistentClasses) throws MappingException {
            bindMapSecondPass(this.property, mappings, persistentClasses, (org.hibernate.mapping.Map) collection, Collections.EMPTY_MAP);
        }
    }

    private static void bindMapSecondPass(GrailsDomainClassProperty property, Mappings mappings, Map persistentClasses, org.hibernate.mapping.Map map, Map inheritedMetas) {
        bindCollectionSecondPass(property, mappings, persistentClasses, map, inheritedMetas);

        SimpleValue value = new SimpleValue(map.getCollectionTable());

        bindSimpleValue(getIndexColumnType(property, STRING_TYPE), value, true, getIndexColumnName(property),mappings);
        PropertyConfig pc = getPropertyConfig(property);
        if(pc!=null && pc.getIndexColumn()!=null) {
            bindColumnConfigToColumn(getColumnForSimpleValue(value), getSingleColumnConfig(pc.getIndexColumn()));
        }

        if (!value.isTypeSpecified()) {
            throw new MappingException("map index element must specify a type: "
                    + map.getRole());
        }
        map.setIndex(value);

        if (!property.isOneToMany() && !property.isManyToMany()) {
            SimpleValue elt = new SimpleValue(map.getCollectionTable());
            map.setElement(elt);

            String typeName = getTypeName(getPropertyConfig(property));
            if(typeName == null) {
                if(property.isBasicCollectionType()) {
                    typeName = property.getReferencedPropertyType().getName();
                }
                else {
                    typeName = Hibernate.STRING.getName();
                }
            }
            bindSimpleValue(typeName, elt, false, getMapElementName(property),mappings);
            if(pc != null) {
                bindColumnConfigToColumn(getColumnForSimpleValue(elt), getSingleColumnConfig(pc));
            }

            elt.setTypeName(typeName);

            map.setInverse(false);
        }
        else {
            map.setInverse(false);
        }


    }

    private static ColumnConfig getSingleColumnConfig(PropertyConfig propertyConfig) {
        if(propertyConfig != null) {
            List columns = propertyConfig.getColumns();
            if(columns != null && !columns.isEmpty()) {
                return (ColumnConfig) columns.get(0);
            }
        }
        return null;
    }


    private static void bindListSecondPass(GrailsDomainClassProperty property, Mappings mappings, Map persistentClasses, org.hibernate.mapping.List list, Map inheritedMetas) {
        bindCollectionSecondPass(property, mappings, persistentClasses, list, inheritedMetas);

        String columnName = getIndexColumnName(property);

        SimpleValue iv = new SimpleValue(list.getCollectionTable());
        bindSimpleValue("integer", iv, true, columnName, mappings);
        iv.setTypeName("integer");
        list.setIndex(iv);
        list.setBaseIndex(0);
        list.setInverse(false);

        Value v = list.getElement();
        v.createForeignKey();


        if (property.isBidirectional()) {


            String entityName;
            Value element = list.getElement();
            if (element instanceof ManyToOne) {
                ManyToOne manyToOne = (ManyToOne) element;
                entityName = manyToOne.getReferencedEntityName();
            } else {
                entityName = ((OneToMany) element).getReferencedEntityName();
            }

            PersistentClass referenced = mappings.getClass(entityName);


            final boolean isManyToMany = property.isManyToMany();
            Class mappedClass = referenced.getMappedClass();
            Mapping m = getMapping(mappedClass);

            if(!isCompositeIdProperty(m,property.getOtherSide())) {
                Backref prop = new Backref();
                prop.setEntityName(property.getDomainClass().getFullName());
                prop.setName(UNDERSCORE + property.getDomainClass().getShortName() + UNDERSCORE + property.getName() + "Backref");
                prop.setSelectable(false);
                prop.setUpdateable(false);
                if(isManyToMany)
                    prop.setInsertable(false);
                prop.setCollectionRole(list.getRole());
                prop.setValue(list.getKey());



                DependantValue value = (DependantValue) prop.getValue();
                if(!property.isCircular())
                    value.setNullable(false);
                value.setUpdateable(true);
                prop.setOptional(false);

                referenced.addProperty(prop);
            }

            IndexBackref ib = new IndexBackref();
            ib.setName(UNDERSCORE + property.getName() + "IndexBackref");
            ib.setUpdateable(false);
            ib.setSelectable(false);
            if(isManyToMany)
                ib.setInsertable(false);
            ib.setCollectionRole(list.getRole());
            ib.setEntityName(list.getOwner().getEntityName());
            ib.setValue(list.getIndex());
            referenced.addProperty(ib);
        }

    }


    private static void bindCollectionSecondPass(GrailsDomainClassProperty property, Mappings mappings, Map persistentClasses, Collection collection, Map inheritedMetas) {

        PersistentClass associatedClass = null;

        if (LOG.isDebugEnabled())
            LOG.debug("Mapping collection: "
                    + collection.getRole()
                    + " -> "
                    + collection.getCollectionTable().getName());

        PropertyConfig propConfig = getPropertyConfig(property);

        if(propConfig != null && !StringUtils.isBlank(propConfig.getSort())) {
            GrailsDomainClass referenced = property.getReferencedDomainClass();
            if(referenced != null) {
                GrailsDomainClassProperty propertyToSortBy = referenced.getPropertyByName(propConfig.getSort());                
                collection.setOrderBy(getColumnNameForPropertyAndPath(propertyToSortBy,"",null));
            }
        }

        // Configure one-to-many
        if (collection.isOneToMany()) {

            GrailsDomainClass referenced = property.getReferencedDomainClass();
            Mapping m = getRootMapping(referenced);
            boolean tablePerSubclass = m != null && !m.getTablePerHierarchy();

            if (referenced != null && !referenced.isRoot() && !tablePerSubclass) {
                // NOTE: Work around for http://opensource.atlassian.com/projects/hibernate/browse/HHH-2855
                Mapping referencedMapping = getMapping(referenced);
                Mapping rootMapping = getRootMapping(referenced);
                String discriminatorColumnName = RootClass.DEFAULT_DISCRIMINATOR_COLUMN_NAME;
                String discriminator = referenced.getFullName();
                if(rootMapping != null) {
                    final ColumnConfig discriminatorColumn = rootMapping.getDiscriminatorColumn();
                    if(discriminatorColumn!=null) {
                        discriminatorColumnName = discriminatorColumn.getName();
                    }
                }
                if(referencedMapping != null && referencedMapping.getDiscriminator()!=null) {
                    discriminator = referencedMapping.getDiscriminator();
                }
                collection.setWhere(discriminatorColumnName + " = '" + discriminator + "'");
            }

            OneToMany oneToMany = (OneToMany) collection.getElement();
            String associatedClassName = oneToMany.getReferencedEntityName();

            associatedClass = (PersistentClass) persistentClasses.get(associatedClassName);
            // if there is no persistent class for the association throw
            // exception
            if (associatedClass == null) {
                throw new MappingException("Association references unmapped class: " + oneToMany.getReferencedEntityName());
            }

            oneToMany.setAssociatedClass(associatedClass);
            if (shouldBindCollectionWithForeignKey(property)) {
                collection.setCollectionTable(associatedClass.getTable());
            }

            bindCollectionForPropertyConfig(collection, propConfig);

        }

        if (isSorted(property)) {
            collection.setSorted(true);
        }

        // setup the primary key references
        DependantValue key = createPrimaryKeyValue(property, collection, persistentClasses);

        // link a bidirectional relationship
        if (property.isBidirectional()) {
            GrailsDomainClassProperty otherSide = property.getOtherSide();
            if (otherSide.isManyToOne() && shouldBindCollectionWithForeignKey(property)) {
                linkBidirectionalOneToMany(collection, associatedClass, key, otherSide);
            } else if (property.isManyToMany() || Map.class.isAssignableFrom(property.getType())) {
                bindDependentKeyValue(property, key, mappings);
            }
        } else {
            if(hasJoinKeyMapping(propConfig)) {
                bindSimpleValue("long", key,false, propConfig.getJoinTable().getKey().getName(), mappings);
            } else {
                bindDependentKeyValue(property, key, mappings);
            }
        }
        collection.setKey(key);

        // get cache config
        if (propConfig != null) {
            CacheConfig cacheConfig = propConfig.getCache();
            if (cacheConfig != null) {
                collection.setCacheConcurrencyStrategy(cacheConfig.getUsage());
            }
        }


        // if we have a many-to-many
        if (property.isManyToMany() || isBidirectionalOneToManyMap(property)) {
            GrailsDomainClassProperty otherSide = property.getOtherSide();

            if (property.isBidirectional()) {
                if (LOG.isDebugEnabled())
                    LOG.debug("[GrailsDomainBinder] Mapping other side " + otherSide.getDomainClass().getName() + "." + otherSide.getName() + " -> " + collection.getCollectionTable().getName() + " as ManyToOne");
                ManyToOne element = new ManyToOne(collection.getCollectionTable());
                bindManyToMany(otherSide, element, mappings);
                collection.setElement(element);
                bindCollectionForPropertyConfig(collection, propConfig);
                if(property.isCircular()) {
                    collection.setInverse(false);
                }

            } else {
                // TODO support unidirectional many-to-many
            }

        } else if (shouldCollectionBindWithJoinColumn(property)) {
            bindCollectionWithJoinTable(property, mappings, collection, propConfig);

        } else if (isUnidirectionalOneToMany(property)) {
            // for non-inverse one-to-many, with a not-null fk, add a backref!
            // there are problems with list and map mappings and join columns relating to duplicate key constraints
            // TODO change this when HHH-1268 is resolved
            bindUnidirectionalOneToMany(property, mappings, collection);
        }
    }

    private static Mapping getRootMapping(GrailsDomainClass referenced) {
        if (referenced == null) return null;
        Class current = referenced.getClazz();
        while (true) {
            Class superClass = current.getSuperclass();
            if (Object.class.equals(superClass)) break;
            current = superClass;
        }

        return getMapping(current);
    }

    private static boolean isBidirectionalOneToManyMap(GrailsDomainClassProperty property) {
        return Map.class.isAssignableFrom(property.getType()) && property.isBidirectional();
    }

    private static void bindCollectionWithJoinTable(GrailsDomainClassProperty property, Mappings mappings, Collection collection, PropertyConfig config) {
        SimpleValue element;
        if(property.isBasicCollectionType()) {
           element = new SimpleValue(collection.getCollectionTable());
        }
        else {
            // for a normal unidirectional one-to-many we use a join column
            element = new ManyToOne(collection.getCollectionTable());
            bindUnidirectionalOneToManyInverseValues(property, (ManyToOne) element);

        }
        collection.setInverse(false);

        String columnName;

        final boolean hasJoinColumnMapping = hasJoinColumnMapping(config);
        if(property.isBasicCollectionType()) {
            final Class referencedType = property.getReferencedPropertyType();
            String className = referencedType.getName();
            final boolean isEnum = GrailsClassUtils.isJdk5Enum(referencedType);
            if(hasJoinColumnMapping) {
                columnName = config.getJoinTable().getColumn().getName();
            }
            else
            {
                columnName = isEnum ? namingStrategy.propertyToColumnName(className) : namingStrategy.propertyToColumnName(property.getName()) + UNDERSCORE + namingStrategy.propertyToColumnName(className);
            }

            if(isEnum) {
                 bindEnumType(property, referencedType,element,columnName,"");
            }
            else {

                String typeName = getTypeName(config);
                if(typeName == null) {
                    Type type = TypeFactory.basic(className);
                    if(type != null) {
                        typeName = type.getName();
                    }
                }
                if(typeName == null) throw new MappingException("Type ["+typeName+"] is not a basic type or a domain class and cannot be mapped. Either specify a type within the [mapping] block or use a basic type (String, Integer etc.)");


                bindSimpleValue(typeName, element,true, columnName, mappings);
                if(hasJoinColumnMapping) {
                    bindColumnConfigToColumn(getColumnForSimpleValue(element), config.getJoinTable().getColumn());
                }
            }

        } else {
            final GrailsDomainClass domainClass = property.getReferencedDomainClass();

            Mapping m = getMapping(domainClass.getClazz());
            if(hasCompositeIdentifier(m))  {

                CompositeIdentity ci = (CompositeIdentity) m.getIdentity();
                bindCompositeIdentifierToManyToOne(property, (ManyToOne)element, ci, domainClass, EMPTY_PATH);
            }
            else {
                if(hasJoinColumnMapping) {
                    columnName = config.getJoinTable().getColumn().getName();
                }
                else {
                    columnName = namingStrategy.propertyToColumnName(domainClass.getPropertyName()) + FOREIGN_KEY_SUFFIX;
                }

                bindSimpleValue("long", element,true, columnName, mappings);
            }
        }

        collection.setElement(element);

        bindCollectionForPropertyConfig(collection, config);
    }

    private static Column getColumnForSimpleValue(SimpleValue element) {
        return (Column)element.getColumnIterator().next();
    }

    private static String getTypeName(PropertyConfig config) {
        if(config !=null && config.getType()!=null) {
            final Object typeObj = config.getType();
            if(typeObj instanceof Class) {
                return ((Class)typeObj).getName();
            }
            else {
                return typeObj.toString();
            }
        }
        return null;
    }

    private static void bindColumnConfigToColumn(Column column, ColumnConfig columnConfig) {
        if(columnConfig != null) {
            column.setLength(columnConfig.getLength());
            column.setPrecision(columnConfig.getPrecision());
            column.setSqlType(columnConfig.getSqlType());
            column.setUnique(columnConfig.getUnique());
            column.setScale(columnConfig.getScale());
        }
    }

    private static boolean hasJoinColumnMapping(PropertyConfig config) {
        return config != null && config.getJoinTable() != null && config.getJoinTable().getColumn()!=null;
    }

    private static boolean shouldCollectionBindWithJoinColumn(GrailsDomainClassProperty property) {
        PropertyConfig config = getPropertyConfig(property);
        JoinTable jt = config != null ? config.getJoinTable() : new JoinTable();

        return (isUnidirectionalOneToMany(property) || property.isBasicCollectionType()) && jt != null;
    }


    /**
     * @param property
     * @param manyToOne
     */
    private static void bindUnidirectionalOneToManyInverseValues(GrailsDomainClassProperty property, ManyToOne manyToOne) {
        PropertyConfig config = getPropertyConfig(property);
        if (config != null) {
            manyToOne.setLazy(config.getLazy());
            manyToOne.setIgnoreNotFound(config.getIgnoreNotFound());
        } else {
            manyToOne.setLazy(true);
        }

        // set referenced entity
        manyToOne.setReferencedEntityName(property.getReferencedPropertyType().getName());
    }


    private static void bindCollectionForPropertyConfig(Collection collection, PropertyConfig config) {
        if (config != null) {
            collection.setLazy(config.getLazy());            
        } else {
            collection.setLazy(true);
            collection.setExtraLazy(false);
        }
    }

    private static PropertyConfig getPropertyConfig(GrailsDomainClassProperty property) {
        Mapping m = getMapping(property.getDomainClass().getClazz());
        PropertyConfig config = m != null ? m.getPropertyConfig(property.getName()) : null;
        return config;
    }

    /**
     * Checks whether a property is a unidirectional non-circular one-to-many
     *
     * @param property The property to check
     * @return True if it is unidirectional and a one-to-many
     */
    private static boolean isUnidirectionalOneToMany(GrailsDomainClassProperty property) {
        return property.isOneToMany() && !property.isBidirectional();
    }

    /**
     * Binds the primary key value column
     *
     * @param property The property
     * @param key      The key
     * @param mappings The mappings
     */
    private static void bindDependentKeyValue(GrailsDomainClassProperty property, DependantValue key, Mappings mappings) {
        if (LOG.isDebugEnabled())
            LOG.debug("[GrailsDomainBinder] binding  [" + property.getName() + "] with dependant key");

        GrailsDomainClass refDomainClass = property.getDomainClass();
        final Mapping mapping = getMapping(refDomainClass.getClazz());
        if(shouldCollectionBindWithJoinColumn(property) && hasCompositeIdentifier(mapping)) {
            CompositeIdentity ci = (CompositeIdentity) mapping.getIdentity();
            bindCompositeIdentifierToManyToOne(property, key, ci, refDomainClass, EMPTY_PATH);
        }
        else {
            bindSimpleValue(property, null, key, EMPTY_PATH, mappings);
        }


    }

    /**
     * Creates the DependentValue object that forms a primary key reference for the collection
     *
     * @param property          The grails property
     * @param collection        The collection object
     * @param persistentClasses
     * @return The DependantValue (key)
     */
    private static DependantValue createPrimaryKeyValue(GrailsDomainClassProperty property, Collection collection, Map persistentClasses) {
        KeyValue keyValue;
        DependantValue key;
        String propertyRef = collection.getReferencedPropertyName();
        // this is to support mapping by a property
        if (propertyRef == null) {
            keyValue = collection.getOwner().getIdentifier();
        } else {
            keyValue = (KeyValue) collection.getOwner().getProperty(propertyRef).getValue();
        }

        if (LOG.isDebugEnabled())
            LOG.debug("[GrailsDomainBinder] creating dependant key value  to table [" + keyValue.getTable().getName() + "]");

        key = new DependantValue(collection.getCollectionTable(), keyValue);


        key.setTypeName(null);
        // make nullable and non-updateable
        key.setNullable(true);
        key.setUpdateable(false);
        return key;
    }

    /**
     * Binds a unidirectional one-to-many creating a psuedo back reference property in the process.
     *
     * @param property
     * @param mappings
     * @param collection
     */
    private static void bindUnidirectionalOneToMany(GrailsDomainClassProperty property, Mappings mappings, Collection collection) {
        Value v = collection.getElement();
        v.createForeignKey();
        String entityName;
        if (v instanceof ManyToOne) {
            ManyToOne manyToOne = (ManyToOne) v;

            entityName = manyToOne.getReferencedEntityName();
        } else {
            entityName = ((OneToMany) v).getReferencedEntityName();
        }
        collection.setInverse(false);
        PersistentClass referenced = mappings.getClass(entityName);
        Backref prop = new Backref();
        prop.setEntityName(property.getDomainClass().getFullName());
        prop.setName(UNDERSCORE + property.getDomainClass().getShortName() + UNDERSCORE + property.getName() + "Backref");
        prop.setUpdateable(false);
        prop.setInsertable(true);
        prop.setCollectionRole(collection.getRole());
        prop.setValue(collection.getKey());
        prop.setOptional(true);

        referenced.addProperty(prop);
    }


    private static Property getProperty(PersistentClass associatedClass,
                                        String propertyName)
            throws MappingException {
        try {
            return associatedClass.getProperty(propertyName);
        } catch (MappingException e) {
            //maybe it's squirreled away in a composite primary key
            if (associatedClass.getKey() instanceof Component) {
                return ((Component) associatedClass.getKey()).getProperty(propertyName);
            } else {
                throw e;
            }
        }
    }


    /**
     * Links a bidirectional one-to-many, configuring the inverse side and using a column copy to perform the link
     *
     * @param collection      The collection one-to-many
     * @param associatedClass The associated class
     * @param key             The key
     * @param otherSide       The other side of the relationship
     */
    private static void linkBidirectionalOneToMany(Collection collection, PersistentClass associatedClass, DependantValue key, GrailsDomainClassProperty otherSide) {
        collection.setInverse(true);

        //		Iterator mappedByColumns = associatedClass.getProperty( otherSide.getName() ).getValue().getColumnIterator();
        Iterator mappedByColumns = getProperty(associatedClass, otherSide.getName())
                .getValue().getColumnIterator();
        while (mappedByColumns.hasNext()) {
            Column column = (Column) mappedByColumns.next();
            linkValueUsingAColumnCopy(otherSide, column, key);
        }
    }

    /**
     * Establish whether a collection property is sorted
     *
     * @param property The property
     * @return True if sorted
     */
    private static boolean isSorted(GrailsDomainClassProperty property) {
        return SortedSet.class.isAssignableFrom(property.getType());
    }

    /**
     * Binds a many-to-many relationship. A many-to-many consists of
     * - a key (a DependentValue)
     * - an element
     * <p/>
     * The element is a ManyToOne from the association table to the target entity
     *
     * @param property The grails property
     * @param element  The ManyToOne element
     * @param mappings The mappings
     */
    private static void bindManyToMany(GrailsDomainClassProperty property, ManyToOne element, Mappings mappings) {
        bindManyToOne(property, element, EMPTY_PATH, mappings);
        element.setReferencedEntityName(property.getDomainClass().getFullName());
    }

    private static void linkValueUsingAColumnCopy(GrailsDomainClassProperty prop, Column column, DependantValue key) {
        Column mappingColumn = new Column();
        mappingColumn.setName(column.getName());
        mappingColumn.setLength(column.getLength());
        mappingColumn.setNullable(prop.isOptional());
        mappingColumn.setSqlType(column.getSqlType());

        mappingColumn.setValue(key);
        key.addColumn(mappingColumn);
        key.getTable().addColumn(mappingColumn);
    }

    /**
     * First pass to bind collection to Hibernate metamodel, sets up second pass
     *
     * @param property   The GrailsDomainClassProperty instance
     * @param collection The collection
     * @param owner      The owning persistent class
     * @param mappings   The Hibernate mappings instance
     * @param path
     */
    private static void bindCollection(GrailsDomainClassProperty property, Collection collection, PersistentClass owner, Mappings mappings, String path) {

        // set role
        String propertyName = getNameForPropertyAndPath(property, path);
        collection.setRole(StringHelper.qualify(property.getDomainClass().getFullName(), propertyName));

        PropertyConfig pc = getPropertyConfig(property);
        // configure eager fetching
        if (property.getFetchMode() == GrailsDomainClassProperty.FETCH_EAGER) {
            collection.setFetchMode(FetchMode.JOIN);
        }
        else if (pc != null && pc.getFetch() != null) {
            collection.setFetchMode(pc.getFetch());
        }
        else {
            collection.setFetchMode(FetchMode.DEFAULT);
        }


        // if it's a one-to-many mapping
        if (shouldBindCollectionWithForeignKey(property)) {
            OneToMany oneToMany = new OneToMany(collection.getOwner());
            collection.setElement(oneToMany);
            bindOneToMany(property, oneToMany, mappings);
        } else {
            bindCollectionTable(property, mappings, collection);

            if (!property.isOwningSide()) {
                collection.setInverse(true);
            }
        }

        if(pc != null && pc.getBatchSize() != null) {
            collection.setBatchSize(pc.getBatchSize().intValue());
        }

        // set up second pass
        if (collection instanceof org.hibernate.mapping.Set) {
            mappings.addSecondPass(new GrailsCollectionSecondPass(property, mappings, collection));
        }
        else if (collection instanceof org.hibernate.mapping.List) {
            mappings.addSecondPass(new ListSecondPass(property, mappings, collection));
        }
        else if (collection instanceof org.hibernate.mapping.Map) {
            mappings.addSecondPass(new MapSecondPass(property, mappings, collection));
        }

    }

    /*
     * We bind collections with foreign keys if specified in the mapping and only if it is a unidirectional one-to-many
     * that is
     */
    private static boolean shouldBindCollectionWithForeignKey(GrailsDomainClassProperty property) {
        return (property.isOneToMany() && property.isBidirectional() ||
                !shouldCollectionBindWithJoinColumn(property)) && !Map.class.isAssignableFrom(property.getType()) && !property.isManyToMany() && !property.isBasicCollectionType();
    }

    private static boolean isListOrMapCollection(GrailsDomainClassProperty property) {
        return Map.class.isAssignableFrom(property.getType()) || List.class.isAssignableFrom(property.getType());
    }

    private static String getNameForPropertyAndPath(GrailsDomainClassProperty property, String path) {
        String propertyName;
        if (StringHelper.isNotEmpty(path)) {
            propertyName = StringHelper.qualify(path, property.getName());
        } else {
            propertyName = property.getName();
        }
        return propertyName;
    }

    private static void bindCollectionTable(GrailsDomainClassProperty property, Mappings mappings, Collection collection) {
        String tableName = calculateTableForMany(property);
        Table t = mappings.addTable(
                mappings.getSchemaName(),
                mappings.getCatalogName(),
                tableName,
                null,
                false
        );
        collection.setCollectionTable(t);
    }

    /**
     * This method will calculate the mapping table for a many-to-many. One side of
     * the relationship has to "own" the relationship so that there is not a situation
     * where you have two mapping tables for left_right and right_left
     */
    private static String calculateTableForMany(GrailsDomainClassProperty property) {
        String propertyColumnName = namingStrategy.propertyToColumnName(property.getName());
        if (Map.class.isAssignableFrom(property.getType())) {
            String tablePrefix = getTableName(property.getDomainClass());
            return tablePrefix + "_" + propertyColumnName;

        }
        else {
            PropertyConfig config = getPropertyConfig(property);
            JoinTable jt = config != null ? config.getJoinTable() : null;
            boolean hasJoinTableMapping = jt != null && jt.getName() != null;

            String left = getTableName(property.getDomainClass());

            if (property.isBasicCollectionType()) {
                if (hasJoinTableMapping) {
                    return jt.getName();
                }
                else {
                    return left + UNDERSCORE + propertyColumnName;
                }
            }
            else {
                String right = getTableName(property.getReferencedDomainClass());

                if (property.isManyToMany()) {
                    if (hasJoinTableMapping) {
                        return jt.getName();
                    }
                    if (property.isOwningSide()) {
                        return left + UNDERSCORE + propertyColumnName;
                    }
                    else {
                        return right + UNDERSCORE + namingStrategy.propertyToColumnName(property.getOtherSide().getName());
                    }
                }
                else {
                    if (shouldCollectionBindWithJoinColumn(property)) {
                        if (hasJoinTableMapping) {
                            return jt.getName();
                        }
                        left = trimBackTigs(left);
                        right = trimBackTigs(right);
                        return left + UNDERSCORE + right;
                    }
                    if (property.isOwningSide()) {
                        return left + UNDERSCORE + right;
                    }
                    else {
                        return right + UNDERSCORE + left;
                    }
                }
            }
        }
    }

    private static String trimBackTigs(String tableName) {
        if (tableName.startsWith(BACKTICK)) return tableName.substring(1, tableName.length() - 1);
        return tableName;
    }

    /**
     * Evaluates the table name for the given property
     *
     * @param domainClass The domain class to evaluate
     * @return The table name
     */
    private static String getTableName(GrailsDomainClass domainClass) {
        Mapping m = getMapping(domainClass.getClazz());
        String tableName = null;
        if (m != null && m.getTableName() != null) {
            tableName = m.getTableName();
        }
        if (tableName == null) {
            tableName = namingStrategy.classToTableName(domainClass.getShortName());
        }
        return tableName;
    }

    /**
     * Binds a Grails domain class to the Hibernate runtime meta model
     *
     * @param domainClass The domain class to bind
     * @param mappings    The existing mappings
     * @throws MappingException Thrown if the domain class uses inheritance which is not supported
     */
    public static void bindClass(GrailsDomainClass domainClass, Mappings mappings)
            throws MappingException {
        //if(domainClass.getClazz().getSuperclass() == java.lang.Object.class) {
        if (domainClass.isRoot()) {
            bindRoot(domainClass, mappings);
        }
        //}
        //else {
        //	throw new MappingException("Grails domain classes do not support inheritance");
        //}
    }

    /**
     * Evaluates a Mapping object from the domain class if it has a mapping closure
     *
     * @param domainClass The domain class
     */
    public static Mapping evaluateMapping(GrailsDomainClass domainClass) {

        try {
            Object o = GrailsClassUtils.getStaticPropertyValue(domainClass.getClazz(), GrailsDomainClassProperty.MAPPING);
            if (o instanceof Closure) {
                HibernateMappingBuilder builder = new HibernateMappingBuilder(domainClass.getFullName());
                Mapping m = builder.evaluate((Closure) o);
                MAPPING_CACHE.put(domainClass.getClazz(), m);
                return m;
            }
        } catch (Exception e) {
            GrailsUtil.deepSanitize(e);
            throw new GrailsDomainException("Error evaluating ORM mappings block for domain ["+domainClass.getFullName()+"]:  " + e.getMessage(), e);

        }
        return null;
    }

    /**
     * Evaluates a Mapping object from the domain class if it has a namedQueries closure
     *
     * @param domainClass The domain class
     */
    public static void evaluateNamedQueries(GrailsDomainClass domainClass) {

        try {
            Object o = GrailsClassUtils.getStaticPropertyValue(domainClass.getClazz(), GrailsDomainClassProperty.NAMED_QUERIES);
            if (o instanceof Closure) {
                HibernateNamedQueriesBuilder builder = new HibernateNamedQueriesBuilder(domainClass);
                builder.evaluate((Closure) o);
            }
        } catch (Exception e) {
            GrailsUtil.deepSanitize(e);
            throw new GrailsDomainException("Error evaluating named queries block for domain ["+domainClass.getFullName()+"]:  " + e.getMessage(), e);

        }
    }

    /**
     * Obtains a mapping object for the given domain class nam
     *
     * @param theClass The domain class in question
     * @return A Mapping object or null
     */
    public static Mapping getMapping(Class theClass) {
        return theClass != null ? MAPPING_CACHE.get(theClass) : null;
    }

    /**
     * Obtains a mapping object for the given domain class nam
     *
     * @param domainClass The domain class in question
     * @return A Mapping object or null
     */
    public static Mapping getMapping(GrailsDomainClass domainClass) {
        return domainClass != null ? MAPPING_CACHE.get(domainClass.getClazz()) : null;
    }

    /**
     * Binds the specified persistant class to the runtime model based on the
     * properties defined in the domain class
     *
     * @param domainClass     The Grails domain class
     * @param persistentClass The persistant class
     * @param mappings        Existing mappings
     */
    private static void bindClass(GrailsDomainClass domainClass, PersistentClass persistentClass, Mappings mappings) {

        // set lazy loading for now
        persistentClass.setLazy(true);
        persistentClass.setEntityName(domainClass.getFullName());
        persistentClass.setProxyInterfaceName(domainClass.getFullName());
        persistentClass.setClassName(domainClass.getFullName());

        // set dynamic insert to false
        persistentClass.setDynamicInsert(false);
        // set dynamic update to false
        persistentClass.setDynamicUpdate(false);
        // set select before update to false
        persistentClass.setSelectBeforeUpdate(false);

        // add import to mappings
        if (mappings.isAutoImport() && persistentClass.getEntityName().indexOf('.') > 0) {
            mappings.addImport(persistentClass.getEntityName(), StringHelper.unqualify(persistentClass
                    .getEntityName()));
        }
    }


    /**
     * Binds a root class (one with no super classes) to the runtime meta model
     * based on the supplied Grails domain class
     *
     * @param domainClass The Grails domain class
     * @param mappings    The Hibernate Mappings object
     */
    public static void bindRoot(GrailsDomainClass domainClass, Mappings mappings) {
        if (mappings.getClass(domainClass.getFullName()) == null) {
            RootClass root = new RootClass();
            if(!domainClass.hasSubClasses()) {
                root.setPolymorphic(false);
            }
            bindClass(domainClass, root, mappings);

            Mapping m = getMapping(domainClass);


            bindRootPersistentClassCommonValues(domainClass, root, mappings);

            if (!domainClass.getSubClasses().isEmpty()) {
                boolean tablePerSubclass = m != null && !m.getTablePerHierarchy();
                if (!tablePerSubclass) {
                    // if the root class has children create a discriminator property
                    bindDiscriminatorProperty(root.getTable(), root, mappings);
                }
                // bind the sub classes
                bindSubClasses(domainClass, root, mappings);
            }

            if(root.getEntityPersisterClass() == null) {
                root.setEntityPersisterClass(GroovyAwareSingleTableEntityPersister.class);
            }
            mappings.addClass(root);
        } else {
            LOG.info("[GrailsDomainBinder] Class [" + domainClass.getFullName() + "] is already mapped, skipping.. ");
        }
    }

    /**
     * Binds the sub classes of a root class using table-per-heirarchy inheritance mapping
     *
     * @param domainClass The root domain class to bind
     * @param parent      The parent class instance
     * @param mappings    The mappings instance
     */
    private static void bindSubClasses(GrailsDomainClass domainClass, PersistentClass parent, Mappings mappings) {
        Set subClasses = domainClass.getSubClasses();

        for (Iterator i = subClasses.iterator(); i.hasNext();) {
            GrailsDomainClass sub = (GrailsDomainClass) i.next();
            Set subSubs = sub.getSubClasses();
            if (sub.getClazz().getSuperclass().equals(domainClass.getClazz())) {
                bindSubClass(sub, parent, mappings);
            }
        }
    }

    /**
     * Binds a sub class
     *
     * @param sub      The sub domain class instance
     * @param parent   The parent persistent class instance
     * @param mappings The mappings instance
     */
    private static void bindSubClass(GrailsDomainClass sub, PersistentClass parent, Mappings mappings) {
        evaluateMapping(sub);
        Mapping m = getMapping(parent.getMappedClass());
        Subclass subClass;
        boolean tablePerSubclass = m != null && !m.getTablePerHierarchy();
        if (tablePerSubclass) {
            subClass = new JoinedSubclass(parent);
        } else {
            subClass = new SingleTableSubclass(parent);
            // set the descriminator value as the name of the class. This is the
            // value used by Hibernate to decide what the type of the class is
            // to perform polymorphic queries
            Mapping subMapping = getMapping(sub);
            subClass.setDiscriminatorValue(subMapping != null && subMapping.getDiscriminator() != null ? subMapping.getDiscriminator() : sub.getFullName());

        }
        subClass.setEntityName(sub.getFullName());
        parent.addSubclass(subClass);
        mappings.addClass(subClass);

        if (tablePerSubclass)
            bindJoinedSubClass(sub, (JoinedSubclass) subClass, mappings, m);
        else
            bindSubClass(sub, subClass, mappings);


        if (!sub.getSubClasses().isEmpty()) {
            // bind the sub classes
            bindSubClasses(sub, subClass, mappings);
        }

    }

    /**
     * Binds a joined sub-class mapping using table-per-subclass
     *
     * @param sub            The Grails sub class
     * @param joinedSubclass The Hibernate Subclass object
     * @param mappings       The mappings Object
     * @param gormMapping    The GORM mapping object
     */
    private static void bindJoinedSubClass(GrailsDomainClass sub, JoinedSubclass joinedSubclass, Mappings mappings, Mapping gormMapping) {
        bindClass(sub, joinedSubclass, mappings);

        if (joinedSubclass.getEntityPersisterClass() == null) {
            joinedSubclass.getRootClass()
                    .setEntityPersisterClass(GroovyAwareJoinedSubclassEntityPersister.class);
        }

        Table mytable = mappings.addTable(
                mappings.getSchemaName(),
                mappings.getCatalogName(),
                getJoinedSubClassTableName(sub, joinedSubclass, null, mappings),
                null,
                false
        );


        joinedSubclass.setTable(mytable);
        LOG.info(
                "Mapping joined-subclass: " + joinedSubclass.getEntityName() +
                        " -> " + joinedSubclass.getTable().getName()
        );


        SimpleValue key = new DependantValue(mytable, joinedSubclass.getIdentifier());
        joinedSubclass.setKey(key);
        GrailsDomainClassProperty identifier = sub.getIdentifier();
        String columnName = getColumnNameForPropertyAndPath(identifier, EMPTY_PATH, null);
        bindSimpleValue(identifier.getType().getName(), key, false, columnName, mappings);

        joinedSubclass.createPrimaryKey();

        // properties
        createClassProperties(sub, joinedSubclass, mappings);
    }

    private static String getJoinedSubClassTableName(
            GrailsDomainClass sub, PersistentClass model, Table denormalizedSuperTable,
            Mappings mappings
    ) {

        String logicalTableName = StringHelper.unqualify(model.getEntityName());
        String physicalTableName = getTableName(sub);


        mappings.addTableBinding(mappings.getSchemaName(), mappings.getCatalogName(), logicalTableName, physicalTableName, denormalizedSuperTable);
        return physicalTableName;
    }

    /**
     * Binds a sub-class using table-per-heirarchy in heritance mapping
     *
     * @param sub      The Grails domain class instance representing the sub-class
     * @param subClass The Hibernate SubClass instance
     * @param mappings The mappings instance
     */
    private static void bindSubClass(GrailsDomainClass sub, Subclass subClass, Mappings mappings) {
        bindClass(sub, subClass, mappings);

        if (subClass.getEntityPersisterClass() == null) {
            subClass.getRootClass()
                    .setEntityPersisterClass(GroovyAwareSingleTableEntityPersister.class);
//                    .setEntityPersisterClass(SingleTableEntityPersister.class);
        }

        if (LOG.isDebugEnabled())
            LOG.debug(
                    "Mapping subclass: " + subClass.getEntityName() +
                            " -> " + subClass.getTable().getName()
            );

        // properties
        createClassProperties(sub, subClass, mappings);
    }

    /**
     * Creates and binds the discriminator property used in table-per-heirarchy inheritance to
     * discriminate between sub class instances
     *
     * @param table    The table to bind onto
     * @param entity   The root class entity
     * @param mappings The mappings instance
     */
    private static void bindDiscriminatorProperty(Table table, RootClass entity, Mappings mappings) {
        Mapping m = getMapping(entity.getMappedClass());
        SimpleValue d = new SimpleValue(table);
        entity.setDiscriminator(d);
        entity.setDiscriminatorValue(m!=null && m.getDiscriminator() != null ? m.getDiscriminator() : entity.getClassName());

        bindSimpleValue(
                STRING_TYPE,
                d,
                false,
                RootClass.DEFAULT_DISCRIMINATOR_COLUMN_NAME,
                mappings
        );

        ColumnConfig cc = m!= null ? m.getDiscriminatorColumn() : null;
        if(cc != null) {
            Column c = (Column) d.getColumnIterator().next();
            if(cc.getName() != null) {
                c.setName(cc.getName());
            }
            bindColumnConfigToColumn(c, cc);
        }

        entity.setPolymorphic(true);
    }


    /**
     * Binds a persistent classes to the table representation and binds the class properties
     *
     * @param domainClass
     * @param root
     * @param mappings
     */
    private static void bindRootPersistentClassCommonValues(GrailsDomainClass domainClass, RootClass root, Mappings mappings) {

        // get the schema and catalog names from the configuration
        Mapping m = getMapping(domainClass.getClazz());
        String schema = mappings.getSchemaName();
        String catalog = mappings.getCatalogName();


        if (m != null) {
            CacheConfig cc = m.getCache();
            if (cc != null && cc.getEnabled()) {
                root.setCacheConcurrencyStrategy(cc.getUsage());
                root.setLazyPropertiesCacheable(!"non-lazy".equals(cc.getInclude()));
            }

            Integer bs = m.getBatchSize();
            if(bs != null) {
                root.setBatchSize(bs.intValue());
            }

            if(m.getDynamicUpdate()) {
                root.setDynamicUpdate(true);
            }
            if(m.getDynamicInsert()) {
                root.setDynamicInsert(true);
            }
        }

        final boolean hasTableDefinition = m != null && m.getTable() != null;
        if(hasTableDefinition && m.getTable().getSchema() != null)  {
             schema =  m.getTable().getSchema();
        }
        if(hasTableDefinition && m.getTable().getCatalog() != null) {
            catalog = m.getTable().getCatalog();
        }

        // create the table
        Table table = mappings.addTable(
                schema,
                catalog,
                getTableName(domainClass),
                null,
                false
        );
        root.setTable(table);

        if (LOG.isDebugEnabled())
            LOG.debug("[GrailsDomainBinder] Mapping Grails domain class: " + domainClass.getFullName() + " -> " + root.getTable().getName());

        bindIdentity(domainClass, root, mappings, m);

        if (m != null) {
            if (m.getVersioned()) {
                bindVersion(domainClass.getVersion(), root, mappings);
            }
        } else
            bindVersion(domainClass.getVersion(), root, mappings);

        root.createPrimaryKey();

        createClassProperties(domainClass, root, mappings);
    }

    private static void bindIdentity(GrailsDomainClass domainClass, RootClass root, Mappings mappings, Mapping gormMapping) {
        GrailsDomainClassProperty identifierProp = domainClass.getIdentifier();
        if (gormMapping != null) {
            Object id = gormMapping.getIdentity();
            if (id instanceof CompositeIdentity) {
                bindCompositeId(domainClass, root, (CompositeIdentity) id, gormMapping, mappings);
            } else {
                final Identity identity = (Identity) id;
                String propertyName = identity.getName();
                if(propertyName!=null) {
                    GrailsDomainClassProperty namedIdentityProp = domainClass.getPropertyByName(propertyName);
                    if(namedIdentityProp == null) {
                        throw new MappingException("Mapping specifies an identifier property name that doesn't exist ["+propertyName+"]");
                    }
                    if(!namedIdentityProp.equals(identifierProp)) {
                        identifierProp = namedIdentityProp;
                    }
                }
                bindSimpleId(identifierProp, root, mappings, identity);
            }

        } else {
            bindSimpleId(identifierProp, root, mappings, null);
        }
    }

    private static void bindCompositeId(GrailsDomainClass domainClass, RootClass root, CompositeIdentity compositeIdentity, Mapping gormMapping, Mappings mappings) {
        Component id = new Component(root);
        id.setNullValue("undefined");
        root.setIdentifier(id);
        root.setEmbeddedIdentifier(true);
        id.setComponentClassName(domainClass.getFullName());
        id.setKey(true);
        id.setEmbedded(true);

        String path = StringHelper.qualify(
                root.getEntityName(),
                "id");

        id.setRoleName(path);

        String[] props = compositeIdentity.getPropertyNames();
        for (String propName : props) {
            GrailsDomainClassProperty property = domainClass.getPropertyByName(propName);
            if (property == null)
                throw new MappingException("Property [" + propName + "] referenced in composite-id mapping of class [" + domainClass.getFullName() + "] is not a valid property!");


            bindComponentProperty(id, null, property, root, "", root.getTable(), mappings);
        }
    }

    /**
     * Creates and binds the properties for the specified Grails domain class and PersistantClass
     * and binds them to the Hibernate runtime meta model
     *
     * @param domainClass     The Grails domain class
     * @param persistentClass The Hibernate PersistentClass instance
     * @param mappings        The Hibernate Mappings instance
     */
    protected static void createClassProperties(GrailsDomainClass domainClass, PersistentClass persistentClass, Mappings mappings) {

        GrailsDomainClassProperty[] persistentProperties = domainClass.getPersistentProperties();
        Table table = persistentClass.getTable();

        Mapping gormMapping = getMapping(domainClass.getClazz());

        for (GrailsDomainClassProperty currentGrailsProp : persistentProperties) {

            // if its inherited skip
            boolean isBidirectionalManyToOne = isBidirectionalManyToOne(currentGrailsProp);
            if (currentGrailsProp.isInherited())
                continue;
            else if (currentGrailsProp.isInherited() && isBidirectionalManyToOne && currentGrailsProp.isCircular())
                continue;
            if (isCompositeIdProperty(gormMapping, currentGrailsProp)) continue;
            if (isIdentityProperty(gormMapping, currentGrailsProp)) continue;

            if (LOG.isDebugEnabled())
                LOG.debug("[GrailsDomainBinder] Binding persistent property [" + currentGrailsProp.getName() + "]");

            Value value = null;

            // see if its a collection type
            CollectionType collectionType = CollectionType.collectionTypeForClass(currentGrailsProp.getType());

            Class userType = getUserType(currentGrailsProp);

            if (collectionType != null) {
                // create collection
                Collection collection = collectionType.create(
                        currentGrailsProp,
                        persistentClass,
                        EMPTY_PATH, mappings
                );
                mappings.addCollection(collection);
                value = collection;
            }
            else if (currentGrailsProp.isEnum()) {
                value = new SimpleValue(table);
                bindEnumType(currentGrailsProp, (SimpleValue) value, EMPTY_PATH, mappings);
            }
            // work out what type of relationship it is and bind value
            else if (currentGrailsProp.isManyToOne()) {
                if (LOG.isDebugEnabled())
                    LOG.debug("[GrailsDomainBinder] Binding property [" + currentGrailsProp.getName() + "] as ManyToOne");

                value = new ManyToOne(table);
                bindManyToOne(currentGrailsProp, (ManyToOne) value, EMPTY_PATH, mappings);
            }
            else if (currentGrailsProp.isOneToOne() && userType == null) {
                if (LOG.isDebugEnabled())
                    LOG.debug("[GrailsDomainBinder] Binding property [" + currentGrailsProp.getName() + "] as OneToOne");

                if(currentGrailsProp.isHasOne()&&!currentGrailsProp.isBidirectional()) {
                   throw new MappingException("hasOne property ["+currentGrailsProp.getDomainClass().getName()+"."+currentGrailsProp.getName()+"] is not bidirectional. Specify the other side of the relationship!"); 
                }
                else if (canBindOneToOneWithSingleColumnAndForeignKey(currentGrailsProp)) {
                    value = new OneToOne(table, persistentClass);
                    bindOneToOne(currentGrailsProp, (OneToOne) value, EMPTY_PATH, mappings);
                }
                else {
                    if(currentGrailsProp.isHasOne() && currentGrailsProp.isBidirectional()) {
                        value = new OneToOne(table, persistentClass);
                        bindOneToOne(currentGrailsProp, (OneToOne) value, EMPTY_PATH, mappings);
                    }
                    else {
                        value = new ManyToOne(table);
                        bindManyToOne(currentGrailsProp, (ManyToOne) value, EMPTY_PATH, mappings);
                    }
                }
            }
            else if (currentGrailsProp.isEmbedded()) {
                value = new Component(persistentClass);

                bindComponent((Component) value, currentGrailsProp, true, mappings);
            }
            else {
                if (LOG.isDebugEnabled())
                    LOG.debug("[GrailsDomainBinder] Binding property [" + currentGrailsProp.getName() + "] as SimpleValue");

                value = new SimpleValue(table);
                bindSimpleValue(currentGrailsProp, null, (SimpleValue) value, EMPTY_PATH, mappings);
            }

            if (value != null) {
                Property property = createProperty(value, persistentClass, currentGrailsProp, mappings);
                persistentClass.addProperty(property);
            }
        }

        bindNaturalIdentifier(table, gormMapping, persistentClass);
    }


    private static void bindNaturalIdentifier(Table table, Mapping mapping, PersistentClass persistentClass) {
        Object o = mapping != null ? mapping.getIdentity() : null;
        if(o instanceof Identity) {
            Identity identity = (Identity) o;
            final NaturalId naturalId = identity.getNatural();
            if(naturalId !=null && !naturalId.getPropertyNames().isEmpty()) {
                UniqueKey uk = new UniqueKey();
                uk.setName("_UniqueKey");
                uk.setTable(table);

                boolean mutable = naturalId.isMutable();

                for (String propertyName : naturalId.getPropertyNames()) {
                    Property property = persistentClass.getProperty(propertyName);

                    property.setNaturalIdentifier(true);
                    if(!mutable) property.setUpdateable(false);

                    uk.addColumns(property.getColumnIterator());
                }

                table.addUniqueKey(uk);
                
            }
        }
    }

    private static boolean canBindOneToOneWithSingleColumnAndForeignKey(GrailsDomainClassProperty currentGrailsProp) {
        if(currentGrailsProp.isBidirectional()) {
            final GrailsDomainClassProperty otherSide = currentGrailsProp.getOtherSide();
            if(otherSide.isHasOne()) return false;
            if(!currentGrailsProp.isOwningSide() && (otherSide != null && otherSide.isOwningSide())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIdentityProperty(Mapping gormMapping, GrailsDomainClassProperty currentGrailsProp) {
        if(gormMapping != null) {
            Object identityMapping = gormMapping.getIdentity();
            if(identityMapping instanceof Identity) {
                Identity identity = (Identity)identityMapping;
                if(identity.getName()!=null && identity.getName()!=null && identity.getName().equals(currentGrailsProp.getName()))
                    return true;
            }
        }
        return false;
    }

    private static void bindEnumType(GrailsDomainClassProperty property, SimpleValue simpleValue, String path, Mappings mappings) {
        final Class propertyType = property.getType();
        bindEnumType(property, propertyType, simpleValue, getColumnNameForPropertyAndPath(property, path, null),path);

    }

    private static void bindEnumType(GrailsDomainClassProperty property, Class propertyType, SimpleValue simpleValue, String columnName, String path) {
        Properties enumProperties = new Properties();
        enumProperties.put(ENUM_CLASS_PROP, propertyType.getName());

        PropertyConfig pc = getPropertyConfig(property);
        String enumType = pc != null ? pc.getEnumType() : DEFAULT_ENUM_TYPE;
        if(enumType.equals(DEFAULT_ENUM_TYPE) && IdentityEnumType.supports(propertyType)) {
            simpleValue.setTypeName(IdentityEnumType.class.getName());
        } else {
            simpleValue.setTypeName(ENUM_TYPE_CLASS);
            if(enumType.equals(DEFAULT_ENUM_TYPE) || "string".equalsIgnoreCase(enumType)) {
                enumProperties.put(ENUM_TYPE_PROP, String.valueOf(Types.VARCHAR));
            }
            else if(!"ordinal".equalsIgnoreCase(enumType)) {
                LOG.warn("Invalid enumType specified when mapping property ["+property.getName()+"] of class ["+property.getDomainClass().getClazz()+"]. Using defaults instead.");
            }
        }

        simpleValue.setTypeParameters(enumProperties);
        Table t = simpleValue.getTable();
        Column column = new Column();
        column.setNullable(property.isOptional());
        column.setValue(simpleValue);
        column.setName(columnName);
        if (t != null) t.addColumn(column);

        simpleValue.addColumn(column);
    }

    private static Class getUserType(GrailsDomainClassProperty currentGrailsProp) {
        Class userType = null;
        PropertyConfig config = getPropertyConfig(currentGrailsProp);
        Object typeObj = config != null ? config.getType() : null;
        if (typeObj instanceof Class) {
            userType = (Class) typeObj;
        } else if (typeObj != null) {
            String typeName = typeObj.toString();
            try {
                userType = Class.forName(typeName, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
            	if(LOG.isWarnEnabled()) {
            		LOG.warn("UserType not found ", e);
            	}
            }
        }
        return userType;
    }

    private static boolean isCompositeIdProperty(Mapping gormMapping, GrailsDomainClassProperty currentGrailsProp) {
        if (gormMapping != null && gormMapping.getIdentity() != null) {
            Object id = gormMapping.getIdentity();
            if (id instanceof CompositeIdentity) {
                CompositeIdentity cid = (CompositeIdentity) id;
                if (ArrayUtils.contains(cid.getPropertyNames(), currentGrailsProp.getName()))
                    return true;
            }
        }
        return false;
    }

    private static boolean isBidirectionalManyToOne(GrailsDomainClassProperty currentGrailsProp) {
        return (currentGrailsProp.isBidirectional() && currentGrailsProp.isManyToOne());
    }

    /**
     * Binds a Hibernate component type using the given GrailsDomainClassProperty instance
     *
     * @param component  The component to bind
     * @param property   The property
     * @param isNullable Whether it is nullable or not
     * @param mappings   The Hibernate Mappings object
     */
    private static void bindComponent(Component component, GrailsDomainClassProperty property, boolean isNullable, Mappings mappings) {
        component.setEmbedded(true);
        Class type = property.getType();
        String role = StringHelper.qualify(type.getName(), property.getName());
        component.setRoleName(role);
        component.setComponentClassName(type.getName());


        GrailsDomainClass domainClass = property.getReferencedDomainClass() != null ? property.getReferencedDomainClass() : property.getComponent();
        
        evaluateMapping(domainClass);
        GrailsDomainClassProperty[] properties = domainClass.getPersistentProperties();
        Table table = component.getOwner().getTable();
        PersistentClass persistentClass = component.getOwner();
        String path = property.getName();
        Class propertyType = property.getDomainClass().getClazz();

        for (GrailsDomainClassProperty currentGrailsProp : properties) {
            if (currentGrailsProp.isIdentity()) continue;
            if (currentGrailsProp.getName().equals(GrailsDomainClassProperty.VERSION)) continue;

            if (currentGrailsProp.getType().equals(propertyType)) {
                component.setParentProperty(currentGrailsProp.getName());
                continue;
            }

            bindComponentProperty(component, property, currentGrailsProp, persistentClass, path, table, mappings);

        }

    }


    private static void bindComponentProperty(Component component, GrailsDomainClassProperty componentProperty, GrailsDomainClassProperty currentGrailsProp, PersistentClass persistentClass, String path, Table table, Mappings mappings) {
        Value value = null;
        // see if its a collection type
        CollectionType collectionType = CollectionType.collectionTypeForClass(currentGrailsProp.getType());
        if (collectionType != null) {
            // create collection

            Collection collection = collectionType.create(
                    currentGrailsProp,
                    persistentClass,
                    path,
                    mappings
            );
            mappings.addCollection(collection);
            value = collection;
        }
        // work out what type of relationship it is and bind value
        else if (currentGrailsProp.isManyToOne()) {
            if (LOG.isDebugEnabled())
                LOG.debug("[GrailsDomainBinder] Binding property [" + currentGrailsProp.getName() + "] as ManyToOne");

            value = new ManyToOne(table);
            bindManyToOne(currentGrailsProp, (ManyToOne) value, path, mappings);
        } else if (currentGrailsProp.isOneToOne()) {
            if (LOG.isDebugEnabled())
                LOG.debug("[GrailsDomainBinder] Binding property [" + currentGrailsProp.getName() + "] as OneToOne");

           if (canBindOneToOneWithSingleColumnAndForeignKey(currentGrailsProp)) {
                value = new OneToOne(table, persistentClass);
                bindOneToOne(currentGrailsProp, (OneToOne) value, path, mappings);
            }
            else {
                value = new ManyToOne(table);
                bindManyToOne(currentGrailsProp, (ManyToOne) value, path, mappings);
            }
        }
        
        else {
            if (LOG.isDebugEnabled())
                LOG.debug("[GrailsDomainBinder] Binding property [" + currentGrailsProp.getName() + "] as SimpleValue");

            value = new SimpleValue(table);
            if (currentGrailsProp.isEnum()) {
                bindEnumType(currentGrailsProp, (SimpleValue) value, path, mappings);
            }
			else {
            	bindSimpleValue(currentGrailsProp, componentProperty, (SimpleValue) value, path, mappings);
			}
        }

        if (value != null) {
            Property persistentProperty = createProperty(value, persistentClass, currentGrailsProp, mappings);
            component.addProperty(persistentProperty);
            if(isComponentPropertyNullable(componentProperty)) {
                final Iterator columnIterator = value.getColumnIterator();
                while(columnIterator.hasNext()) {
                    Column c = (Column) columnIterator.next();
                    c.setNullable(true);
                }
            }
        }
    }

    private static boolean isComponentPropertyNullable(GrailsDomainClassProperty componentProperty) {
        if(componentProperty == null) return false;
        final GrailsDomainClass domainClass = componentProperty.getDomainClass();
        final Mapping mapping = getMapping(domainClass.getClazz());
        return !domainClass.isRoot() && (mapping == null || mapping.isTablePerHierarchy()) || componentProperty.isOptional();
    }

    /**
     * Creates a persistant class property based on the GrailDomainClassProperty instance
     *
     * @param value
     * @param persistentClass
     * @param mappings
     */
    private static Property createProperty(Value value, PersistentClass persistentClass, GrailsDomainClassProperty grailsProperty, Mappings mappings) {
        // set type
        value.setTypeUsingReflection(persistentClass.getClassName(), grailsProperty.getName());


        if (value.getTable() != null)
            value.createForeignKey();

        Property prop = new Property();

        PropertyConfig config = getPropertyConfig(grailsProperty);
        prop.setValue(value);

        bindProperty(grailsProperty, prop, mappings);
        return prop;
    }

    /**
     * @param property
     * @param oneToOne
     * @param mappings
     */
/*	private static void bindOneToOne(GrailsDomainClassProperty property, OneToOne oneToOne, Mappings mappings) {

		// bind value
		bindSimpleValue(property, oneToOne, mappings );
		// set foreign key type
		oneToOne.setForeignKeyType( ForeignKeyDirection.FOREIGN_KEY_TO_PARENT );

		oneToOne.setForeignKeyName( property.getFieldName() + FOREIGN_KEY_SUFFIX );

		// TODO configure fetch settings
		oneToOne.setFetchMode( FetchMode.DEFAULT );
		// TODO configure lazy loading
		oneToOne.setLazy(true);

		oneToOne.setPropertyName( property.getTagName() );
		oneToOne.setReferencedEntityName( property.getType().getTagName() );


	}*/

    /**
     * @param currentGrailsProp
     * @param one
     * @param mappings
     */
    private static void bindOneToMany(GrailsDomainClassProperty currentGrailsProp, OneToMany one, Mappings mappings) {
        one.setReferencedEntityName(currentGrailsProp.getReferencedPropertyType().getName());
        PropertyConfig config = getPropertyConfig(currentGrailsProp);
        one.setIgnoreNotFound(true);
    }

    /**
     * Binds a many-to-one relationship to the
     *
     * @param property
     * @param manyToOne
     * @param path
     * @param mappings
     */
    private static void bindManyToOne(GrailsDomainClassProperty property, ManyToOne manyToOne, String path, Mappings mappings) {
        bindManyToOneValues(property, manyToOne);
        GrailsDomainClass refDomainClass = property.isManyToMany() ? property.getDomainClass() : property.getReferencedDomainClass();
        Mapping mapping = getMapping(refDomainClass);
        if(hasCompositeIdentifier(mapping)) {
            CompositeIdentity ci = (CompositeIdentity) mapping.getIdentity();
            bindCompositeIdentifierToManyToOne(property, manyToOne, ci, refDomainClass, path);
        }
        else {
            if(property.isCircular() && property.isManyToMany()) {
                PropertyConfig pc = getPropertyConfig(property);

                if(pc == null) {
                    if(mapping == null) {
                        mapping = new Mapping();
                        MAPPING_CACHE.put(refDomainClass.getClazz(), mapping);
                    }
                    pc = new PropertyConfig();
                    mapping.getColumns().put(property.getName(), pc);
                }
                if(!hasJoinKeyMapping(pc)) {
                    JoinTable jt = new JoinTable();
                    final ColumnConfig columnConfig = new ColumnConfig();
                    columnConfig.setName(namingStrategy.propertyToColumnName(property.getName())+UNDERSCORE+FOREIGN_KEY_SUFFIX);
                    jt.setKey(columnConfig);
                    pc.setJoinTable(jt);
                }
                bindSimpleValue(property, manyToOne, path, pc);
            }
            else {

                // bind column
                bindSimpleValue(property, null, manyToOne, path, mappings);
            }

        }

        PropertyConfig config = getPropertyConfig(property);
        if(property.isOneToOne()) {
            manyToOne.setAlternateUniqueKey(true);
            Column c = getColumnForSimpleValue(manyToOne);
            if(config != null) {
                c.setUnique(config.isUnique());
            }
            else if(property.isBidirectional() && property.getOtherSide().isHasOne()) {
                c.setUnique(true);
            }
        }

    }

    private static void bindCompositeIdentifierToManyToOne(GrailsDomainClassProperty property, SimpleValue value, CompositeIdentity compositeId, GrailsDomainClass refDomainClass, String path) {
        String[] propertyNames = compositeId.getPropertyNames();
        PropertyConfig config = getPropertyConfig(property);
        if(config == null) config = new PropertyConfig();

        for (String propertyName : propertyNames) {
            final ColumnConfig cc = new ColumnConfig();

            cc.setName(namingStrategy.classToTableName(refDomainClass.getShortName()) + UNDERSCORE +getDefaultColumnName(refDomainClass.getPropertyByName(propertyName)));
            config.getColumns().add(cc);
        }
        bindSimpleValue(property, value, path, config);
    }

    private static boolean hasCompositeIdentifier(Mapping mapping) {
        return mapping != null && (mapping.getIdentity() instanceof CompositeIdentity);
    }

    private static void bindOneToOne(final GrailsDomainClassProperty property, OneToOne oneToOne, String path, Mappings mappings) {
        PropertyConfig config = getPropertyConfig(property);
        final GrailsDomainClassProperty otherSide = property.getOtherSide();

        oneToOne.setConstrained(otherSide.isHasOne());
        oneToOne.setForeignKeyType( oneToOne.isConstrained() ?
                                    ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT :
                                    ForeignKeyDirection.FOREIGN_KEY_TO_PARENT);
        oneToOne.setAlternateUniqueKey(true);
        
        if (config != null && config.getFetch() != null) {
            oneToOne.setFetchMode(config.getFetch());
        }
        else {
            oneToOne.setFetchMode(FetchMode.DEFAULT);
        }

        oneToOne.setReferencedEntityName(otherSide.getDomainClass().getFullName());
        if(otherSide.isHasOne()) {
            PropertyConfig pc = getPropertyConfig(property);
            bindSimpleValue(property, oneToOne, path, pc);
        }
        else {

            oneToOne.setReferencedPropertyName(otherSide.getName());
        }
    }


    /**
     * @param property
     * @param manyToOne
     */
    private static void bindManyToOneValues(GrailsDomainClassProperty property, ManyToOne manyToOne) {
        PropertyConfig config = getPropertyConfig(property);

        if (config != null && config.getFetch() != null) {
            manyToOne.setFetchMode(config.getFetch());
        }
        else {
            manyToOne.setFetchMode(FetchMode.DEFAULT);
        }

        if (config != null) {
           manyToOne.setIgnoreNotFound(config.getIgnoreNotFound());
        }

        // set referenced entity
        manyToOne.setReferencedEntityName(property.getReferencedPropertyType().getName());
    }

    /**
     * @param version
     * @param mappings
     */
    private static void bindVersion(GrailsDomainClassProperty version, RootClass entity, Mappings mappings) {

        SimpleValue val = new SimpleValue(entity.getTable());

        bindSimpleValue(version, null, val, EMPTY_PATH, mappings);

        if (!val.isTypeSpecified()) {
            val.setTypeName("version".equals(version.getName()) ? "integer" : "timestamp");
        }
        Property prop = new Property();
        prop.setValue(val);

        bindProperty(version, prop, mappings);
        val.setNullValue("undefined");
        entity.setVersion(prop);
        entity.addProperty(prop);
    }

    /**
     * @param identifier
     * @param entity
     * @param mappings
     * @param mappedId
     */
    private static void bindSimpleId(GrailsDomainClassProperty identifier, RootClass entity, Mappings mappings, Identity mappedId) {

        // create the id value
        SimpleValue id = new SimpleValue(entity.getTable());
        // set identifier on entity

        Properties params = new Properties();
        entity.setIdentifier(id);


        if (mappedId != null) {
            id.setIdentifierGeneratorStrategy(mappedId.getGenerator());
            params.putAll(mappedId.getParams());
            if("assigned".equals(mappedId.getGenerator())) {
                id.setNullValue("undefined");
            }

        } else {
            // configure generator strategy
            id.setIdentifierGeneratorStrategy("native");

        }

        if (mappings.getSchemaName() != null) {
            params.setProperty(PersistentIdentifierGenerator.SCHEMA, mappings.getSchemaName());
        }
        if (mappings.getCatalogName() != null) {
            params.setProperty(PersistentIdentifierGenerator.CATALOG, mappings.getCatalogName());
        }
        id.setIdentifierGeneratorProperties(params);

        // bind value
        bindSimpleValue(identifier, null, id, EMPTY_PATH, mappings);

        // create property
        Property prop = new Property();
        prop.setValue(id);

        // bind property
        bindProperty(identifier, prop, mappings);
        // set identifier property
        entity.setIdentifierProperty(prop);

        id.getTable().setIdentifierValue(id);

    }

    /**
     * Binds a property to Hibernate runtime meta model. Deals with cascade strategy based on the Grails domain model
     *
     * @param grailsProperty The grails property instance
     * @param prop           The Hibernate property
     * @param mappings       The Hibernate mappings
     */
    private static void bindProperty(GrailsDomainClassProperty grailsProperty, Property prop, Mappings mappings) {
        // set the property name
        prop.setName(grailsProperty.getName());
        if (isBidirectionalManyToOneWithListMapping(grailsProperty, prop)) {
            prop.setInsertable(false);
            prop.setUpdateable(false);
        } else {
            prop.setInsertable(true);
            prop.setUpdateable(true);
        }

        prop.setPropertyAccessorName(mappings.getDefaultAccess());
        prop.setOptional(grailsProperty.isOptional());

        setCascadeBehaviour(grailsProperty, prop);

        // lazy to true
        boolean isLazyable = grailsProperty.isOneToOne() ||
                             grailsProperty.isManyToOne() ||
                             grailsProperty.isEmbedded() ||
                             grailsProperty.isPersistent() && !grailsProperty.isAssociation() && !grailsProperty.isIdentity();

        if(isLazyable) {
            PropertyConfig config = getPropertyConfig(grailsProperty);
            final boolean isLazy = config!=null ? config.getLazy() : true;
            prop.setLazy(isLazy);

            if(isLazy && (grailsProperty.isManyToOne() || grailsProperty.isOneToOne())) {
                HibernatePluginSupport.handleLazyProxy(grailsProperty.getDomainClass(), grailsProperty);
            }

        }

    }

    private static boolean isBidirectionalManyToOneWithListMapping(GrailsDomainClassProperty grailsProperty, Property prop) {
        GrailsDomainClassProperty otherSide = grailsProperty.getOtherSide();
        return grailsProperty.isBidirectional() && otherSide != null && prop.getValue() instanceof ManyToOne && List.class.isAssignableFrom(otherSide.getType());
    }

    private static void setCascadeBehaviour(GrailsDomainClassProperty grailsProperty, Property prop) {
        String cascadeStrategy = "none";
        // set to cascade all for the moment
        GrailsDomainClass domainClass = grailsProperty.getDomainClass();
        PropertyConfig config = getPropertyConfig(grailsProperty);
        GrailsDomainClass referenced = grailsProperty.getReferencedDomainClass();
        if (config != null && config.getCascade() != null) {
            cascadeStrategy = config.getCascade();
        } else if (grailsProperty.isAssociation()) {
            if(grailsProperty.isHasOne()) {
               cascadeStrategy = CASCADE_ALL;
            }
            else if (grailsProperty.isOneToOne()) {
                if (referenced != null && referenced.isOwningClass(domainClass.getClazz()))
                    cascadeStrategy = CASCADE_ALL;
            } else if (grailsProperty.isOneToMany()) {
                if (referenced != null && referenced.isOwningClass(domainClass.getClazz()))
                    cascadeStrategy = CASCADE_ALL;
                else
                    cascadeStrategy = CASCADE_SAVE_UPDATE;
            } else if (grailsProperty.isManyToMany()) {
                if ((referenced != null && referenced.isOwningClass(domainClass.getClazz())) || grailsProperty.isCircular())
                    cascadeStrategy = CASCADE_SAVE_UPDATE;
            } else if (grailsProperty.isManyToOne()) {

                if (referenced != null && referenced.isOwningClass(domainClass.getClazz()) && !isCircularAssociation(grailsProperty))
                    cascadeStrategy = CASCADE_ALL;
                else
                    cascadeStrategy = CASCADE_NONE;
            }
        }
        else if (grailsProperty.isBasicCollectionType()) {
            cascadeStrategy = CASCADE_ALL;
        }
        else if (Map.class.isAssignableFrom(grailsProperty.getType())) {
            referenced = grailsProperty.getReferencedDomainClass();
            if (referenced != null && referenced.isOwningClass(grailsProperty.getDomainClass().getClazz())) {
                cascadeStrategy = CASCADE_ALL;
            } else {
                cascadeStrategy = CASCADE_SAVE_UPDATE;
            }
        }
        logCascadeMapping(grailsProperty, cascadeStrategy, referenced);
        prop.setCascade(cascadeStrategy);
    }

    private static boolean isCircularAssociation(GrailsDomainClassProperty grailsProperty) {
        return grailsProperty.getType().equals(grailsProperty.getDomainClass().getClazz());
    }

    private static void logCascadeMapping(GrailsDomainClassProperty grailsProperty, String cascadeStrategy, GrailsDomainClass referenced) {
        if (LOG.isDebugEnabled() && grailsProperty.isAssociation() && referenced!=null) {
            String assType = getAssociationDescription(grailsProperty);
            LOG.debug("Mapping cascade strategy for " + assType + " property " + grailsProperty.getDomainClass().getFullName() + "." + grailsProperty.getName() + " referencing type [" + referenced.getClazz() + "] -> [CASCADE: " + cascadeStrategy + "]");
        }
    }

    private static String getAssociationDescription(GrailsDomainClassProperty grailsProperty) {
        String assType = "unknown";
        if (grailsProperty.isManyToMany()) {
            assType = "many-to-many";
        } else if (grailsProperty.isOneToMany()) {
            assType = "one-to-many";
        } else if (grailsProperty.isOneToOne()) {
            assType = "one-to-one";
        } else if (grailsProperty.isManyToOne()) {
            assType = "many-to-one";
        } else if (grailsProperty.isEmbedded()) {
            assType = "embedded";
        }
        return assType;
    }

    /**
     * Binds a simple value to the Hibernate metamodel. A simple value is
     * any type within the Hibernate type system
     *
     * @param property
     * @param parentProperty
     * @param simpleValue The simple value to bind
     * @param path
     * @param mappings    The Hibernate mappings instance
     */
    private static void bindSimpleValue(GrailsDomainClassProperty property, GrailsDomainClassProperty parentProperty, SimpleValue simpleValue, String path, Mappings mappings) {
        // set type
        PropertyConfig propertyConfig = getPropertyConfig(property);
        bindSimpleValue(property,parentProperty, simpleValue, path, propertyConfig);
    }

    private static void bindSimpleValue(GrailsDomainClassProperty grailsProp, SimpleValue simpleValue, String path, PropertyConfig propertyConfig) {
        bindSimpleValue(grailsProp, null, simpleValue, path, propertyConfig);
    }

    private static void bindSimpleValue(GrailsDomainClassProperty grailsProp, GrailsDomainClassProperty parentProperty, SimpleValue simpleValue, String path, PropertyConfig propertyConfig) {
        setTypeForPropertyConfig(grailsProp, simpleValue, propertyConfig);
        Table table = simpleValue.getTable();

        // Add the column definitions for this value/property. Note that
        // not all custom mapped properties will have column definitions,
        // in which case we still need to create a Hibernate column for
        // this value.
        List columnDefinitions = propertyConfig != null ? propertyConfig.getColumns() : Arrays.asList(new Object[]{null});
        if (columnDefinitions.isEmpty()) columnDefinitions = Arrays.asList(new Object[]{null});

        for (int i = 0, n = columnDefinitions.size(); i < n; i++) {
            ColumnConfig cc = (ColumnConfig) columnDefinitions.get(i);
            Column column = new Column();

            // Check for explicitly mapped column name and SQL type.
            if (cc != null) {
                if (cc.getName() != null) {
                    column.setName(cc.getName());
                }
                if (cc.getSqlType() != null) {
                    column.setSqlType(cc.getSqlType());
                }
            }

            column.setValue(simpleValue);
            bindColumn(grailsProp, parentProperty,column, cc, path, table);

            if (table != null) table.addColumn(column);

            simpleValue.addColumn(column);
        }
    }

    private static void setTypeForPropertyConfig(GrailsDomainClassProperty grailsProp, SimpleValue simpleValue, PropertyConfig config) {
        if (config != null && config.getType() != null) {
            Object type = config.getType();
            if (type instanceof Class) {
                simpleValue.setTypeName(((Class) type).getName());
            } else {
                simpleValue.setTypeName(type.toString());
            }
        } else {
            simpleValue.setTypeName(grailsProp.getType().getName());
        }
    }

    /**
     * Binds a value for the specified parameters to the meta model.
     *
     * @param type        The type of the property
     * @param simpleValue The simple value instance
     * @param nullable    Whether it is nullable
     * @param columnName  The property name
     * @param mappings    The mappings
     */
    private static void bindSimpleValue(String type, SimpleValue simpleValue, boolean nullable, String columnName, Mappings mappings) {

        simpleValue.setTypeName(type);
        Table t = simpleValue.getTable();
        Column column = new Column();
        column.setNullable(nullable);
        column.setValue(simpleValue);
        column.setName(columnName);
        if (t != null) t.addColumn(column);

        simpleValue.addColumn(column);
    }

    /**
     * Binds a Column instance to the Hibernate meta model
     *
     * @param property The Grails domain class property
     * @param parentProperty
     * @param column     The column to bind
     * @param path
     * @param table      The table name
     */
    private static void bindColumn(
            GrailsDomainClassProperty property,
            GrailsDomainClassProperty parentProperty,
            Column column,
            ColumnConfig cc,
            String path,
            Table table) {
        Class userType = getUserType(property);
        String columnName = getColumnNameForPropertyAndPath(property, path, cc);
        if((property.isAssociation() || property.isBasicCollectionType()) && userType == null) {
            // Only use conventional naming when the column has not been explicitly mapped.
            if (column.getName() == null) {
                column.setName(columnName);
            }
            if (property.isManyToMany()) {
                column.setNullable(false);
            }
            else if (property.isOneToOne() && property.isBidirectional() && !property.isOwningSide()) {
                if(property.getOtherSide().isHasOne())
                    column.setNullable(false);
                else
                    column.setNullable(true);
            }
            else if((property.isManyToOne() || property.isOneToOne()) && property.isCircular() ) {
                column.setNullable(true);
            }
            else {
                column.setNullable(property.isOptional());
            }
        }
        else {
            column.setName(columnName);
            column.setNullable(property.isOptional() || (parentProperty != null && parentProperty.isOptional() ));

            // Use the constraints for this property to more accurately define
            // the column's length, precision, and scale
            ConstrainedProperty constrainedProperty = getConstrainedProperty(property);
            if (constrainedProperty != null) {
                if (String.class.isAssignableFrom(property.getType()) || byte[].class.isAssignableFrom(property.getType())) {
                    bindStringColumnConstraints(column, constrainedProperty);
                }

                if (Number.class.isAssignableFrom(property.getType())) {
                    bindNumericColumnConstraints(column, constrainedProperty);
                }
            }
        }

        ConstrainedProperty cp = getConstrainedProperty(property);
        if (cp != null && cp.hasAppliedConstraint(UniqueConstraint.UNIQUE_CONSTRAINT)) {
            UniqueConstraint uc = (UniqueConstraint) cp.getAppliedConstraint(UniqueConstraint.UNIQUE_CONSTRAINT);
            if (uc != null && uc.isUnique()) {
                if (!uc.isUniqueWithinGroup()) {
                    column.setUnique(true);
                } else if (uc.getUniquenessGroup().size() > 0) {
                    createKeyForProps(property, path, table, columnName, uc.getUniquenessGroup());
                }
            }
        } else {
            Object val = cp != null ? cp.getMetaConstraintValue(UniqueConstraint.UNIQUE_CONSTRAINT) : null;
            if (val instanceof Boolean) {
                column.setUnique(((Boolean) val).booleanValue());
            } else if (val instanceof String) {
                createKeyForProps(property, path, table, columnName, Arrays.asList(new String[]{(String) val}));
            } else if (val instanceof List && ((List) val).size() > 0) {
                createKeyForProps(property, path, table, columnName, (List) val);
            }
        }


        bindIndex(column, cc, table);

        if(!property.getDomainClass().isRoot()) {
            if(LOG.isDebugEnabled())
                LOG.debug("[GrailsDomainBinder] Sub class property [" + property.getName() + "] for column name ["+column.getName()+"] in table ["+table.getName()+"] set to nullable");
            column.setNullable(true);
        }

        if(LOG.isDebugEnabled())
            LOG.debug("[GrailsDomainBinder] bound property [" + property.getName() + "] to column name ["+column.getName()+"] in table ["+table.getName()+"]");
    }

    private static void createKeyForProps(GrailsDomainClassProperty grailsProp, String path, Table table, String columnName, List propertyNames) {
        List keyList = new ArrayList();
        keyList.add(new Column(columnName));
        for (Iterator i = ((List) propertyNames).iterator(); i.hasNext();) {
            String propertyName = (String) i.next();
            GrailsDomainClassProperty otherProp = grailsProp.getDomainClass().getPropertyByName(propertyName);
            String otherColumnName = getColumnNameForPropertyAndPath(otherProp, path, null);
            keyList.add(new Column(otherColumnName));
        }
        createUniqueKeyForColumns(table, columnName, keyList);
    }

    private static void createUniqueKeyForColumns(Table table, String columnName, List keyList) {
        Collections.reverse(keyList);
        UniqueKey key = table.getOrCreateUniqueKey("unique-" + columnName);
        List columns = key.getColumns();
        if (columns.size() == 0) {
            LOG.debug("create unique key for " + table.getName() + " columns = " + keyList);
            key.addColumns(keyList.iterator());
        }
    }

    private static void bindIndex(Column column, ColumnConfig cc, Table table) {
        if (cc != null) {
            String indexDefinition = cc.getIndex();
            if (indexDefinition != null) {
                String[] tokens = indexDefinition.split(",");
                for (int i = 0; i < tokens.length; i++) {
                    String index = tokens[i];
                    table.getOrCreateIndex(index).addColumn(column);
                }
            }
        }
    }

    private static String getColumnNameForPropertyAndPath(
            GrailsDomainClassProperty grailsProp,
            String path,
            ColumnConfig cc) {
        // First try the column config.
        String columnName = null;
        if (cc != null) {
            if(supportsJoinColumnMapping(grailsProp)) {
                PropertyConfig pc = getPropertyConfig(grailsProp);
                if(hasJoinKeyMapping(pc)) {
                    columnName = pc.getJoinTable().getKey().getName();
                }
                else {
                    columnName = cc.getName();
                }

            }
            else {
                columnName = cc.getName();
            }
        } else {
            // No column config given, so try to fetch it from the
            // mapping.
            GrailsDomainClass domainClass = grailsProp.getDomainClass();
            Mapping m = getMapping(domainClass.getClazz());
            if (m != null) {
                PropertyConfig c = m.getPropertyConfig(grailsProp.getName());

                if (supportsJoinColumnMapping(grailsProp) && hasJoinKeyMapping(c)) {
                   columnName = c.getJoinTable().getKey().getName();
                }
                else if (c != null && c.getColumn() != null) {
                    columnName = c.getColumn();
                }
            }
        }

        if (columnName == null) {
            if (StringHelper.isNotEmpty(path)) {
                columnName = namingStrategy.propertyToColumnName(path) +
                        UNDERSCORE +
                        getDefaultColumnName(grailsProp);
            } else {
                columnName = getDefaultColumnName(grailsProp);
            }
        }
        return columnName;
    }

    private static boolean hasJoinKeyMapping(PropertyConfig c) {
        return c != null && c.getJoinTable() != null && c.getJoinTable().getKey() != null;
    }

    private static boolean supportsJoinColumnMapping(GrailsDomainClassProperty grailsProp) {
        return grailsProp.isManyToMany() || isUnidirectionalOneToMany(grailsProp) || grailsProp.isBasicCollectionType();
    }

    private static String getDefaultColumnName(GrailsDomainClassProperty property) {
        String columnName = namingStrategy.propertyToColumnName(property.getName());
        if(property.isAssociation() && property.getReferencedDomainClass()!=null) {

            if(property.isManyToMany()) {
                return getForeignKeyForPropertyDomainClass(property);
            }
            else if(!property.isBidirectional() && property.isOneToMany()) {
                String prefix = namingStrategy.classToTableName(property.getDomainClass().getName());
                return prefix+ UNDERSCORE +columnName + FOREIGN_KEY_SUFFIX;
            }
            else {
                if(property.isInherited() && isBidirectionalManyToOne(property)) {
                    return namingStrategy.propertyToColumnName(property.getDomainClass().getName()) + '_'+ columnName + FOREIGN_KEY_SUFFIX ;
                }
                else {
                    return columnName + FOREIGN_KEY_SUFFIX ;
                }

            }
        }
        else if(property.isBasicCollectionType()) {
            return getForeignKeyForPropertyDomainClass(property);
        }
        else {
            return columnName;
        }
    }

    private static String getForeignKeyForPropertyDomainClass(GrailsDomainClassProperty property) {
        final String propertyName = property.getDomainClass().getPropertyName();
        return namingStrategy.propertyToColumnName(propertyName) + FOREIGN_KEY_SUFFIX;
    }


    private static String getIndexColumnName(GrailsDomainClassProperty property) {
        PropertyConfig pc = getPropertyConfig(property);
        if(pc != null && pc.getIndexColumn() != null && pc.getIndexColumn().getColumn() != null) {
            return pc.getIndexColumn().getColumn();
        }
        return namingStrategy.propertyToColumnName(property.getName()) + UNDERSCORE + IndexedCollection.DEFAULT_INDEX_COLUMN_NAME;
    }


    private static String getIndexColumnType(GrailsDomainClassProperty property, String defaultType) {
        PropertyConfig pc = getPropertyConfig(property);
        if(pc != null && pc.getIndexColumn() != null && pc.getIndexColumn().getType() != null) {
            return getTypeName(pc.getIndexColumn());
        }
        return defaultType;
    }


    private static String getMapElementName(GrailsDomainClassProperty property) {
        PropertyConfig pc = getPropertyConfig(property);

        if(hasJoinTableColumnNameMapping(pc)) {
            return pc.getJoinTable().getColumn().getName();
        }
        return namingStrategy.propertyToColumnName(property.getName()) + UNDERSCORE + IndexedCollection.DEFAULT_ELEMENT_COLUMN_NAME;
    }

    private static boolean hasJoinTableColumnNameMapping(PropertyConfig pc) {
        return pc != null && pc.getJoinTable() != null && pc.getJoinTable().getColumn() != null && pc.getJoinTable().getColumn().getName() != null;
    }


    /**
     * Returns the constraints applied to the specified domain class property.
     *
     * @param grailsProp the property whose constraints will be returned
     * @return the <code>ConstrainedProperty</code> object representing the property's constraints
     */
    private static ConstrainedProperty getConstrainedProperty(GrailsDomainClassProperty grailsProp) {
        ConstrainedProperty constrainedProperty = null;
        Map constraints = grailsProp.getDomainClass().getConstrainedProperties();
        for (Iterator constrainedPropertyIter = constraints.values().iterator(); constrainedPropertyIter.hasNext() && (constrainedProperty == null);) {
            ConstrainedProperty tmpConstrainedProperty = (ConstrainedProperty) constrainedPropertyIter.next();
            if (tmpConstrainedProperty.getPropertyName().equals(grailsProp.getName())) {
                constrainedProperty = tmpConstrainedProperty;
            }
        }
        return constrainedProperty;
    }

    /**
     * Interrogates the specified constraints looking for any constraints that would limit the
     * length of the property's value.  If such constraints exist, this method adjusts the length
     * of the column accordingly.
     *
     * @param column              the column that corresponds to the property
     * @param constrainedProperty the property's constraints
     */
    protected static void bindStringColumnConstraints(Column column, ConstrainedProperty constrainedProperty) {
        Integer columnLength = constrainedProperty.getMaxSize();
        List inListValues = constrainedProperty.getInList();
        if (columnLength != null) {
            column.setLength(columnLength.intValue());
        } else if (inListValues != null) {
            column.setLength(getMaxSize(inListValues));
        }
    }

    /**
     * Interrogates the specified constraints looking for any constraints that would limit the
     * precision and/or scale of the property's value.  If such constraints exist, this method adjusts
     * the precision and/or scale of the column accordingly.
     *
     * @param column              the column that corresponds to the property
     * @param constrainedProperty the property's constraints
     */
    protected static void bindNumericColumnConstraints(Column column, ConstrainedProperty constrainedProperty) {
        int scale = Column.DEFAULT_SCALE;
        int precision = Column.DEFAULT_PRECISION;

        if (constrainedProperty.getScale() != null) {
            scale = constrainedProperty.getScale().intValue();
            column.setScale(scale);
        }

        Comparable minConstraintValue = constrainedProperty.getMin();
        Comparable maxConstraintValue = constrainedProperty.getMax();

        int minConstraintValueLength = 0;
        if ((minConstraintValue != null) && (minConstraintValue instanceof Number)) {
            minConstraintValueLength = Math.max(
                    countDigits((Number) minConstraintValue),
                    countDigits(new Long(((Number) minConstraintValue).longValue())) + scale
            );
        }
        int maxConstraintValueLength = 0;
        if ((maxConstraintValue != null) && (maxConstraintValue instanceof Number)) {
            maxConstraintValueLength = Math.max(
                    countDigits((Number) maxConstraintValue),
                    countDigits(new Long(((Number) maxConstraintValue).longValue())) + scale
            );
        }

        if (minConstraintValueLength > 0 && maxConstraintValueLength > 0) {
            // If both of min and max constraints are setted we could use
            // maximum digits number in it as precision
            precision = NumberUtils.max(new int[]{minConstraintValueLength, maxConstraintValueLength});
        } else {
            // Overwise we should also use default precision
            precision = NumberUtils.max(new int[]{precision, minConstraintValueLength, maxConstraintValueLength});
        }

        column.setPrecision(precision);
    }

    /**
     * @return a count of the digits in the specified number
     */
    private static int countDigits(Number number) {
        int numDigits = 0;

        if (number != null) {
            // Remove everything that's not a digit (e.g., decimal points or signs)
            String digitsOnly = number.toString().replaceAll("\\D", EMPTY_PATH);
            numDigits = digitsOnly.length();
        }

        return numDigits;
    }

    /**
     * @return the maximum length of the strings in the specified list
     */
    private static int getMaxSize(List inListValues) {
        int maxSize = 0;

        for (Iterator iter = inListValues.iterator(); iter.hasNext();) {
            String value = (String) iter.next();
            maxSize = Math.max(value.length(), maxSize);
        }

        return maxSize;
    }
}
