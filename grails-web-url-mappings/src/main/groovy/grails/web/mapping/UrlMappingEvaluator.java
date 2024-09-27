/*
 * Copyright 2004-2005 the original author or authors.
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
package grails.web.mapping;

import groovy.lang.Closure;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Evaluates URL mapping from the given Spring Resource or class.
 *
 * @see UrlMapping
 * @see UrlMappingInfo
 *
 * @author Graeme Rocher
 * @since 0.5
 */
public interface UrlMappingEvaluator {

    /**
     * Evaluates URL mapping from the give Spring Resource
     *
     * @param resource The Spring Resource to evaluate mapping from
     *
     * @return A list of UrlMapping instances
     */
    @SuppressWarnings("rawtypes")
    List<UrlMapping> evaluateMappings(Resource resource);

    /**
     * Evaluates mapping from the given class if possible
     *
     * @param theClass The class to evaluate mapping from
     * @return A list of UrlMapping instances
     */
    @SuppressWarnings("rawtypes")
    List<UrlMapping> evaluateMappings(Class theClass);

    /**
     * Evaluates mapping from the given closure if possible
     *
     * @param closure The closure to evaluate mapping from
     * @return A list of UrlMapping instances
     */
    @SuppressWarnings("rawtypes")
    List<UrlMapping> evaluateMappings(Closure closure);
}
