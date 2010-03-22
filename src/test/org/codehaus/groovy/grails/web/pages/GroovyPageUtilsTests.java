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

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import junit.framework.TestCase;

/**
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: May 1, 2009
 */
public class GroovyPageUtilsTests extends TestCase{
    public void testGetViewURI() {
    	GroovyPagesUriSupport uriSupport = new GroovyPagesUriSupport();
        assertEquals("/foo/bar.gsp", uriSupport.getViewURI("foo", "bar"));
        assertEquals("/bar/foo.gsp", uriSupport.getViewURI("foo", "/bar/foo"));
        assertEquals("/foo/bar/foo.gsp", uriSupport.getViewURI("foo", "bar/foo"));
    }

    public void testNoSuffxGetViewURI() {
    	GroovyPagesUriSupport uriSupport = new GroovyPagesUriSupport();
        assertEquals("/foo/bar", uriSupport.getNoSuffixViewURI("foo", "bar"));
        assertEquals("/bar/foo", uriSupport.getNoSuffixViewURI("foo", "/bar/foo"));
        assertEquals("/foo/bar/foo", uriSupport.getNoSuffixViewURI("foo", "bar/foo"));
    }


    public void testGetTemplateURI() {
    	GroovyPagesUriSupport uriSupport = new GroovyPagesUriSupport();
        assertEquals("/foo/_bar.gsp", uriSupport.getTemplateURI("foo", "bar"));
        assertEquals("/bar/_foo.gsp", uriSupport.getTemplateURI("foo", "/bar/foo"));
        assertEquals("/foo/bar/_foo.gsp", uriSupport.getTemplateURI("foo", "bar/foo"));

    }

    public void testGetTemplateURIForController() throws IllegalAccessException, InstantiationException {
        GroovyObject controller = (GroovyObject) new GroovyClassLoader().parseClass("class FooController { }").newInstance();
        GroovyPagesUriSupport uriSupport = new GroovyPagesUriSupport();
        assertEquals("/foo/_bar.gsp", uriSupport.getTemplateURI(controller, "bar"));
        assertEquals("/bar/_foo.gsp", uriSupport.getTemplateURI(controller, "/bar/foo"));
        assertEquals("/foo/bar/_foo.gsp", uriSupport.getTemplateURI(controller, "bar/foo"));

    }

    public void testGetViewURIForController() throws IllegalAccessException, InstantiationException {
        GroovyObject controller = (GroovyObject) new GroovyClassLoader().parseClass("class FooController { }").newInstance();
        GroovyPagesUriSupport uriSupport = new GroovyPagesUriSupport();
        assertEquals("/foo/bar.gsp", uriSupport.getViewURI(controller, "bar"));
        assertEquals("/bar/foo.gsp", uriSupport.getViewURI(controller, "/bar/foo"));
        assertEquals("/foo/bar/foo.gsp", uriSupport.getViewURI(controller, "bar/foo"));
    }

   public void testNoSuffixGetViewURIForController() throws IllegalAccessException, InstantiationException {
        GroovyObject controller = (GroovyObject) new GroovyClassLoader().parseClass("class FooController { }").newInstance();
        GroovyPagesUriSupport uriSupport = new GroovyPagesUriSupport();
        assertEquals("/foo/bar", uriSupport.getNoSuffixViewURI(controller, "bar"));
        assertEquals("/bar/foo", uriSupport.getNoSuffixViewURI(controller, "/bar/foo"));
        assertEquals("/foo/bar/foo", uriSupport.getNoSuffixViewURI(controller, "bar/foo"));
    }
}
