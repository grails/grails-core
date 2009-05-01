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
        assertEquals("/foo/bar.gsp", GroovyPageUtils.getViewURI("foo", "bar"));
        assertEquals("/bar/foo.gsp", GroovyPageUtils.getViewURI("foo", "/bar/foo"));
        assertEquals("/foo/bar/foo.gsp", GroovyPageUtils.getViewURI("foo", "bar/foo"));
    }

    public void testNoSuffxGetViewURI() {
        assertEquals("/foo/bar", GroovyPageUtils.getNoSuffixViewURI("foo", "bar"));
        assertEquals("/bar/foo", GroovyPageUtils.getNoSuffixViewURI("foo", "/bar/foo"));
        assertEquals("/foo/bar/foo", GroovyPageUtils.getNoSuffixViewURI("foo", "bar/foo"));
    }


    public void testGetTemplateURI() {
        assertEquals("/foo/_bar.gsp", GroovyPageUtils.getTemplateURI("foo", "bar"));
        assertEquals("/bar/_foo.gsp", GroovyPageUtils.getTemplateURI("foo", "/bar/foo"));
        assertEquals("/foo/bar/_foo.gsp", GroovyPageUtils.getTemplateURI("foo", "bar/foo"));

    }

    public void testGetTemplateURIForController() throws IllegalAccessException, InstantiationException {
        GroovyObject controller = (GroovyObject) new GroovyClassLoader().parseClass("class FooController { }").newInstance();
        assertEquals("/foo/_bar.gsp", GroovyPageUtils.getTemplateURI(controller, "bar"));
        assertEquals("/bar/_foo.gsp", GroovyPageUtils.getTemplateURI(controller, "/bar/foo"));
        assertEquals("/foo/bar/_foo.gsp", GroovyPageUtils.getTemplateURI(controller, "bar/foo"));

    }

    public void testGetViewURIForController() throws IllegalAccessException, InstantiationException {
        GroovyObject controller = (GroovyObject) new GroovyClassLoader().parseClass("class FooController { }").newInstance();
        assertEquals("/foo/bar.gsp", GroovyPageUtils.getViewURI(controller, "bar"));
        assertEquals("/bar/foo.gsp", GroovyPageUtils.getViewURI(controller, "/bar/foo"));
        assertEquals("/foo/bar/foo.gsp", GroovyPageUtils.getViewURI(controller, "bar/foo"));
    }

   public void testNoSuffixGetViewURIForController() throws IllegalAccessException, InstantiationException {
        GroovyObject controller = (GroovyObject) new GroovyClassLoader().parseClass("class FooController { }").newInstance();
        assertEquals("/foo/bar", GroovyPageUtils.getNoSuffixViewURI(controller, "bar"));
        assertEquals("/bar/foo", GroovyPageUtils.getNoSuffixViewURI(controller, "/bar/foo"));
        assertEquals("/foo/bar/foo", GroovyPageUtils.getNoSuffixViewURI(controller, "bar/foo"));
    }
}
