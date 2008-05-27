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
package org.codehaus.groovy.grails.plugins;

import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.springframework.context.ApplicationContext;

/**
 * Test for the DefaultGrailsPlugin class
 * 
 * @author Graeme Rocher
 *
 */
public class DefaultGrailsPluginTests extends AbstractGrailsMockTests {
	

	private Class versioned;
	private Class notVersion;
	private Class notPluginClass;
    private Class disabled;
    private Class observed;
    private Class camelCased;

    protected void onTearDown()
    {
        versioned = null;
        notVersion = null;
        notPluginClass = null;
        disabled = null;
        observed = null;
        super.onTearDown();
        //To change body of overridden methods use File | Settings | File Templates.
    }

    protected void onSetUp() {
		versioned = gcl.parseClass("class MyGrailsPlugin {\n" +
						"def version = 1.1;" +
						"def doWithSpring = {" +
						"classEditor(org.springframework.beans.propertyeditors.ClassEditor,application.classLoader )" +
						"}\n" +
						"def doWithApplicationContext = { ctx ->" +
						"assert ctx != null" +
						"}" +
						"}");
		notVersion = gcl.parseClass("class AnotherGrailsPlugin {\n" +
						"}");	
		notPluginClass = gcl.parseClass("class SomeOtherPlugin {\n" +
									"def version = 1.4;" +
									"}");

        disabled = gcl.parseClass("class DisabledGrailsPlugin {" +
                                          "def version = 1.1; " +
                                            "def status = 'disabled'; }");

        observed = gcl.parseClass("class ObservingGrailsPlugin {" +
                                          "def observe = ['another'];" +
                                          "def version = 1.1; " +
                                            "def status = 'disabled'; }");

        camelCased = gcl.parseClass("class CamelCasedGrailsPlugin {" +
                                          "def version = 2.1; " +
                                            "def status = 'disabled'; }");
    }

	public void testDefaultGrailsPlugin() {
		 GrailsPlugin versionPlugin = new DefaultGrailsPlugin(versioned, ga);
		 
		 try {
			GrailsPlugin notVersionPlugin = new DefaultGrailsPlugin(notVersion, ga);
			fail("Should have thrown IllegalArgumentException for unversioned plugin");
		} catch (PluginException e) {
			// expected
		}
		
		try {
			GrailsPlugin notPlugin = new DefaultGrailsPlugin(notPluginClass, ga);
			fail("Should have thrown an exception for invalid plugin");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

    public void testGetPluginPath() {
       GrailsPlugin versionPlugin = new DefaultGrailsPlugin(versioned, ga);

        assertEquals("/plugins/my-1.1", versionPlugin.getPluginPath());

        GrailsPlugin camelCasedPlugin = new DefaultGrailsPlugin(camelCased, ga);

        assertEquals("/plugins/camel-cased-2.1", camelCasedPlugin.getPluginPath());
    }

    public void testDisabledPlugin() {
        GrailsPlugin disabledPlugin = new DefaultGrailsPlugin(disabled, ga);
        GrailsPlugin enabledPlugin = new DefaultGrailsPlugin(versioned, ga);



        assertFalse(disabledPlugin.isEnabled());
        assertTrue(enabledPlugin.isEnabled());
    }

    public void testDoWithApplicationContext() {
		GrailsPlugin versionPlugin = new DefaultGrailsPlugin(versioned, ga);
		
		RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration();
		versionPlugin.doWithRuntimeConfiguration(springConfig);
		
		ApplicationContext ctx = springConfig.getApplicationContext();
		
		assertTrue(ctx.containsBean("classEditor"));
		
		versionPlugin.doWithApplicationContext(ctx);
	}

	public void testDoWithRuntimeConfiguration() {
		GrailsPlugin versionPlugin = new DefaultGrailsPlugin(versioned, ga);
		
		RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration();
		versionPlugin.doWithRuntimeConfiguration(springConfig);
		
		ApplicationContext ctx = springConfig.getApplicationContext();
		
		assertTrue(ctx.containsBean("classEditor"));
	}

	public void testGetName() {
		GrailsPlugin versionPlugin = new DefaultGrailsPlugin(versioned, ga);
		assertEquals("my", versionPlugin.getName());
	}

	public void testGetVersion() {
		GrailsPlugin versionPlugin = new DefaultGrailsPlugin(versioned, ga);
		assertEquals("1.1", versionPlugin.getVersion());
	}

    public void testObservers() {

        GrailsPlugin observingPlugin = new DefaultGrailsPlugin(observed, ga);

        assertEquals(1, observingPlugin.getObservedPluginNames().length);
        assertEquals("another", observingPlugin.getObservedPluginNames()[0]);

    }

}
