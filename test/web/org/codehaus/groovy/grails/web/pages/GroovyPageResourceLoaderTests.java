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
package org.codehaus.groovy.grails.web.pages;

import junit.framework.TestCase;

/**
 * Tests for the development ResourceLoader instance of Groovy Server Pages.
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Feb 26, 2007
 *        Time: 5:44:06 PM
 */
public class GroovyPageResourceLoaderTests extends TestCase {

    public void testGetRealLocationInProject() {
        GroovyPageResourceLoader rl = new GroovyPageResourceLoader();

        assertEquals("grails-app/views/layouts/main.gsp", rl.getRealLocationInProject("/WEB-INF/grails-app/views/layouts/main.gsp"));
        assertEquals("grails-app/views/books/list.gsp", rl.getRealLocationInProject("/WEB-INF/grails-app/views/books/list.gsp"));
        assertEquals("grails-app/views/_template.gsp",rl.getRealLocationInProject( "/WEB-INF/grails-app/views/_template.gsp"));
        assertEquals("web-app/other.gsp", rl.getRealLocationInProject("/other.gsp"));
        assertEquals("web-app/somedir/other.gsp",rl.getRealLocationInProject( "/somedir/other.gsp"));
    }
}
