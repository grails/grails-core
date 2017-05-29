/* Copyright 2004-2005 the original author or authors.
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
package org.grails.commons;

import grails.core.ArtefactHandler;
import groovy.lang.GroovyClassLoader;
import junit.framework.TestCase;
import org.grails.core.artefact.DomainClassArtefactHandler;

/**
 * @author Marc Palmer
 */
public class DomainClassArtefactHandlerTests extends TestCase {

    public void testIsDomainClass() {

        GroovyClassLoader gcl = new GroovyClassLoader();
        Class<?> c = gcl.parseClass("@grails.persistence.Entity\nclass Test { Long id;Long version;}\n");

        ArtefactHandler handler = new DomainClassArtefactHandler();
        assertTrue(handler.isArtefact(c));
    }
}
