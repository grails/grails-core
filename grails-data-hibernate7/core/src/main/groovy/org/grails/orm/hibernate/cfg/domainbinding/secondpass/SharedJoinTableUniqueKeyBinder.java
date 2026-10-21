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
package org.grails.orm.hibernate.cfg.domainbinding.secondpass;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

/**
 * Completes the implicit unique key of a join table that several collections of one owner share.
 *
 * <p>Two unidirectional {@code hasMany} to the same class without a {@code joinTable} mapping share one join table, with a
 * key column for each of them and one element column. Hibernate creates the table's unique key when it creates the keys of
 * the first collection, over that collection's key column and the element column, and creates none for the others, as the
 * table has a unique key already ({@code Set.createPrimaryKey}). A row of a later collection leaves the first key column
 * null, so on a database that treats nulls as equal in a unique constraint two of its rows for the same element, from two
 * owners, are duplicates. This binder adds the key columns of every collection to that unique key once the collection's
 * keys are created, so a row is unique by the key column of its own collection and the element.</p>
 *
 * <p>A unique key the mapping declares explicitly is left alone, and so is a collection without a join table or whose
 * key the other side owns.</p>
 *
 * @since 8.0.1
 */
public class SharedJoinTableUniqueKeyBinder {

    /** Adds the key columns of the collection to the implicit unique key of its join table that covers its element. */
    public void bind(Collection collection) {
        Table table = collection.getCollectionTable();
        if (table == null || collection.isInverse() || collection.isOneToMany() || collection.getKey() == null) {
            return;
        }
        List<Column> keyColumns = collection.getKey().getColumns();
        List<Column> elementColumns = columnsOf(collection);
        if (keyColumns.isEmpty() || elementColumns.isEmpty()) {
            return;
        }
        for (UniqueKey uniqueKey : table.getUniqueKeys().values()) {
            if (uniqueKey.isExplicit() || !elementColumns.stream().allMatch(uniqueKey::containsColumn)) {
                continue;
            }
            for (Column column : keyColumns) {
                if (!uniqueKey.containsColumn(column)) {
                    uniqueKey.addColumn(column);
                }
            }
        }
    }

    private static List<Column> columnsOf(Collection collection) {
        List<Column> columns = new ArrayList<>();
        if (collection.getElement() != null) {
            for (Selectable selectable : collection.getElement().getSelectables()) {
                if (selectable instanceof Column column) {
                    columns.add(column);
                }
            }
        }
        return columns;
    }

}
