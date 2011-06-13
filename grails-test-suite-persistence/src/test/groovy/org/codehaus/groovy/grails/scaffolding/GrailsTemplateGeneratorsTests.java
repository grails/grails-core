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
package org.codehaus.groovy.grails.scaffolding;

import grails.util.BuildSettings;
import grails.util.BuildSettingsHolder;
import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.StringWriter;
import java.util.Map;

import junit.framework.TestCase;

import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.plugins.MockGrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerHolder;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * @author Graeme Rocher
 */
@SuppressWarnings("rawtypes")
public class GrailsTemplateGeneratorsTests extends TestCase {

    @Override
    protected void setUp() {
        BuildSettings buildSettings = new BuildSettings(new File("."));
        BuildSettingsHolder.setSettings(buildSettings);
        MockGrailsPluginManager pluginManager = new MockGrailsPluginManager();
        PluginManagerHolder.setPluginManager(pluginManager);
        pluginManager.registerMockPlugin(DefaultGrailsTemplateGeneratorTests.fakeHibernatePlugin);
    }

    @Override
    protected void tearDown() {
        BuildSettingsHolder.setSettings(null);
        PluginManagerHolder.setPluginManager(null);
    }

    public void testGenerateController() throws Exception {
        DefaultGrailsTemplateGenerator generator;

        GroovyClassLoader gcl = new GroovyClassLoader(Thread.currentThread().getContextClassLoader());

        generator = new DefaultGrailsTemplateGenerator();
        generator.setBasedir("../grails-resources");

        Class dc = gcl.parseClass("class Test { \n Long id;\n  Long version;  }");
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(dc);

        File generatedFile = new File("test/grails-app/controllers/TestController.groovy");
        if (generatedFile.exists()) {
            generatedFile.delete();
        }

        StringWriter sw = new StringWriter();
        generator.generateController(domainClass,sw);

        String text = sw.toString();

        Class controllerClass = gcl.parseClass(text);
        BeanWrapper bean = new BeanWrapperImpl(controllerClass.newInstance());

        assertEquals("TestController", controllerClass.getName());

        assertTrue(bean.isReadableProperty("list"));
        assertTrue(bean.isReadableProperty("update"));
        assertTrue(bean.isReadableProperty("create"));
        assertTrue(bean.isReadableProperty("list"));
        assertTrue(bean.isReadableProperty("show"));
        assertTrue(bean.isReadableProperty("edit"));
        assertTrue(bean.isReadableProperty("delete"));

        Object propertyValue = GrailsClassUtils.getStaticPropertyValue(controllerClass, "allowedMethods");
        assertTrue("allowedMethods property was the wrong type", propertyValue instanceof Map);
        Map map = (Map) propertyValue;
        assertTrue("allowedMethods did not contain the delete action", map.containsKey("delete"));
        assertTrue("allowedMethods did not contain the save action", map.containsKey("save"));
        assertTrue("allowedMethods did not contain the update action", map.containsKey("update"));

        assertEquals("allowedMethods had incorrect value for delete action", "POST", map.get("delete"));
        assertEquals("allowedMethods had incorrect value for save action", "POST", map.get("save"));
        assertEquals("allowedMethods had incorrect value for update action", "POST", map.get("update"));
    }

    public void testGenerateViews() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader(Thread.currentThread().getContextClassLoader());

        DefaultGrailsTemplateGenerator generator = new DefaultGrailsTemplateGenerator();
        generator.setBasedir("../grails-resources");

        Class dc = gcl.parseClass(
                "class Test { " +
                "\n Long id;" +
                "\n Long version;" +
                "\n String name;" +
                "\n TimeZone tz;" +
                "\n Locale locale;" +
                "\n Currency currency;" +
                "\n Boolean active;" +
                "\n Date age  }");
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(dc);

        File show = new File("test/grails-app/views/test/show.gsp");
        show.deleteOnExit();
        File list = new File("test/grails-app/views/test/list.gsp");
        list.deleteOnExit();
        File edit = new File("test/grails-app/views/test/edit.gsp");
        edit.deleteOnExit();
        File create = new File("test/grails-app/views/test/create.gsp");
        create.deleteOnExit();
        File form = new File("test/grails-app/views/test/_form.gsp");
        form.deleteOnExit();

        generator.generateViews(domainClass,"test");

        assertTrue(show.exists());
        assertTrue(list.exists());
        assertTrue(edit.exists());
        assertTrue(create.exists());
        assertTrue(form.exists());
    }
}
