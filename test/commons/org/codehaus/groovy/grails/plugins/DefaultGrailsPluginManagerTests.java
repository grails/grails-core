package org.codehaus.groovy.grails.plugins;

import groovy.lang.GroovyClassLoader;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.support.MockApplicationContext;

public class DefaultGrailsPluginManagerTests extends TestCase {
	
	private Class first;
	private Class second;
	private Class third;
	private Class fourth;

	public void testLoadPlugins() throws IOException {

        GroovyClassLoader gcl = new GroovyClassLoader();
        
        first = gcl.parseClass("class FirstGrailsPlugin {\n" +
        	"def version = 1.0\n" +
			"}");	
        second = gcl.parseClass("class SecondGrailsPlugin {\n" +
            "def version = 1.0\n" +
            "def dependsOn = [first:version]\n" +
			"}");	
        third = gcl.parseClass("import org.codehaus.groovy.grails.plugins.support.GrailsPluginUtils\n" +
        		"class ThirdGrailsPlugin {\n" +
            "def version = GrailsPluginUtils.getGrailsVersion()\n" +
            "def dependsOn = [i18n:version]\n" +
			"}");	
        fourth = gcl.parseClass("class FourthGrailsPlugin {\n" +
            "def version = 1.0\n" +
            "def dependsOn = [second:version, third:version]\n" +
			"}");	

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{}, gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);
        
        DefaultGrailsPluginManager manager = new DefaultGrailsPluginManager(new Class[]{first, second, third, fourth}, app);
        manager.setParentApplicationContext(parent);
        manager.setPluginFilter(new IncludingPluginFilter(new String[]{"dataSource", "first", "third"}));
        
        manager.loadPlugins();
       
        List pluginList = manager.getPluginList();
        
        assertNotNull(manager.getGrailsPlugin("dataSource"));
        assertNotNull(manager.getGrailsPlugin("first"));
        assertNotNull(manager.getGrailsPlugin("third"));
        //dataSource depends on core
        assertNotNull(manager.getGrailsPlugin("core"));
        //third depends on i18n
        assertNotNull(manager.getGrailsPlugin("third"));
        
        assertEquals("Expected plugins not loaded. Expected " + 5 + " but got " + pluginList, 5, pluginList.size());

	}

}
