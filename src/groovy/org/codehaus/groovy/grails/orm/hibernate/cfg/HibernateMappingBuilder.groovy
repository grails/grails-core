/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.orm.hibernate.cfg
/**
 * A builder that implements the ORM mapping DSL constructing a model that can be evaluated by the
 * GrailsDomainBinder class which maps GORM classes onto the database

 * @author Graeme Rocher
 * @since 1.0
  *
 * Created: Sep 26, 2007
 * Time: 2:24:45 PM
 *
 */

import org.apache.commons.logging.LogFactory

class HibernateMappingBuilder {

    static final LOG = LogFactory.getLog(HibernateMappingBuilder.class)

    Mapping mapping
    String className

    /**
     * Constructor for builder
     *
     * @param className The name of the class being mapped
     *
     */
    HibernateMappingBuilder(String className) {
        this.className = className
    }

    /**
     * Central entry point for the class. Passing a closure that defines a set of mappings will evaluate said mappings
     * and populate the "mapping" property of this class which can then be obtained with getMappings()
     *
     * @param mappingClosure The closure that defines the ORM DSL 
     */
    Mapping evaluate(Closure mappingClosure) {
        mapping = new Mapping()
        mappingClosure.resolveStrategy = Closure.DELEGATE_ONLY
        mappingClosure.delegate = this
        mappingClosure.call()
        mapping
    }

    /**
    * <p>Configures the table name. Example:
    * <code> { table 'foo' }
    *
    * @param name The name of the table
    */
    void table(String name) {
        mapping.tableName = name
    }

    /**
     * Set whether auto time stamping should occur for last_updated and date_created columns 
     */
    void autoTimestamp(boolean b) {
        mapping.autoTimestamp = b
    }

    /**
    * <p>Configures whether to use versioning for optimistic locking
    * <code> { version false }
    *
    * @param isVersioned True if a version property should be configured
    */
    void version(boolean isVersioned) {
        mapping.versioned = isVersioned 
    }

    /**
    * <p>Configures the second-level cache for the class
    * <code> { cache usage:'read-only', include:'all' }
    *
    * @param args Named arguments that contain the "usage" and/or "include" parameters
    */
    void cache(Map args) {
        mapping.cache = new CacheConfig(enabled:true)
        if(args.usage) {
            if(CacheConfig.USAGE_OPTIONS.contains(args.usage))
                mapping.cache.usage = args.usage
            else
                LOG.warn("ORM Mapping Invalid: Specified [usage] with value [$args.usage] of [cache] in class [$className] is not valid")
        }
        if(args.include) {
            if(CacheConfig.INCLUDE_OPTIONS.contains(args.include))
                mapping.cache.include = args.include
            else
                LOG.warn("ORM Mapping Invalid: Specified [include] with value [$args.include] of [cache] in class [$className] is not valid")            
        }
    }

    /**
    * <p>Configures the second-level cache for the class
    * <code> { cache 'read-only' }
    *
    * @param usage The usage type for the cache which is one of CacheConfig.USAGE_OPTIONS
    */
    void cache(String usage) {
        cache(usage:usage)
    }
    /**
    * <p>Configures the second-level cache for the class
    * <code> { cache 'read-only', include:'all }
    *
    * @param usage The usage type for the cache which is one of CacheConfig.USAGE_OPTIONS
    */
    void cache(String usage, Map args) {
        args = args ? args : [:]
        args.usage = usage
        cache(args)        
    }

    /**
     * If true the class and its sub classes will be mapped with table per hierarchy mapping
     */
    void tablePerHierarchy(boolean isTablePerHierarchy) {
        mapping.tablePerHierarchy = isTablePerHierarchy
    }

    /**
     * If true the class and its subclasses will be mapped with table per subclass mapping
     */
    void tablePerSubclass(boolean isTablePerSubClass) {
        mapping.tablePerHierarchy = !isTablePerSubClass
    }

    /**
    * <p>Configures the second-level cache with the default usage of 'read-write' and the default include of 'all' if
    *  the passed argument is true

    * <code> { cache true }
    *
    * @param shouldCache True if the default cache configuration should be applied
    */
    void cache(boolean shouldCache) {
        mapping.cache = new CacheConfig(enabled:shouldCache)
    }

