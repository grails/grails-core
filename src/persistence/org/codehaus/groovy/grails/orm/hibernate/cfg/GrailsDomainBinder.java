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


import groovy.lang.Closure;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.cfg.Mappings;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.SecondPass;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.mapping.*;
import org.hibernate.mapping.Collection;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.usertype.UserType;
import org.hibernate.util.StringHelper;
import org.springframework.validation.Validator;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
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
 *
 * Created: 06-Jul-2005
 */
public final class GrailsDomainBinder {

	private static final String FOREIGN_KEY_SUFFIX = "_id";
	private static final Log LOG = LogFactory.getLog( GrailsDomainBinder.class );
	private static final NamingStrategy namingStrategy = ImprovedNamingStrategy.INSTANCE;
    private static final String STRING_TYPE = "string";
    private static final String EMPTY_PATH = "";
    private static final char UNDERSCORE = '_';
    private static final String CASCADE_ALL = "all";
    private static final String CASCADE_SAVE_UPDATE = "save-update";
    private static final String CASCADE_MERGE = "merge";

    private static final Map MAPPING_CACHE = new HashMap();


    /**
	 * A Collection type, for the moment only Set is supported
	 *
	 * @author Graeme
	 *
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
				bindCollection( property, coll, owner, mappings, path);
				return coll;
			}

		};

        private static CollectionType LIST = new CollectionType(List.class) {

            public Collection create(GrailsDomainClassProperty property, PersistentClass owner, String path, Mappings mappings) throws MappingException {
                org.hibernate.mapping.List coll = new org.hibernate.mapping.List(owner);
                coll.setCollectionTable(owner.getTable());
                bindCollection( property, coll, owner, mappings, path);
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
			INSTANCES.put( Set.class, SET );
			INSTANCES.put( SortedSet.class, SET );
            INSTANCES.put( List.class, LIST );
            INSTANCES.put( Map.class, MAP );
        }
		public static CollectionType collectionTypeForClass(Class clazz) {
			return (CollectionType)INSTANCES.get( clazz );
		}
	}


	/**
	 * Second pass class for grails relationships. This is required as all
	 * persistent classes need to be loaded in the first pass and then relationships
	 * established in the second pass compile
	 *
	 * @author Graeme
	 *
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
			bindCollectionSecondPass( this.property, mappings, persistentClasses, collection,inheritedMetas );
		}

		public void doSecondPass(Map persistentClasses) throws MappingException {
			bindCollectionSecondPass( this.property, mappings, persistentClasses, collection,Collections.EMPTY_MAP );
		}

	}

    static class ListSecondPass extends GrailsCollectionSecondPass {
        public ListSecondPass(GrailsDomainClassProperty property, Mappings mappings, Collection coll) {
            super(property, mappings, coll);
        }


        public void doSecondPass(Map persistentClasses, Map inheritedMetas) throws MappingException {
            bindListSecondPass(this.property, mappings, persistentClasses, (org.hibernate.mapping.List)collection, inheritedMetas);
        }

        public void doSecondPass(Map persistentClasses) throws MappingException {
            bindListSecondPass(this.property, mappings, persistentClasses, (org.hibernate.mapping.List)collection, Collections.EMPTY_MAP);
        }
    }

    static class MapSecondPass extends GrailsCollectionSecondPass {

        public MapSecondPass(GrailsDomainClassProperty property, Mappings mappings, Collection coll) {
            super(property, mappings, coll);
        }


        public void doSecondPass(Map persistentClasses, Map inheritedMetas) throws MappingException {
            bindMapSecondPass( this.property, mappings, persistentClasses, (org.hibernate.mapping.Map)collection, inheritedMetas);
        }


        public void doSecondPass(Map persistentClasses) throws MappingException {
            bindMapSecondPass( this.property, mappings, persistentClasses, (org.hibernate.mapping.Map)collection, Collections.EMPTY_MAP);
        }
    }

    private static void bindMapSecondPass(GrailsDomainClassProperty property, Mappings mappings, Map persistentClasses, org.hibernate.mapping.Map map, Map inheritedMetas) {
        bindCollectionSecondPass(property, mappings, persistentClasses, map, inheritedMetas);
        String columnName = namingStrategy.propertyToColumnName(property.getName());

        SimpleValue value = new SimpleValue( map.getCollectionTable() );

        bindSimpleValue(STRING_TYPE, value, false, columnName + UNDERSCORE + IndexedCollection.DEFAULT_INDEX_COLUMN_NAME,mappings);

        if ( !value.isTypeSpecified() ) {
            throw new MappingException( "map index element must specify a type: "
                + map.getRole() );
        }
        map.setIndex( value );

        if(!property.isOneToMany()) {

            SimpleValue elt = new SimpleValue( map.getCollectionTable() );
            map.setElement( elt );
            map.setInverse(false);

            bindSimpleValue(STRING_TYPE, elt, false, columnName + UNDERSCORE + IndexedCollection.DEFAULT_ELEMENT_COLUMN_NAME,mappings);
            elt.setTypeName(STRING_TYPE);
        }
        else {
            Value element = map.getElement();
            String entityName= ( (OneToMany) element).getReferencedEntityName();
			PersistentClass referenced = mappings.getClass( entityName );
			IndexBackref ib = new IndexBackref();
			ib.setName( UNDERSCORE + property.getName() + "IndexBackref" );
			ib.setUpdateable( false );
			ib.setSelectable( false );
			ib.setCollectionRole( map.getRole() );
			ib.setEntityName( map.getOwner().getEntityName() );
			ib.setValue( map.getIndex() );
			// ( (Column) ( (SimpleValue) ic.getIndex() ).getColumnIterator().next()
			// ).setNullable(false);
			referenced.addProperty( ib );
        }
    }


    private static void bindListSecondPass(GrailsDomainClassProperty property, Mappings mappings, Map persistentClasses, org.hibernate.mapping.List list, Map inheritedMetas) {
        bindCollectionSecondPass( property, mappings, persistentClasses, list,inheritedMetas );

        String columnName = namingStrategy.propertyToColumnName(property.getName()+ UNDERSCORE +IndexedCollection.DEFAULT_INDEX_COLUMN_NAME);

        SimpleValue iv = new SimpleValue( list.getCollectionTable() );
        bindSimpleValue("integer", iv, false,columnName, mappings);
        iv.setTypeName( "integer" );
        list.setIndex( iv );
        String entityName;
        Value element = list.getElement();
        if(element instanceof ManyToOne) {
            ManyToOne manyToOne = (ManyToOne) element;
            entityName = manyToOne.getReferencedEntityName();
        }
        else {
            entityName = ( (OneToMany) element).getReferencedEntityName();
        }

        PersistentClass referenced = mappings.getClass( entityName );
        IndexBackref ib = new IndexBackref();
        ib.setName( UNDERSCORE + property.getName() + "IndexBackref" );
        ib.setUpdateable( false );
        ib.setSelectable( false );
        ib.setCollectionRole( list.getRole() );
        ib.setEntityName( list.getOwner().getEntityName() );
        ib.setValue( list.getIndex() );
         ( (Column) ( (SimpleValue) list.getIndex() ).getColumnIterator().next()
         ).setNullable(true);
        referenced.addProperty( ib );

    }


    private static void bindCollectionSecondPass(GrailsDomainClassProperty property, Mappings mappings, Map persistentClasses, Collection collection, Map inheritedMetas) {

		PersistentClass associatedClass = null;

		if(LOG.isDebugEnabled())
			LOG.debug( "Mapping collection: "
					+ collection.getRole()
					+ " -> "
					+ collection.getCollectionTable().getName() );

        ColumnConfig cc = getColumnConfig(property);
        // Configure one-to-many
        if(collection.isOneToMany() ) {

            GrailsDomainClass referenced = property.getReferencedDomainClass();
            if(referenced != null && !referenced.isRoot()) {
                // NOTE: Work around for http://opensource.atlassian.com/projects/hibernate/browse/HHH-2855
                collection.setWhere(RootClass.DEFAULT_DISCRIMINATOR_COLUMN_NAME + " = '"+referenced.getFullName()+"'");
            }

            OneToMany oneToMany = (OneToMany)collection.getElement();
            String associatedClassName = oneToMany.getReferencedEntityName();

            associatedClass = (PersistentClass)persistentClasses.get(associatedClassName);
            // if there is no persistent class for the association throw
            // exception
            if(associatedClass == null) {
                throw new MappingException( "Association references unmapped class: " + oneToMany.getReferencedEntityName() );
            }

            oneToMany.setAssociatedClass( associatedClass );
            if(shouldBindCollectionWithForeignKey(property)) {
                collection.setCollectionTable( associatedClass.getTable() );
            }

            bindCollectionForColumnConfig(collection, cc);

		}

        if(isSorted(property)) {
            collection.setSorted(true);
        }

        // setup the primary key references
		DependantValue key = createPrimaryKeyValue(property, collection,persistentClasses);

		// link a bidirectional relationship
		if(property.isBidirectional()) {
			GrailsDomainClassProperty otherSide = property.getOtherSide();
			if(otherSide.isManyToOne()) {
				linkBidirectionalOneToMany(collection, associatedClass, key, otherSide);
			}
			else if(property.isManyToMany() /*&& property.isOwningSide()*/) {
				bindDependentKeyValue(property,key,mappings);
			}
		}
		else {
            if(cc != null && cc.getJoinTable() != null && cc.getJoinTable().getKey() != null) {                   
                bindSimpleValue("long", key,false, cc.getJoinTable().getKey(), mappings);
            }
            else {
                bindDependentKeyValue(property,key,mappings);
            }
		}
		collection.setKey( key );

        // get cache config
        if(cc != null) {
            CacheConfig cacheConfig = cc.getCache();
            if(cacheConfig != null) {
                collection.setCacheConcurrencyStrategy(cacheConfig.getUsage());
            }
        }



        // if we have a many-to-many
		if(property.isManyToMany() ) {
			GrailsDomainClassProperty otherSide = property.getOtherSide();

			if(property.isBidirectional()) {
				if(LOG.isDebugEnabled())
					LOG.debug("[GrailsDomainBinder] Mapping other side "+otherSide.getDomainClass().getName()+"."+otherSide.getName()+" -> "+collection.getCollectionTable().getName()+" as ManyToOne");
				ManyToOne element = new ManyToOne( collection.getCollectionTable() );
				bindManyToMany(otherSide, element, mappings);
				collection.setElement(element);
                bindCollectionForColumnConfig(collection, cc);

            }
			else {
				// TODO support unidirectional many-to-many
			}

		} else if (shouldCollectionBindWithJoinColumn(property)) {
            bindCollectionWithJoinTable(property, mappings, collection, cc);

        }
        else if(isUnidirectionalOneToMany(property)) {
            // for non-inverse one-to-many, with a not-null fk, add a backref!
            // there are problems with list and map mappings and join columns relating to duplicate key constraints
            // TODO change this when HHH-1268 is resolved
            bindUnidirectionalOneToMany(property, mappings, collection);
        }
    }

    private static void bindCollectionWithJoinTable(GrailsDomainClassProperty property, Mappings mappings, Collection collection, ColumnConfig cc) {
        // for a normal unidirectional one-to-many we use a join column
        ManyToOne element = new ManyToOne( collection.getCollectionTable() );

        bindUnidirectionalOneToManyInverseValues(property, element);

        String columnName;
        JoinTable jt = cc.getJoinTable();
        if(jt != null && jt.getKey()!=null)
            columnName = jt.getColumn();
        else
            columnName = namingStrategy.propertyToColumnName(property.getReferencedDomainClass().getPropertyName()) + FOREIGN_KEY_SUFFIX;
        
        bindSimpleValue("long", element,false, columnName, mappings);
        // we make the other side of the join unique so that it because a unidirectional one-to-many with a join instead of a many-to-many
        ((Column)element.getColumnIterator().next()).setUnique(true);

        collection.setElement(element);
        collection.setInverse(false);
        bindCollectionForColumnConfig(collection, cc);
    }

    private static boolean shouldCollectionBindWithJoinColumn(GrailsDomainClassProperty property) {
        ColumnConfig cc = getColumnConfig(property);
        boolean join = cc != null && cc.getJoinTable() != null;

        return isUnidirectionalOneToMany(property) && !isListOrMapCollection(property) && join;
    }


    /**
     * @param property
     * @param manyToOne
     */
    private static void bindUnidirectionalOneToManyInverseValues(GrailsDomainClassProperty property, ManyToOne manyToOne) {
        ColumnConfig cc = getColumnConfig(property);
        if(cc != null) {
           manyToOne.setLazy(cc.getLazy());
        }
        else {
            manyToOne.setLazy(true);
        }

        // set referenced entity
        manyToOne.setReferencedEntityName( property.getReferencedPropertyType().getName() );
        manyToOne.setIgnoreNotFound(true);
    }


    private static void bindCollectionForColumnConfig(Collection collection, ColumnConfig cc) {
        if(cc!=null) {
            collection.setLazy(cc.getLazy());
        }
        else {
            collection.setLazy(true);
        }
    }

    private static ColumnConfig getColumnConfig(GrailsDomainClassProperty property) {
        Mapping m = getMapping(property.getDomainClass().getFullName());
        ColumnConfig cc = m != null ? m.getColumn(property.getName()) : null;
        return cc;
    }

    /**
	 * Checks whether a property is a unidirectional non-circular one-to-many
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
	 * @param key The key
	 * @param mappings The mappings
	 */
	private static void bindDependentKeyValue(GrailsDomainClassProperty property, DependantValue key, Mappings mappings) {
		if(LOG.isDebugEnabled())
			LOG.debug("[GrailsDomainBinder] binding  ["+property.getName()+"] with dependant key");

		bindSimpleValue(property, key, EMPTY_PATH, mappings);
	}

	/**
	 * Creates the DependentValue object that forms a primary key reference for the collection
	 *
	 * @param property The grails property
	 * @param collection The collection object
	 * @param persistentClasses
	 * @return The DependantValue (key)
	 */
	private static DependantValue createPrimaryKeyValue(GrailsDomainClassProperty property, Collection collection, Map persistentClasses) {
		KeyValue keyValue;
		DependantValue key;
		String propertyRef = collection.getReferencedPropertyName();
		// this is to support mapping by a property
		if(propertyRef == null) {
			keyValue = collection.getOwner().getIdentifier();
		}
		else {
			keyValue = (KeyValue)collection.getOwner().getProperty( propertyRef ).getValue();
		}

		if(LOG.isDebugEnabled())
			LOG.debug( "[GrailsDomainBinder] creating dependant key value  to table ["+keyValue.getTable().getName()+"]");

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
            String entityName;
            if(v instanceof ManyToOne) {
                entityName = ((ManyToOne)v).getReferencedEntityName();
            }
            else {
                entityName = ((OneToMany)v).getReferencedEntityName();
            }
			PersistentClass referenced = mappings.getClass( entityName );
			Backref prop = new Backref();
		    prop.setEntityName(property.getDomainClass().getFullName());
		    prop.setName(UNDERSCORE + property.getDomainClass().getShortName() + UNDERSCORE + property.getName() + "Backref" );
			prop.setUpdateable( false );
			prop.setInsertable( true );
			prop.setCollectionRole( collection.getRole() );
			prop.setValue( collection.getKey() );
			prop.setOptional( true );

			referenced.addProperty( prop );
	}


	private static Property getProperty(PersistentClass associatedClass,
										String propertyName) 
	throws MappingException {
		try {
			return associatedClass.getProperty(propertyName);
		} catch (MappingException e) {
			//maybe it's squirreled away in a composite primary key
			if (associatedClass.getKey() instanceof Component) {
				return ((Component)associatedClass.getKey()).getProperty(propertyName);
			} else {
				throw e;
			}
		}
	}


	/**
	 * Links a bidirectional one-to-many, configuring the inverse side and using a column copy to perform the link
	 *
	 * @param collection The collection one-to-many
	 * @param associatedClass The associated class
	 * @param key The key
	 * @param otherSide The other side of the relationship
	 */
	private static void linkBidirectionalOneToMany(Collection collection, PersistentClass associatedClass, DependantValue key, GrailsDomainClassProperty otherSide) {
		collection.setInverse(true);

		//		Iterator mappedByColumns = associatedClass.getProperty( otherSide.getName() ).getValue().getColumnIterator();
		Iterator mappedByColumns = getProperty(associatedClass, otherSide.getName())
			.getValue().getColumnIterator();
		while(mappedByColumns.hasNext()) {
			Column column = (Column)mappedByColumns.next();
			linkValueUsingAColumnCopy(otherSide,column,key);
		}
	}

	/**
	 * Establish whether a collection property is sorted
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
	 *
	 * The element is a ManyToOne from the association table to the target entity
	 *
	 * @param property The grails property
	 * @param element The ManyToOne element
	 * @param mappings The mappings
	 */
	private static void bindManyToMany(GrailsDomainClassProperty property, ManyToOne element, Mappings mappings) {
		bindManyToOne(property,element, EMPTY_PATH, mappings);
		element.setReferencedEntityName(property.getDomainClass().getFullName());
	}

	private static void linkValueUsingAColumnCopy(GrailsDomainClassProperty prop, Column column, DependantValue key) {
		Column mappingColumn = new Column();
		mappingColumn.setName(column.getName());
		mappingColumn.setLength(column.getLength());
		mappingColumn.setNullable(prop.isOptional());
		mappingColumn.setSqlType(column.getSqlType());

		mappingColumn.setValue(key);
		key.addColumn( mappingColumn );
		key.getTable().addColumn( mappingColumn );
	}

	/**
	 * First pass to bind collection to Hibernate metamodel, sets up second pass
	 *
	 * @param property The GrailsDomainClassProperty instance
     * @param collection The collection
     * @param owner The owning persistent class
     * @param mappings The Hibernate mappings instance
     * @param path
     */
	private static void bindCollection(GrailsDomainClassProperty property, Collection collection, PersistentClass owner, Mappings mappings, String path) {

        // set role
        String propertyName = getNameForPropertyAndPath(property, path);
        collection.setRole( StringHelper.qualify( property.getDomainClass().getFullName() , propertyName ) );

        // configure eager fetching
        if(property.getFetchMode() == GrailsDomainClassProperty.FETCH_EAGER) {
            collection.setFetchMode(FetchMode.JOIN);
        }
        else {
            collection.setFetchMode( FetchMode.DEFAULT );
        }


        // if its a one-to-many mapping
        if(shouldBindCollectionWithForeignKey(property)) {
            OneToMany oneToMany = new OneToMany( collection.getOwner() );
            collection.setElement( oneToMany );
            bindOneToMany( property, oneToMany, mappings );
        }
        else {
            bindCollectionTable(property, mappings, collection);

            if(!property.isOwningSide()) {
                collection.setInverse(true);
            }
        }


		// setup second pass
        if(collection instanceof org.hibernate.mapping.Set)
            mappings.addSecondPass( new GrailsCollectionSecondPass(property, mappings, collection) );
        else if(collection instanceof org.hibernate.mapping.List) {
            mappings.addSecondPass( new ListSecondPass(property, mappings, collection) );
        }
        else if(collection instanceof org.hibernate.mapping.Map) {
            mappings.addSecondPass( new MapSecondPass(property, mappings, collection));
        }

    }

    /*
     * We bind collections with foreign keys if specified in the mapping and only if it is a unidirectional one-to-many
     * that is
     */
    private static boolean shouldBindCollectionWithForeignKey(GrailsDomainClassProperty property) {
        ColumnConfig cc = getColumnConfig(property);
        boolean join = cc != null && cc.getJoinTable() != null;
        return (property.isOneToMany() && property.isBidirectional()) || 
                (isUnidirectionalOneToMany(property) && !join)   ||
                (property.isOneToMany() && isListOrMapCollection(property));
    }

    private static boolean isListOrMapCollection(GrailsDomainClassProperty property) {
        return Map.class.isAssignableFrom(property.getType()) || List.class.isAssignableFrom(property.getType());
    }

    private static String getNameForPropertyAndPath(GrailsDomainClassProperty property, String path) {
        String propertyName;
        if(StringHelper.isNotEmpty(path))  {
            propertyName = StringHelper.qualify(path, property.getName());
        }
        else {
            propertyName = property.getName();
        }
        return propertyName;
    }

    private static void bindCollectionTable(GrailsDomainClassProperty property, Mappings mappings, Collection collection) {
        String tableName = calculateTableForMany(property);
        Table t =  mappings.addTable(
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
        if(Map.class.isAssignableFrom(property.getType())) {
            String tablePrefix = getTableName(property.getDomainClass());
            return tablePrefix + "_" + namingStrategy.propertyToColumnName(property.getName());

        }
        else {
            ColumnConfig cc = getColumnConfig(property);
            JoinTable jt = cc != null ? cc.getJoinTable() : null;

            if(property.isManyToMany() && jt != null) {
                 return jt.getName();
            }
            else {

                String left = getTableName(property.getDomainClass());
                String right = getTableName(property.getReferencedDomainClass());

                if(property.isOwningSide()) {
                    return left+ UNDERSCORE +right;
                }
                else if(shouldCollectionBindWithJoinColumn(property)) {
                    if(jt != null && jt.getName() != null) {
                        return jt.getName();
                    }
                    return left+ UNDERSCORE +right;
                }
                else {
                    return right+ UNDERSCORE +left;
                }
            }
        }
	}

	/**
	 * Evaluates the table name for the given property
	 *
	 * @param domainClass The domain class to evaluate
	 * @return The table name
	 */
	private static String getTableName(GrailsDomainClass domainClass) {
        Mapping m = getMapping(domainClass.getFullName());
        String tableName = null;
        if(m != null && m.getTableName() != null) {
            tableName = m.getTableName();
        }
        if(tableName == null) {
			tableName = namingStrategy.classToTableName(domainClass.getShortName());
        }
        return tableName;
	}
	/**
	 * Binds a Grails domain class to the Hibernate runtime meta model
	 * @param domainClass The domain class to bind
	 * @param mappings The existing mappings
	 * @throws MappingException Thrown if the domain class uses inheritance which is not supported
	 */
	public static void bindClass(GrailsDomainClass domainClass, Mappings mappings)
		throws MappingException {
		//if(domainClass.getClazz().getSuperclass() == java.lang.Object.class) {
		if(domainClass.isRoot()) {
            evaluateMapping(domainClass);
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
    private static void evaluateMapping(GrailsDomainClass domainClass) {
        Object o = domainClass.getPropertyValue(GrailsDomainClassProperty.MAPPING);
        if(o instanceof Closure) {
            HibernateMappingBuilder builder = new HibernateMappingBuilder(domainClass.getFullName());
            Mapping m = builder.evaluate((Closure)o);
            MAPPING_CACHE.put(domainClass.getFullName(), m);
        }
    }

    /**
     * Obtains a mapping object for the given domain class nam
     * @param domainClassName The domain class name in question
     * @return A Mapping object or null
     */
    public static Mapping getMapping(String domainClassName) {
        return (Mapping)MAPPING_CACHE.get(domainClassName);
    }

    /**
	 * Binds the specified persistant class to the runtime model based on the
	 * properties defined in the domain class
	 * @param domainClass The Grails domain class
	 * @param persistentClass The persistant class
	 * @param mappings Existing mappings
	 */
	private static void bindClass(GrailsDomainClass domainClass, PersistentClass persistentClass, Mappings mappings) {

		// set lazy loading for now
		persistentClass.setLazy(true);
		persistentClass.setEntityName(domainClass.getFullName());
		persistentClass.setProxyInterfaceName( domainClass.getFullName() );
		persistentClass.setClassName(domainClass.getFullName());

        // set dynamic insert to false
		persistentClass.setDynamicInsert(false);
		// set dynamic update to false
		persistentClass.setDynamicUpdate(false);
		// set select before update to false
		persistentClass.setSelectBeforeUpdate(false);

		// add import to mappings
		if ( mappings.isAutoImport() && persistentClass.getEntityName().indexOf( '.' ) > 0 ) {
			mappings.addImport( persistentClass.getEntityName(), StringHelper.unqualify( persistentClass
				.getEntityName() ) );
		}
	}


	/**
	 * Binds a root class (one with no super classes) to the runtime meta model
	 * based on the supplied Grails domain class
	 *
	 * @param domainClass The Grails domain class
	 * @param mappings The Hibernate Mappings object
	 */
	public static void bindRoot(GrailsDomainClass domainClass, Mappings mappings) {
		if(mappings.getClass(domainClass.getFullName()) == null) {
			RootClass root = new RootClass();
			bindClass(domainClass, root, mappings);

            Mapping m = getMapping(domainClass.getName());

            if(m!=null) {
                CacheConfig cc = m.getCache();
                if(cc != null && cc.getEnabled()) {
                    root.setCacheConcurrencyStrategy(cc.getUsage());
                    root.setLazyPropertiesCacheable(!"non-lazy".equals(cc.getInclude()));
                }
            }

            bindRootPersistentClassCommonValues(domainClass, root, mappings);

			if(!domainClass.getSubClasses().isEmpty()) {
                boolean tablePerHierarchy = m == null || m.getTablePerHierarchy();
                if(tablePerHierarchy) {
                    // if the root class has children create a discriminator property
                    bindDiscriminatorProperty(root.getTable(), root, mappings);
                }
				// bind the sub classes
				bindSubClasses(domainClass,root,mappings);
			}
			mappings.addClass(root);
		}
		else {
			LOG.info("[GrailsDomainBinder] Class ["+domainClass.getFullName()+"] is already mapped, skipping.. ");
		}
	}

	/**
	 * Binds the sub classes of a root class using table-per-heirarchy inheritance mapping
	 *
	 * @param domainClass The root domain class to bind
	 * @param parent The parent class instance
	 * @param mappings The mappings instance
	 */
	private static void bindSubClasses(GrailsDomainClass domainClass, PersistentClass parent, Mappings mappings) {
		Set subClasses = domainClass.getSubClasses();

		for (Iterator i = subClasses.iterator(); i.hasNext();) {
			GrailsDomainClass sub = (GrailsDomainClass) i.next();
            Set subSubs = sub.getSubClasses();
            if(sub.getClazz().getSuperclass().equals(domainClass.getClazz())) {
                bindSubClass(sub,parent,mappings);
            }
        }
	}

	/**
	 * Binds a sub class
	 *
	 * @param sub The sub domain class instance
     * @param parent The parent persistent class instance
     * @param mappings The mappings instance
     */
	private static void bindSubClass(GrailsDomainClass sub, PersistentClass parent, Mappings mappings) {

        Mapping m = getMapping(parent.getClassName());
        Subclass subClass;
        boolean tablePerSubclass = m != null && !m.getTablePerHierarchy();
        if(tablePerSubclass) {
            subClass = new JoinedSubclass(parent);
        }
        else {
            subClass = new SingleTableSubclass(parent);
            // set the descriminator value as the name of the class. This is the
            // value used by Hibernate to decide what the type of the class is
            // to perform polymorphic queries
            subClass.setDiscriminatorValue(sub.getFullName());

        }
        subClass.setEntityName(sub.getFullName());
        parent.addSubclass( subClass );
        mappings.addClass( subClass );

        if(tablePerSubclass)
            bindJoinedSubClass(sub, (JoinedSubclass)subClass, mappings, m);
        else
            bindSubClass(sub,subClass,mappings);


        if(!sub.getSubClasses().isEmpty()) {
            // bind the sub classes
            bindSubClasses(sub,subClass,mappings);
        }

    }

    /**
     * Binds a joined sub-class mapping using table-per-subclass
     * @param sub The Grails sub class
     * @param joinedSubclass The Hibernate Subclass object
     * @param mappings The mappings Object
     * @param gormMapping The GORM mapping object
     */
    private static void bindJoinedSubClass(GrailsDomainClass sub, JoinedSubclass joinedSubclass, Mappings mappings, Mapping gormMapping) {
        bindClass( sub, joinedSubclass, mappings );

        if ( joinedSubclass.getEntityPersisterClass() == null ) {
            joinedSubclass.getRootClass()
                .setEntityPersisterClass( JoinedSubclassEntityPersister.class );
        }

		Table mytable = mappings.addTable(
				mappings.getSchemaName(),
				mappings.getCatalogName(),
				getJoinedSubClassTableName( sub,joinedSubclass,  null, mappings ),
				null,
				false
			);


        joinedSubclass.setTable( mytable );
		LOG.info(
				"Mapping joined-subclass: " + joinedSubclass.getEntityName() +
				" -> " + joinedSubclass.getTable().getName()
			);





        SimpleValue key = new DependantValue( mytable, joinedSubclass.getIdentifier() );
		joinedSubclass.setKey( key );
        String columnName = namingStrategy.propertyToColumnName(sub.getIdentifier().getName());
        bindSimpleValue( sub.getIdentifier().getType().getName(), key, false, columnName, mappings );

        joinedSubclass.createPrimaryKey();

        // properties
		createClassProperties( sub, joinedSubclass, mappings);
    }

	private static String getJoinedSubClassTableName(
            GrailsDomainClass sub, PersistentClass model, Table denormalizedSuperTable,
            Mappings mappings
    ) {

		String logicalTableName = StringHelper.unqualify( model.getEntityName() );
		String physicalTableName = getTableName(sub);


		mappings.addTableBinding( mappings.getSchemaName(), mappings.getCatalogName(), logicalTableName, physicalTableName, denormalizedSuperTable );
		return physicalTableName;
	}

    /**
	 * Binds a sub-class using table-per-heirarchy in heritance mapping
	 *
	 * @param sub The Grails domain class instance representing the sub-class
	 * @param subClass The Hibernate SubClass instance
	 * @param mappings The mappings instance
	 */
	private static void bindSubClass(GrailsDomainClass sub, Subclass subClass, Mappings mappings) {
		bindClass( sub, subClass, mappings );

		if ( subClass.getEntityPersisterClass() == null ) {
			subClass.getRootClass()
					.setEntityPersisterClass( SingleTableEntityPersister.class );
		}

		if(LOG.isDebugEnabled())
			LOG.debug(
					"Mapping subclass: " + subClass.getEntityName() +
					" -> " + subClass.getTable().getName()
				);

		// properties
		createClassProperties( sub, subClass, mappings);
	}

	/**
	 * Creates and binds the discriminator property used in table-per-heirarchy inheritance to
	 * discriminate between sub class instances
	 *
	 * @param table The table to bind onto
	 * @param entity The root class entity
	 * @param mappings The mappings instance
	 */
	private static void bindDiscriminatorProperty(Table table, RootClass entity, Mappings mappings) {
		SimpleValue d = new SimpleValue( table );
		entity.setDiscriminator( d );
		entity.setDiscriminatorValue(entity.getClassName());
		bindSimpleValue(
                STRING_TYPE,
				d,
				false,
				RootClass.DEFAULT_DISCRIMINATOR_COLUMN_NAME,
				mappings
			);

		entity.setPolymorphic( true );
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
		String schema = mappings.getSchemaName();
		String catalog = mappings.getCatalogName();

		// create the table
		Table table = mappings.addTable(
				schema,
				catalog,
				getTableName(domainClass),
				null,
				false
		);
		root.setTable(table);

		if(LOG.isDebugEnabled())
			LOG.debug( "[GrailsDomainBinder] Mapping Grails domain class: " + domainClass.getFullName() + " -> " + root.getTable().getName() );

        Mapping m = getMapping(domainClass.getFullName());

        bindIdentity(domainClass, root, mappings, m);

        if(m != null) {
            if(m.getVersioned()) {
                bindVersion( domainClass.getVersion(), root, mappings );
            }
        }
        else
            bindVersion( domainClass.getVersion(), root, mappings );

		root.createPrimaryKey();

		createClassProperties(domainClass,root,mappings);
	}

    private static void bindIdentity(GrailsDomainClass domainClass, RootClass root, Mappings mappings, Mapping gormMapping) {
        if(gormMapping != null) {
            Object id = gormMapping.getIdentity();
            if(id instanceof CompositeIdentity){
                bindCompositeId(domainClass, root, (CompositeIdentity)id, gormMapping, mappings);
            }
            else {
                bindSimpleId( domainClass.getIdentifier(), root, mappings, (Identity)id );
            }

        }
        else {
            bindSimpleId( domainClass.getIdentifier(), root, mappings, null);
        }
    }

    private static void bindCompositeId(GrailsDomainClass domainClass, RootClass root, CompositeIdentity compositeIdentity, Mapping gormMapping, Mappings mappings) {
        Component id = new Component(root);
        root.setIdentifier(id);
        root.setEmbeddedIdentifier(true);
        id.setComponentClassName( domainClass.getFullName() );
        id.setKey(true);
        id.setEmbedded(true);

        String path = StringHelper.qualify(
                root.getEntityName(),
                "id");

        id.setRoleName(path);

        String[] props = compositeIdentity.getPropertyNames();
        for (int i = 0; i < props.length; i++) {
            String propName = props[i];
            GrailsDomainClassProperty property = domainClass.getPropertyByName(propName);
            if(property == null) throw new MappingException("Property ["+propName+"] referenced in composite-id mapping of class ["+domainClass.getFullName()+"] is not a valid property!");


            bindComponentProperty(id, property, root, "", root.getTable(), mappings);
        }
    }

    /**
	 * Creates and binds the properties for the specified Grails domain class and PersistantClass
	 * and binds them to the Hibernate runtime meta model
	 *
	 * @param domainClass The Grails domain class
	 * @param persistentClass The Hibernate PersistentClass instance
	 * @param mappings The Hibernate Mappings instance
	 */
	protected static void createClassProperties(GrailsDomainClass domainClass, PersistentClass persistentClass, Mappings mappings) {

		GrailsDomainClassProperty[] persistentProperties = domainClass.getPersistentProperties();
		Table table = persistentClass.getTable();

        Mapping gormMapping = getMapping(domainClass.getFullName());

        for(int i = 0; i < persistentProperties.length;i++) {

			GrailsDomainClassProperty currentGrailsProp = persistentProperties[i];
			// if its inherited skip
			if(currentGrailsProp.isInherited() && !isBidirectionalManyToOne(currentGrailsProp))
				continue;
            if (isCompositeIdProperty(gormMapping, currentGrailsProp)) continue;

            if(LOG.isDebugEnabled())
				LOG.debug("[GrailsDomainBinder] Binding persistent property [" + currentGrailsProp.getName() + "]");

			Value value;

			// see if its a collection type
			CollectionType collectionType = CollectionType.collectionTypeForClass( currentGrailsProp.getType() );
			if(collectionType != null) {
				// create collection
				Collection collection = collectionType.create(
						currentGrailsProp,
						persistentClass,
                        EMPTY_PATH, mappings
                );
				mappings.addCollection(collection);
				value = collection;
			}
			// work out what type of relationship it is and bind value
			else if ( currentGrailsProp.isManyToOne() ) {
				if(LOG.isDebugEnabled())
					LOG.debug("[GrailsDomainBinder] Binding property [" + currentGrailsProp.getName() + "] as ManyToOne");

				value = new ManyToOne( table );
                bindManyToOne( currentGrailsProp, (ManyToOne) value, EMPTY_PATH, mappings );
			}
			else if ( currentGrailsProp.isOneToOne() && !UserType.class.isAssignableFrom(currentGrailsProp.getType())) {
				if(LOG.isDebugEnabled())
					LOG.debug("[GrailsDomainBinder] Binding property [" + currentGrailsProp.getName() + "] as OneToOne");

				//value = new OneToOne( table, persistentClass );
				//bindOneToOne( currentGrailsProp, (OneToOne)value, mappings );
				value = new ManyToOne( table );
				bindManyToOne( currentGrailsProp, (ManyToOne) value, EMPTY_PATH, mappings );
			}
            else if ( currentGrailsProp.isEmbedded() ) {
                value = new Component( persistentClass );

                bindComponent((Component)value, currentGrailsProp, true, mappings);
            }
            else {
				if(LOG.isDebugEnabled())
					LOG.debug("[GrailsDomainBinder] Binding property [" + currentGrailsProp.getName() + "] as SimpleValue");

				value = new SimpleValue( table );
				bindSimpleValue( persistentProperties[i], (SimpleValue) value, EMPTY_PATH, mappings );
			}

			if(value != null) {
				Property property = createProperty( value, persistentClass, persistentProperties[i], mappings );
				persistentClass.addProperty( property );
			}
		}
	}

    private static boolean isCompositeIdProperty(Mapping gormMapping, GrailsDomainClassProperty currentGrailsProp) {
        if(gormMapping != null && gormMapping.getIdentity() != null) {
            Object id = gormMapping.getIdentity();
            if(id instanceof CompositeIdentity) {
                CompositeIdentity cid = (CompositeIdentity)id;
                if(ArrayUtils.contains(cid.getPropertyNames(), currentGrailsProp.getName()))
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
     * @param component The component to bind
     * @param property The property
     * @param isNullable Whether it is nullable or not
     * @param mappings The Hibernate Mappings object
     */
    private static void bindComponent(Component component, GrailsDomainClassProperty property, boolean isNullable, Mappings mappings) {
        component.setEmbedded(true);
        Class type = property.getType();
        String role = StringHelper.qualify(type.getName(), property.getName());
        component.setRoleName(role);
        component.setComponentClassName(type.getName());



        GrailsDomainClass domainClass = property.getReferencedDomainClass() != null ? property.getReferencedDomainClass() : new ComponentDomainClass(type);
        GrailsDomainClassProperty[] properties = domainClass.getPersistentProperties();
        Table table = component.getOwner().getTable();
        PersistentClass persistentClass = component.getOwner();
        String path = property.getName();
        Class propertyType = property.getDomainClass().getClazz();

        for (int i = 0; i < properties.length; i++) {
            GrailsDomainClassProperty currentGrailsProp = properties[i];
            if(currentGrailsProp.isIdentity()) continue;
            if(currentGrailsProp.getName().equals(GrailsDomainClassProperty.VERSION)) continue;

            if(currentGrailsProp.getType().equals(propertyType)) {
                component.setParentProperty(currentGrailsProp.getName());
                continue;
            }

            bindComponentProperty(component, currentGrailsProp, persistentClass, path, table, mappings);

        }

    }

    private static GrailsDomainClassProperty[] createDomainClassProperties(ComponentDomainClass type, PropertyDescriptor[] descriptors) {
        List properties = new ArrayList();
        for (int i = 0; i < descriptors.length; i++) {
            PropertyDescriptor descriptor = descriptors[i];
            if(GrailsDomainConfigurationUtil.isNotConfigurational(descriptor)) {
                properties.add(new DefaultGrailsDomainClassProperty(type,descriptor));
            }
        }
        return (GrailsDomainClassProperty[])properties.toArray(new GrailsDomainClassProperty[properties.size()]);
    }

    private static void bindComponentProperty(Component component, GrailsDomainClassProperty property, PersistentClass persistentClass, String path, Table table, Mappings mappings) {
        Value value = null;
        // see if its a collection type
        CollectionType collectionType = CollectionType.collectionTypeForClass( property.getType() );
        if(collectionType != null) {
            // create collection

            Collection collection = collectionType.create(
                    property,
                    persistentClass,
                    path,
                    mappings
            );
            mappings.addCollection(collection);
            value = collection;
        }
        // work out what type of relationship it is and bind value
        else if ( property.isManyToOne() ) {
            if(LOG.isDebugEnabled())
                LOG.debug("[GrailsDomainBinder] Binding property [" + property.getName() + "] as ManyToOne");

            value = new ManyToOne( table );
            bindManyToOne(property, (ManyToOne) value, path, mappings );
        }
        else if ( property.isOneToOne()) {
            if(LOG.isDebugEnabled())
                LOG.debug("[GrailsDomainBinder] Binding property [" + property.getName() + "] as OneToOne");

            //value = new OneToOne( table, persistentClass );
            //bindOneToOne( currentGrailsProp, (OneToOne)value, mappings );
            value = new ManyToOne( table );
            bindManyToOne(property, (ManyToOne) value, path, mappings );
        }
/*
        else if ( currentGrailsProp.isEmbedded() ) {
            value = new Component( persistentClass );

            bindComponent((Component)value, currentGrailsProp, true, mappings);
        }
*/
        else {
            if(LOG.isDebugEnabled())
                LOG.debug("[GrailsDomainBinder] Binding property [" + property.getName() + "] as SimpleValue");

            value = new SimpleValue( table );
            bindSimpleValue(property, (SimpleValue) value, path, mappings );
        }

        if(value != null) {
            Property persistentProperty = createProperty( value, persistentClass, property, mappings );
            component.addProperty( persistentProperty );
        }
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
		value.setTypeUsingReflection( persistentClass.getClassName(), grailsProperty.getName() );

		// if it is a ManyToOne or OneToOne relationship
		if ( value instanceof ToOne ) {
			ToOne toOne = (ToOne) value;
			String propertyRef = toOne.getReferencedPropertyName();
			if ( propertyRef != null ) {
				// TODO: Hmm this method has package visibility. Why?

				//mappings.addUniquePropertyReference( toOne.getReferencedEntityName(), propertyRef );
			}
		}
		else if( value instanceof Collection ) {
			//Collection collection = (Collection)value;
			//String propertyRef = collection.getReferencedPropertyName();
		}

		if(value.getTable() != null)
			value.createForeignKey();

		Property prop = new Property();
		prop.setValue( value );

		bindProperty( grailsProperty, prop, mappings );
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
		one.setReferencedEntityName( currentGrailsProp.getReferencedPropertyType().getName() );
	}

	/**
	 * Binds a many-to-one relationship to the
	 * @param property
     * @param manyToOne
     * @param path
     * @param mappings
     */
	private static void bindManyToOne(GrailsDomainClassProperty property, ManyToOne manyToOne, String path, Mappings mappings) {

		bindManyToOneValues(property, manyToOne);
		// bind column
		bindSimpleValue(property,manyToOne, path, mappings);

	}

	/**
	 * @param property
	 * @param manyToOne
	 */
	private static void bindManyToOneValues(GrailsDomainClassProperty property, ManyToOne manyToOne) {
		// TODO configure fetching
		manyToOne.setFetchMode(FetchMode.DEFAULT);
		// TODO configure lazy loading
        ColumnConfig cc = getColumnConfig(property);

        if(cc != null) {
           manyToOne.setLazy(cc.getLazy());
        }
        else {
            manyToOne.setLazy(true);
        }


		// set referenced entity
		manyToOne.setReferencedEntityName( property.getReferencedPropertyType().getName() );
		manyToOne.setIgnoreNotFound(true);
	}

	/**
	 * @param version
	 * @param mappings
	 */
	private static void bindVersion(GrailsDomainClassProperty version, RootClass entity, Mappings mappings) {

		SimpleValue val = new SimpleValue( entity.getTable() );
		bindSimpleValue( version, val, EMPTY_PATH, mappings);

		if ( !val.isTypeSpecified() ) {
			val.setTypeName( "version".equals( version.getName() ) ? "integer" : "timestamp" );
		}
		Property prop = new Property();
		prop.setValue( val );

		bindProperty( version, prop, mappings );
		val.setNullValue( "undefined" );
		entity.setVersion( prop );
		entity.addProperty( prop );
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
        entity.setIdentifier( id );

        if(mappedId != null) {
            id.setIdentifierGeneratorStrategy(mappedId.getGenerator());
            params.putAll(mappedId.getParams());
        }
        else {
            // configure generator strategy
            id.setIdentifierGeneratorStrategy( "native" );

        }

        if ( mappings.getSchemaName() != null ) {
			params.setProperty( PersistentIdentifierGenerator.SCHEMA, mappings.getSchemaName() );
		}
		if ( mappings.getCatalogName() != null ) {
			params.setProperty( PersistentIdentifierGenerator.CATALOG, mappings.getCatalogName() );
		}
		id.setIdentifierGeneratorProperties(params);

		// bind value
		bindSimpleValue(identifier, id, EMPTY_PATH, mappings );

		// create property
		Property prop = new Property();
		prop.setValue(id);

		// bind property
		bindProperty( identifier, prop, mappings );
		// set identifier property
		entity.setIdentifierProperty( prop );

		id.getTable().setIdentifierValue( id );

	}

	/**
     * Binds a property to Hibernate runtime meta model. Deals with cascade strategy based on the Grails domain model
     *
	 * @param grailsProperty The grails property instance
	 * @param prop The Hibernate property
	 * @param mappings The Hibernate mappings
	 */
	private static void bindProperty(GrailsDomainClassProperty grailsProperty, Property prop, Mappings mappings) {
		// set the property name
		prop.setName( grailsProperty.getName() );

		prop.setInsertable(true);
		prop.setUpdateable(true);
		prop.setPropertyAccessorName( mappings.getDefaultAccess() );
		prop.setOptional( grailsProperty.isOptional() );
        String cascadeStrategy = "none";
        // set to cascade all for the moment
        GrailsDomainClass domainClass = grailsProperty.getDomainClass();
        if(grailsProperty.isAssociation()) {
            GrailsDomainClass referenced = grailsProperty.getReferencedDomainClass();

            if(grailsProperty.isOneToOne()) {
                if(referenced!=null&&referenced.isOwningClass(domainClass.getClazz()))
                    cascadeStrategy = CASCADE_ALL;
            }
            else if(grailsProperty.isOneToMany()) {
                if(referenced!=null&&referenced.isOwningClass(domainClass.getClazz()))
                    cascadeStrategy = CASCADE_ALL;
                else
                    cascadeStrategy = CASCADE_SAVE_UPDATE;
            }
            else if(grailsProperty.isManyToMany()) {
                if(referenced!=null&&referenced.isOwningClass(domainClass.getClazz()))
                    cascadeStrategy = CASCADE_SAVE_UPDATE;
            }
            else if(grailsProperty.isManyToOne()) {
                if(referenced!=null&&referenced.isOwningClass(domainClass.getClazz()))
                    cascadeStrategy = CASCADE_ALL;
                else
                    cascadeStrategy = CASCADE_MERGE;
            }

            logCascadeMapping(grailsProperty, cascadeStrategy, referenced);
            prop.setCascade(cascadeStrategy);
        }
        else if( Map.class.isAssignableFrom(grailsProperty.getType())) {
            GrailsDomainClass referenced = grailsProperty.getReferencedDomainClass();
            if(referenced!=null&&referenced.isOwningClass(grailsProperty.getDomainClass().getClazz())) {
                cascadeStrategy = CASCADE_ALL;
            }
            else {
                cascadeStrategy = CASCADE_SAVE_UPDATE;
            }
            logCascadeMapping(grailsProperty, cascadeStrategy, referenced);
            prop.setCascade(cascadeStrategy);

        }


        // lazy to true
		prop.setLazy(true);

	}

    private static void logCascadeMapping(GrailsDomainClassProperty grailsProperty, String cascadeStrategy, GrailsDomainClass referenced) {
        if(LOG.isDebugEnabled()) {
            String assType = getAssociationDescription(grailsProperty);
            LOG.debug("Mapping cascade strategy for "+assType+" property "+grailsProperty.getDomainClass().getFullName()+"." + grailsProperty.getName() + " referencing type ["+referenced.getClazz()+"] -> [CASCADE: "+cascadeStrategy+"]");
        }
    }

    private static String getAssociationDescription(GrailsDomainClassProperty grailsProperty) {
        String assType = "unknown";
        if(grailsProperty.isManyToMany()) {
            assType = "many-to-many";
        }
        else if(grailsProperty.isOneToMany()) {
            assType = "one-to-many";
        }
        else if(grailsProperty.isOneToOne()) {
            assType = "one-to-one";
        }
        else if(grailsProperty.isManyToOne()) {
            assType = "many-to-one";
        }
        else if(grailsProperty.isEmbedded()) {
            assType = "embedded";
        }
        return assType;
    }

    /**
w	 * Binds a simple value to the Hibernate metamodel. A simple value is
	 * any type within the Hibernate type system
	 *
	 * @param grailsProp The grails domain class property
     * @param simpleValue The simple value to bind
     * @param path
     * @param mappings The Hibernate mappings instance
     */
	private static void bindSimpleValue(GrailsDomainClassProperty grailsProp, SimpleValue simpleValue, String path, Mappings mappings) {
		// set type
        ColumnConfig cc = getColumnConfig(grailsProp);
        setTypeForColumnConfig(grailsProp, simpleValue, cc);
        Table table = simpleValue.getTable();
		Column column = new Column();

		// Check for explicitly mapped column name.
		if (cc != null && cc.getColumn() != null) {
			column.setName(cc.getColumn());
		}

		column.setValue(simpleValue);
		bindColumn(grailsProp, column, path, table);

		if(table != null) table.addColumn(column);

		simpleValue.addColumn(column);
	}

    private static void setTypeForColumnConfig(GrailsDomainClassProperty grailsProp, SimpleValue simpleValue, ColumnConfig cc) {
        if(cc != null && cc.getType() != null) {
            Object type = cc.getType();
            simpleValue.setTypeName(type.toString());
        }
        else {
            simpleValue.setTypeName(grailsProp.getType().getName());
        }
    }

    /**
	 * Binds a value for the specified parameters to the meta model.
	 *
	 * @param type The type of the property
	 * @param simpleValue  The simple value instance
	 * @param nullable Whether it is nullable
	 * @param columnName The property name
	 * @param mappings The mappings
	 */
	private static void bindSimpleValue(String type,SimpleValue simpleValue, boolean nullable, String columnName, Mappings mappings) {

        simpleValue.setTypeName(type);
        Table t = simpleValue.getTable();
		Column column = new Column();
		column.setNullable(nullable);
		column.setValue(simpleValue);
		column.setName(columnName);
		if(t!=null)t.addColumn(column);

		simpleValue.addColumn(column);
	}

	/**
	 * Binds a Column instance to the Hibernate meta model
	 * @param grailsProp The Grails domain class property
     * @param column The column to bind
     * @param path
     * @param table The table name
     */
	private static void bindColumn(GrailsDomainClassProperty grailsProp, Column column, String path, Table table) {
		if(grailsProp.isAssociation()) {
			// Only use conventional naming when the column has not been explicitly mapped.
			if (column.getName() == null) {
                String columnName = getColumnNameForPropertyAndPath(grailsProp, path);
                if(!grailsProp.isBidirectional() && grailsProp.isOneToMany()) {
                    String prefix = namingStrategy.classToTableName(grailsProp.getDomainClass().getName());
                    column.setName(prefix+ UNDERSCORE +columnName + FOREIGN_KEY_SUFFIX);
                } else {

                    if(grailsProp.isInherited() && isBidirectionalManyToOne(grailsProp)) {
                        column.setName( namingStrategy.propertyToColumnName(grailsProp.getDomainClass().getName()) + '_'+ columnName + FOREIGN_KEY_SUFFIX );
                    }
                    else {
                        column.setName( columnName + FOREIGN_KEY_SUFFIX );
                    }

                }
            }
            column.setNullable(true);
		} else {
            String columnName = getColumnNameForPropertyAndPath(grailsProp, path);
            column.setName(columnName);
			column.setNullable(grailsProp.isOptional());

            // Use the constraints for this property to more accurately define
            // the column's length, precision, and scale
            ConstrainedProperty constrainedProperty = getConstrainedProperty(grailsProp);
            if (constrainedProperty != null) {
                if (String.class.isAssignableFrom(grailsProp.getType()) || byte[].class.isAssignableFrom(grailsProp.getType())) {
                    bindStringColumnConstraints(column, constrainedProperty);
                }

                if (Number.class.isAssignableFrom(grailsProp.getType())) {
                    bindNumericColumnConstraints(column, constrainedProperty);
                }
            }
		}

        bindIndex(grailsProp, column, table);

        if(!grailsProp.getDomainClass().isRoot()) {
			if(LOG.isDebugEnabled())
				LOG.debug("[GrailsDomainBinder] Sub class property [" + grailsProp.getName() + "] for column name ["+column.getName()+"] in table ["+table.getName()+"] set to nullable");
			column.setNullable(true);
		}

		if(LOG.isDebugEnabled())
			LOG.debug("[GrailsDomainBinder] bound property [" + grailsProp.getName() + "] to column name ["+column.getName()+"] in table ["+table.getName()+"]");
	}

    private static void bindIndex(GrailsDomainClassProperty grailsProp, Column column, Table table) {
        ColumnConfig cc = getColumnConfig(grailsProp);
        if(cc!=null) {
            String indexDefinition = cc.getIndex();
            if(indexDefinition != null) {
                String[] tokens = indexDefinition.split(",");
                for (int i = 0; i < tokens.length; i++) {
                    String index = tokens[i];
                    table.getOrCreateIndex(index).addColumn(column);
                }
            }
        }
    }

    private static String getColumnNameForPropertyAndPath(GrailsDomainClassProperty grailsProp, String path) {
        GrailsDomainClass domainClass = grailsProp.getDomainClass();

        String columnName = null;
        Mapping m = getMapping(domainClass.getFullName());
        if(m != null) {
            ColumnConfig c = m.getColumn(grailsProp.getName());
            if(c != null && c.getColumn() != null) {
                columnName = c.getColumn();
            }
        }
        if(columnName == null) {
            if(StringHelper.isNotEmpty(path)) {
                columnName = namingStrategy.propertyToColumnName(path) + UNDERSCORE +  namingStrategy.propertyToColumnName(grailsProp.getName());
            }
            else {
                columnName = namingStrategy.propertyToColumnName(grailsProp.getName());
            }
        }
        return columnName;
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
            ConstrainedProperty tmpConstrainedProperty = (ConstrainedProperty)constrainedPropertyIter.next();
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
     * @param column the column that corresponds to the property
     * @param constrainedProperty the property's constraints
     */
    protected static void bindStringColumnConstraints(Column column, ConstrainedProperty constrainedProperty) {
        Integer columnLength = constrainedProperty.getMaxSize();
        List inListValues = constrainedProperty.getInList();
        if (columnLength != null) {
            column.setLength(columnLength.intValue());
        }
        else if (inListValues != null) {
            column.setLength(getMaxSize(inListValues));
        }
    }

    /**
     * Interrogates the specified constraints looking for any constraints that would limit the
     * precision and/or scale of the property's value.  If such constraints exist, this method adjusts
     * the precision and/or scale of the column accordingly.
     *
     * @param column the column that corresponds to the property
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
			precision = NumberUtils.max(new int[] { minConstraintValueLength, maxConstraintValueLength });
		} else {
			// Overwise we should also use default precision
			precision = NumberUtils.max(new int[] { precision, minConstraintValueLength, maxConstraintValueLength });
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
            String value = (String)iter.next();
            maxSize = Math.max(value.length(), maxSize);
        }

        return maxSize;
    }

    private static class ComponentDomainClass extends AbstractGrailsClass implements GrailsDomainClass {
        private GrailsDomainClassProperty[] properties;


        public ComponentDomainClass(Class type) {
            super(type, "");

            PropertyDescriptor[] descriptors;
            try {
                descriptors = java.beans.Introspector.getBeanInfo(type).getPropertyDescriptors();
            } catch (IntrospectionException e) {
                throw new MappingException("Failed to use class ["+type+"] as a component. Cannot introspect! " + e.getMessage());
            }

            this.properties = createDomainClassProperties(this,descriptors);
        }

        public boolean isOwningClass(Class domainClass) {
            return false;
        }

        public GrailsDomainClassProperty[] getProperties() {
            return properties;
        }

        public GrailsDomainClassProperty[] getPersistantProperties() {
            return properties;
        }

        public GrailsDomainClassProperty[] getPersistentProperties() {
            return properties;
        }

        public GrailsDomainClassProperty getIdentifier() {
            return null;  // no identifier for embedded component
        }

        public GrailsDomainClassProperty getVersion() {
            return null;  // no version for embedded component
        }

        public Map getAssociationMap() {
            return Collections.EMPTY_MAP;
        }

        public GrailsDomainClassProperty getPropertyByName(String name) {
            for (int i = 0; i < properties.length; i++) {
                GrailsDomainClassProperty property = properties[i];
                if(property.getName().equals(name)) return property;
            }
            return null;
        }

        public String getFieldName(String propertyName) {
            return null;
        }


        public boolean isOneToMany(String propertyName) {
            return false;
        }

        public boolean isManyToOne(String propertyName) {
            return false;
        }

        public boolean isBidirectional(String propertyName) {
            return false;
        }

        public Class getRelatedClassType(String propertyName) {
            return getPropertyByName(propertyName).getReferencedPropertyType();
        }

        public Map getConstrainedProperties() {
            return Collections.EMPTY_MAP;
        }

        public Validator getValidator() {
            return null;
        }

        public void setValidator(Validator validator) {
        }

        public String getMappingStrategy() {
            return GrailsDomainClass.GORM;
        }

        public boolean isRoot() {
            return true;
        }

        public Set getSubClasses() {
            return Collections.EMPTY_SET;
        }

        public void refreshConstraints() {
            // do nothing
        }

        public boolean hasSubClasses() {
            return false;
        }

        public Map getMappedBy() {
            return Collections.EMPTY_MAP;
        }
    }
}
