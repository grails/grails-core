/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.support;

import groovy.lang.GroovyClassLoader;
import junit.framework.TestCase;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;

/**
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: Jan 30, 2008
 */
public class DevelopmentResourceLoaderTests extends TestCase {

    public void testDevelopmentResourceLoaderNoSlash() {
        DevelopmentResourceLoader resourceLoader = new DevelopmentResourceLoader(new DefaultGrailsApplication(new Class[0], new GroovyClassLoader()));

        assertEquals("file:./grails-app/i18n", resourceLoader.getRealLocationInProject("WEB-INF/grails-app/i18n"));
    }


    public void testDevelopmentResourceLoader() {
        DevelopmentResourceLoader resourceLoader = new DevelopmentResourceLoader(new DefaultGrailsApplication(new Class[0], new GroovyClassLoader()));

        assertEquals("file:./grails-app/i18n", resourceLoader.getRealLocationInProject("/WEB-INF/grails-app/i18n"));
    }

    public void testDevelopmentResourceLoaderAndBaseLoc() {
        DevelopmentResourceLoader resourceLoader = new DevelopmentResourceLoader(new DefaultGrailsApplication(new Class[0], new GroovyClassLoader()), "/home");

        assertEquals("file:/home/grails-app/i18n", resourceLoader.getRealLocationInProject("/WEB-INF/grails-app/i18n"));
    }

}
