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
 * A class that models the mapping from GORM classes to the db 
 * @author Graeme Rocher
 * @since 1.0
  *
 * Created: Sep 26, 2007
 * Time: 2:26:57 PM
 *
 */
class Mapping {

    /**
     * The table name
     */
    String tableName
    /**
     * Whether the class is versioned for optimistic locking
     */
    boolean versioned = true
    Map columns = [:]
    /**
     * The identity definition
     */
    Identity identity = new Identity()
    /**
     * Caching config
     */
    CacheConfig cache

    /**
     * Obtains a ColumnConfig object for the given name
     */
    ColumnConfig getColumn(String name) { columns[name] }
}
/**
 * <p>Defines the identity generation strategy. In the case of a 'composite' identity the properties
 * array defines the property names that formulate the composite id
 */
class Identity {
    String generator = 'native'
    String column = 'id'
    Class type = Long
    Map params = [:]

    String toString() { "id[generator:$generator, column:$id, type:$type]" }

}
class CompositeIdentity extends Identity {
    String[] propertyNames    
}
/**
 * <p> A class that defines a column within the mapping
 */
class ColumnConfig {
    String name
    Class type
    String index
    boolean lazy = false
    boolean unique = false
    int length = -1
    int precision = -1
    int scale = -1

    String toString() {
        "column[name:$name, type:$type, index:$index, lazy:$lazy, unique:$unique, length:$length, precision:$precision, scale:$scale]"
    }
}
/**
 * <p> A class that defines the cache configuration
 */
class CacheConfig {
    static final USAGE_OPTIONS = ['read-only', 'read-write','nonstrict-read-write','transactional']
    static final INCLUDE_OPTIONS = ['all', 'non-lazy']
    
    String usage = "read-write"
    boolean enabled = false
    String include = "all"
}