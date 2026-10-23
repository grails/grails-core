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

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitIndexNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;

/**
 * Gives every collection with a join table a uniqueness constraint of its own over its key column and the element,
 * so that a join table several collections of one owner share stays unique per collection.
 *
 * <p>Two unidirectional {@code hasMany} to the same class without a {@code joinTable} mapping share one join table,
 * with a key column for each of them and one element column. Hibernate creates the table's implicit unique key when
 * it creates the keys of the first collection, over that collection's key column and the element column, and creates
 * none for the others, as the table has a unique key already ({@code Set.createPrimaryKey}). This binder runs after
 * the keys of each collection are created and completes what is missing, in the shape the database needs
 * ({@link SharedJoinTableUniqueness}). A collection whose table is its own already has its constraint and is left as
 * Hibernate bound it, and so is a collection without a join table or whose key the other side owns.</p>
 *
 * @since 8.0.1
 */
public class SharedJoinTableUniqueKeyBinder {

    private final MetadataBuildingContext metadataBuildingContext;
    private final SharedJoinTableUniqueness uniqueness;

    /** Resolves the uniqueness for the dialect of the metadata being built. */
    public SharedJoinTableUniqueKeyBinder(MetadataBuildingContext metadataBuildingContext) {
        this(metadataBuildingContext, metadataBuildingContext.getMetadataCollector().getDatabase().getDialect());
    }

    SharedJoinTableUniqueKeyBinder(MetadataBuildingContext metadataBuildingContext, Dialect dialect) {
        this.metadataBuildingContext = metadataBuildingContext;
        this.uniqueness = SharedJoinTableUniqueness.forDialect(dialect);
    }

    /** The uniqueness this binder applies. */
    public SharedJoinTableUniqueness getUniqueness() {
        return uniqueness;
    }

    /** Gives the collection a uniqueness constraint over its key columns and its element on its join table. */
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
        uniqueness.apply(table, keyColumns, elementColumns, new ImplicitNaming(table));
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

    /** Names constraints and indexes with the implicit naming strategy, as Hibernate names the ones it creates. */
    private final class ImplicitNaming implements SharedJoinTableUniqueness.Naming {

        private final Table table;

        private ImplicitNaming(Table table) {
            this.table = table;
        }

        @Override
        public String uniqueKeyName(List<Column> columns) {
            return strategy().determineUniqueKeyName(new ImplicitUniqueKeyNameSource() {
                @Override
                public Identifier getTableName() {
                    return table.getNameIdentifier();
                }

                @Override
                public List<Identifier> getColumnNames() {
                    return identifiers(columns);
                }

                @Override
                public Identifier getUserProvidedIdentifier() {
                    return null;
                }

                @Override
                public MetadataBuildingContext getBuildingContext() {
                    return metadataBuildingContext;
                }
            }).render(dialect());
        }

        @Override
        public String indexName(List<Column> columns) {
            return strategy().determineIndexName(new ImplicitIndexNameSource() {
                @Override
                public Identifier getTableName() {
                    return table.getNameIdentifier();
                }

                @Override
                public List<Identifier> getColumnNames() {
                    return identifiers(columns);
                }

                @Override
                public Identifier getUserProvidedIdentifier() {
                    return null;
                }

                @Override
                public MetadataBuildingContext getBuildingContext() {
                    return metadataBuildingContext;
                }
            }).render(dialect());
        }

        private List<Identifier> identifiers(List<Column> columns) {
            List<Identifier> names = new ArrayList<>();
            for (Column column : columns) {
                names.add(column.getNameIdentifier(metadataBuildingContext));
            }
            return names;
        }

        private ImplicitNamingStrategy strategy() {
            return metadataBuildingContext.getBuildingOptions().getImplicitNamingStrategy();
        }

        private Dialect dialect() {
            return metadataBuildingContext.getMetadataCollector().getDatabase().getDialect();
        }
    }
}