   /**
    * <p>Configures the identity strategy for the mapping. Examples

    * <code>
    *    { id generator:'sequence' }
    *    { id composite: ['one', 'two'] }
    * </code>
    *
    * @param args The named arguments to the id method
    */
    void id(Map args) {
        if(args.composite) {
            mapping.identity = new CompositeIdentity(propertyNames:args.composite as String[])
            if(args.compositeClass) {
                mapping.identity.compositeClass = args.compositeClass
            }
        }
        else {
            if(args?.generator) {
                mapping.identity.generator = args.remove('generator')
            }
            if(args?.params) {
                def params = args.remove('params')
                for(entry in params) {
                    params[entry.key] = entry.value?.toString()
                }
                mapping.identity.params = params
            }
            // still more arguments?
            if(args) {
                handleMethodMissing("id", [args] as Object[])
            }
        }

    }

    /**
     * A closure used by methodMissing to create column definitions
     */
    private handleMethodMissing = { String name, args ->
        if(args && args[0] instanceof Map) {
            def namedArgs = args[0]
            def column = new ColumnConfig()
            column.column = namedArgs.column
            column.type = namedArgs.type
            column.index = namedArgs.index
            column.lazy = namedArgs.lazy ? true : false
            column.unique = namedArgs.unique ? true : false
            column.length = namedArgs.length ? namedArgs.length : -1
            column.precision = namedArgs.precision ? namedArgs.precision : -1
            column.scale = namedArgs.scale ? namedArgs.scale : -1
            column.cascade = namedArgs.cascade ? namedArgs.cascade : null

            if(namedArgs.cache instanceof String) {
                CacheConfig cc = new CacheConfig()
                if(CacheConfig.USAGE_OPTIONS.contains(namedArgs.cache))
                    cc.usage = namedArgs.cache
                else
                    LOG.warn("ORM Mapping Invalid: Specified [usage] of [cache] with value [$args.usage] for association [$name] in class [$className] is not valid")
                column.cache = cc
            }
            else if(namedArgs.cache == true) {
                column.cache = new CacheConfig()
            }
            else if(namedArgs.cache instanceof Map) {
                def cacheArgs = namedArgs.cache
                CacheConfig cc = new CacheConfig()
                if(CacheConfig.USAGE_OPTIONS.contains(cacheArgs.usage))
                    cc.usage = cacheArgs.usage
                else
                    LOG.warn("ORM Mapping Invalid: Specified [usage] of [cache] with value [$args.usage] for association [$name] in class [$className] is not valid")

                if(CacheConfig.INCLUDE_OPTIONS.contains(cacheArgs.include))
                    cc.include = cacheArgs.include
                else
                    LOG.warn("ORM Mapping Invalid: Specified [include] of [cache] with value [$args.include] for association [$name] in class [$className] is not valid")

                column.cache = cc

            }

            if(namedArgs.joinTable) {
                def join = new JoinTable()
                def joinArgs = namedArgs.joinTable
                if(joinArgs instanceof String) {
                    join.name = joinArgs
                }
                else if(joinArgs instanceof Map) {
                    if(joinArgs.name) join.name = joinArgs.name
                    if(joinArgs.key) join.key = joinArgs.key
                    if(joinArgs.column) join.column = joinArgs.column
                }
                column.joinTable = join
            }
            else if(namedArgs.containsKey('joinTable') && namedArgs.joinTable == false) {
                column.joinTable = null
            }

            mapping.columns[name] = column
        }

    }

    /**
     * <p>Consumes the columns closure and populates the value into the Mapping objects columns property
     *
     * @param callable The closure containing the column definitions
     */
    void columns(Closure callable) {
        callable.resolveStrategy = Closure.DELEGATE_ONLY
        callable.delegate = [invokeMethod:handleMethodMissing] as GroovyObjectSupport
        callable.call()
    }

    void methodMissing(String name, args) {
        if(args && args[0] instanceof Map) {
            handleMethodMissing(name, args)
        }
        else {
            LOG.warn "ORM Mapping Invalid: Specified config option [$name] does not exist for class [$className]!"
        }
    }
}
