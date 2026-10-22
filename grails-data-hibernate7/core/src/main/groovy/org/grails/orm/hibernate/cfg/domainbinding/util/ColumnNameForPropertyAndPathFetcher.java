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
package org.grails.orm.hibernate.cfg.domainbinding.util;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.grails.orm.hibernate.cfg.ColumnConfig;
import org.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.grails.orm.hibernate.cfg.PersistentEntityNamingStrategy;
import org.grails.orm.hibernate.cfg.domainbinding.hibernate.HibernatePersistentProperty;

public class ColumnNameForPropertyAndPathFetcher {

    private static final String UNDERSCORE = "_";
    private final PersistentEntityNamingStrategy namingStrategy;
    private final DefaultColumnNameFetcher defaultColumnNameFetcher;
    private final BackticksRemover backticksRemover;
    private final CompositeUserTypeColumns compositeUserTypeColumns;

    public ColumnNameForPropertyAndPathFetcher(
            PersistentEntityNamingStrategy namingStrategy,
            DefaultColumnNameFetcher defaultColumnNameFetcher,
            BackticksRemover backticksRemover) {
        this(namingStrategy, defaultColumnNameFetcher, backticksRemover,
                new CompositeUserTypeColumns(namingStrategy, backticksRemover));
    }

    protected ColumnNameForPropertyAndPathFetcher(
            PersistentEntityNamingStrategy namingStrategy,
            DefaultColumnNameFetcher defaultColumnNameFetcher,
            BackticksRemover backticksRemover,
            CompositeUserTypeColumns compositeUserTypeColumns) {
        this.namingStrategy = namingStrategy;
        this.defaultColumnNameFetcher = defaultColumnNameFetcher;
        this.backticksRemover = backticksRemover;
        this.compositeUserTypeColumns = compositeUserTypeColumns;
    }

    public String getColumnNameForPropertyAndPath(
            HibernatePersistentProperty grailsProp, String path, ColumnConfig cc) {
        return Optional.ofNullable(grailsProp.getColumnName(cc)).orElseGet(() -> {
            String suffix = defaultColumnNameFetcher.getDefaultColumnName(grailsProp);
            return Optional.ofNullable(path)
                    .filter(GrailsHibernateUtil::isNotEmpty)
                    .map(p -> backticksRemover.apply(namingStrategy.resolveColumnName(p)) +
                            UNDERSCORE +
                            backticksRemover.apply(suffix))
                    .orElse(suffix);
        });
    }

    /**
     * The names of all the columns of the property: the one name {@link #getColumnNameForPropertyAndPath} resolves
     * without a column configuration for a property that has one column, or one name per attribute of the
     * embeddable class for a property mapped with a composite user type, which spans one column per attribute and
     * has no single column name.
     *
     * @param grailsProp the property
     * @param path the path of the property within its entity
     * @return the column names, in column order
     * @since 8.0.1
     */
    public List<String> getColumnNamesForPropertyAndPath(HibernatePersistentProperty grailsProp, String path) {
        if (grailsProp.getCompositeUserType() != null) {
            return compositeUserTypeColumns.columnNamesOf(grailsProp, path);
        }
        return Collections.singletonList(getColumnNameForPropertyAndPath(grailsProp, path, null));
    }
}
