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
package org.codehaus.groovy.grails.commons;

import groovy.lang.GroovyClassLoader;
import junit.framework.TestCase;

/**
 * @author Steven Devijver
 */
public class GrailsClassTests extends TestCase {

    public GrailsClassTests() {
        super();
    }

    public GrailsClassTests(String name) {
        super(name);
    }

    public void testAbstractGrailsClassNoPackage() throws Exception {
        GroovyClassLoader cl = new GroovyClassLoader();
        Class<?> clazz = cl.parseClass("class TestService { }");
        GrailsClass grailsClass = new AbstractGrailsClass(clazz, "Service") {/*empty*/};
        assertEquals("TestService", clazz.getName());
        assertEquals("Test", grailsClass.getName());
        assertEquals("TestService", grailsClass.getFullName());
        assertNotNull(grailsClass.newInstance());
    }

    public void testAbstractGrailsClassPackage() throws Exception {
        GroovyClassLoader cl = new GroovyClassLoader();
        Class<?> clazz = cl.parseClass("package test.casey; class TestService { }");
        GrailsClass grailsClass = new AbstractGrailsClass(clazz, "Service") {/*empty*/};
        assertEquals("test.casey.TestService", clazz.getName());
        assertEquals("Test", grailsClass.getName());
        assertEquals("test.casey.TestService", grailsClass.getFullName());
        assertNotNull(grailsClass.newInstance());
    }

    public void testGrailsClassNonPublicConstructor() throws Exception {
        GroovyClassLoader cl = new GroovyClassLoader();
        Class<?> clazz = cl.parseClass("class ProtectedConstructor { protected ProtectedConstructor() {}}");
        GrailsClass grailsClass = new AbstractGrailsClass(clazz, "ProtectedConstructor") {/*empty*/};
        assertNotNull(grailsClass.newInstance());
    }
}
