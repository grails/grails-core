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

import junit.framework.TestCase;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.io.File;
import java.util.Map;

/**
 * @author Graeme Rocher
 * @since 09-Feb-2006
 */
public class GrailsTemplateGeneratorsTests extends TestCase {

    public void testGenerateController() throws Exception {
        GrailsTemplateGenerator generator;

        GroovyClassLoader gcl = new GroovyClassLoader(Thread.currentThread().getContextClassLoader());

        generator = new DefaultGrailsTemplateGenerator();

        Class dc = gcl.parseClass("class Test { \n Long id;\n  Long version;  }");
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(dc);

        File generatedFile = new File("test/grails-app/controllers/TestController.groovy");
        if(generatedFile.exists()) {
        	generatedFile.delete();
        }
        
        generator.generateController(domainClass,"test");

        
        assertTrue(generatedFile.exists());

        String text = (String)new GroovyShell().evaluate("new File('test/grails-app/controllers/TestController.groovy').text");

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
        assertTrue(bean.isReadableProperty("allowedMethods"));
        
        Object propertyValue = bean.getPropertyValue("allowedMethods");
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
        GrailsTemplateGenerator generator;

        GroovyClassLoader gcl = new GroovyClassLoader(Thread.currentThread().getContextClassLoader());

        generator = new DefaultGrailsTemplateGenerator();

        Class dc = gcl.parseClass("class Test { " +
                                        "\n Long id;" +
                                        "\n Long version;" +
                                        "\n String name;" +
                                        "\n TimeZone tz;" +
                                        "\n Locale locale;" +
                                        "\n Currency currency;" +
                                        "\n Boolean active;" +
                                        "\n Date age  }");
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(dc);

        generator.generateViews(domainClass,"test");

        File showFile = new File("test/grails-app/views/test/show.gsp");
        assertTrue(showFile.exists());
        File listFile = new File("test/grails-app/views/test/list.gsp");
        assertTrue(listFile.exists());
        File editFile = new File("test/grails-app/views/test/edit.gsp");
        assertTrue(editFile.exists());        
    }

}
