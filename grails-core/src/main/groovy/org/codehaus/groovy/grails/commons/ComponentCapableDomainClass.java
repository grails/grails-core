/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.commons;

import java.util.List;

/**
 * Interface for domains capable of supporting components.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public interface ComponentCapableDomainClass {

    /**
     * Adds a component
     *
     * @param component The component
     */
    void addComponent(GrailsDomainClass component);

    /**
     * Gets all the components for this domain class
     *
     * @return The list of components
     */
    List<GrailsDomainClass> getComponents();
}
