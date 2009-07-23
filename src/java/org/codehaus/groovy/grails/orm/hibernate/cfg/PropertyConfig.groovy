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

import org.hibernate.FetchMode


/**
 * Custom mapping for a single domain property. Note that a property
 * can have multiple columns via a component or a user type.
 *
 * @since 1.0.4
 * @author pledbrook
 */
class PropertyConfig {
    /**
     * The Hibernate type or user type of the property. This can be
     * a string or a class.
     */
    def type

    /**
     * The default sort order 
     */
    String sort

    /**
     * The batch size used for lazy loading
     */
    Integer batchSize

    /**
     * Cascading strategy for this property. Only makes sense if the
     * property is an association or collection.
     */
    String cascade
    /**
     * The fetch strategy for this property. 
     */
    FetchMode fetch = FetchMode.DEFAULT

    /**
     * Whether to ignore ObjectNotFoundException
     */
    boolean ignoreNotFound = false

    /**
     *
     */
    List<ColumnConfig> columns = []

    boolean lazy = true
    CacheConfig cache
    JoinTable joinTable = new JoinTable()

    /**
     * The column used to produce the index for index based collections (lists and maps)
     */
    PropertyConfig indexColumn

    /**
     * Shortcut to get the column name for this property.
     * @throws RuntimeException if this property maps to more than one
     * column.
     */
    String getColumn() {
        checkHasSingleColumn()
        return columns[0].name
    }

    String getEnumType() {
      checkHasSingleColumn()
        return columns[0].enumType
    }

    /**
     * Shortcut to get the SQL type of the corresponding column.
     * @throws RuntimeException if this property maps to more than one
     * column.
     */
    String getSqlType() {
        checkHasSingleColumn()
        return columns[0].sqlType
    }

    /**
     * Shortcut to get the index setting for this property's column.
     * @throws RuntimeException if this property maps to more than one
     * column.
     */
    String getIndex() {
        checkHasSingleColumn()
        return columns[0].index
    }

    /**
     * Shortcut to determine whether the property's column is configured
     * to be unique.
     * @throws RuntimeException if this property maps to more than one
     * column.
     */
    boolean isUnique() {
        checkHasSingleColumn()
        return columns[0].unique
    }

    /**
     * Shortcut to get the length of this property's column.
     * @throws RuntimeException if this property maps to more than one
     * column.
     */
    int getLength() {
        checkHasSingleColumn()
        return columns[0].length
    }

    /**
     * Shortcut to get the precision of this property's column.
     * @throws RuntimeException if this property maps to more than one
     * column.
     */
    int getPrecision() {
        checkHasSingleColumn()
        return columns[0].precision
    }

    /**
     * Shortcut to get the scale of this property's column.
     * @throws RuntimeException if this property maps to more than one
     * column.
     */
    int getScale() {
        checkHasSingleColumn()
        return columns[0].scale
    }

    String toString() {
        "property[type:$type, lazy:$lazy, columns:$columns]"
    }

    private void checkHasSingleColumn() {
        if (columns?.size() > 1) {
            throw new RuntimeException("Cannot treat multi-column property as a single-column property")
        }
    }



}
