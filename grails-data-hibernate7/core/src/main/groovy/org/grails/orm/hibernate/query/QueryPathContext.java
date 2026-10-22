/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.orm.hibernate.query;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;

/**
 * What a criterion or order handler sees of the query it converts for: the root of the query and the resolution
 * of property paths the way the built-in criteria resolve them. {@code alias.property}, for an alias created with
 * {@code createAlias}, or {@code association.property} becomes a path on the join GORM created for that
 * association, so a custom criterion and a built-in one on the same association restrict the same row rather
 * than each joining the association again.
 *
 * @since 8.1
 */
public interface QueryPathContext {

    /**
     * @return the root of the query or subquery the handler converts for
     */
    From<?, ?> getRoot();

    /**
     * @param path a property path: {@code property}, {@code association.property} or {@code alias.property}
     * @return the path, or {@code null} when the property path resolves to an expression that is not a path
     */
    Path<?> getFullyQualifiedPath(String path);

    /**
     * @param path a property path, or the alias of a projection
     * @return the expression for it: a path, or the expression an alias or projection stands for
     */
    Expression<?> getFullyQualifiedExpression(String path);

    /**
     * @param alias the column alias of a {@code sqlProjection} of the query
     * @return the projected expression recorded under that alias, so that a handler can order by it, or {@code null}
     *     when no projection of the query carries the alias
     */
    Expression<?> getSelectionAlias(String alias);
}
