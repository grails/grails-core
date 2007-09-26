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
        }
        else {
            if(args.generator) {
                mapping.identity.generator = args.generator
            }
            if(args.params) {
                mapping.identity.params = args.params
            }
        }

    }

    /**
     * <p>Consumes the columns closure and populates the value into the Mapping objects columns property
     *
     * @param callable The closure containing the column definitions
     */
    void columns(Closure callable) {
        callable.resolveStrategy = Closure.DELEGATE_ONLY
        callable.delegate = [invokeMethod:{String name, args ->
            if(args && args[0] instanceof Map) {
                def namedArgs = args[0]
                def column = new ColumnConfig()
                column.name = namedArgs.name
                column.type = namedArgs.type
                column.index = namedArgs.index
                column.lazy = namedArgs.lazy ? true : false
                column.unique = namedArgs.unique ? true : false
                column.length = namedArgs.length ? namedArgs.length : -1
                column.precision = namedArgs.precision ? namedArgs.precision : -1
                column.scale = namedArgs.scale ? namedArgs.scale : -1

                mapping.columns[name] = column
            }

        }] as GroovyObjectSupport
        callable.call()
    }
}
