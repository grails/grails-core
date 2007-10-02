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

    /**
     * Sets whether to use table-per-hierarchy or table-per-subclass mapping
     */
    boolean tablePerHierarchy = true

    /**
     * Sets whether automatic timestamping should occur for columns like last_updated and date_created
     */
    boolean autoTimestamp = true

    Map columns = [:]
    /**
     * The identity definition
     */
    def identity = new Identity()
    /**
     * Caching config
     */
    CacheConfig cache

    /**
     * Obtains a ColumnConfig object for the given name
     */
    ColumnConfig getColumn(String name) { columns[name] }
}

