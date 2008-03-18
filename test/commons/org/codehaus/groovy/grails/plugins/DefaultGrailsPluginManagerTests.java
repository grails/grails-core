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

    protected void tearDown() {
        first = null;
        second = null;
        third = null;
        fourth = null;
    }

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

    /**
     * Test the known 1.0.2 failure where:
     *
     * mail 0.3 = has no deps
     * quartz 0.3-SNAPSHOT: loadAfter = ['core', 'hibernate']
     * emailconfirmation 0.4: dependsOn = [quartz:'0.3 > *', mail: '0.2 > *']
     *
     * ...and emailconfirmation is NOT loaded first.
     */
    public void testDependenciesWithDelayedLoadingWithVersionRangeStrings()  throws IOException {
        GroovyClassLoader gcl = new GroovyClassLoader();

        // These are defined in a specific order so that the one with the range dependencies
        // is the first in the list, and its dependencies load after
        first = gcl.parseClass("class FirstGrailsPlugin {\n" +
            "def version = \"0.4\"\n" +
            "def dependsOn = [second:'0.3 > *', third:'0.2 > *']\n" +
			"}");
        second = gcl.parseClass("class SecondGrailsPlugin {\n" +
        	"def version = \"0.3\"\n" +
            "def dependsOn = [:]\n" +
			"}");
        third = gcl.parseClass("class ThirdGrailsPlugin {\n" +
            "def version = \"0.3-SNAPSHOT\"\n" +
            "def loadAfter = ['core', 'hibernate']\n" +
			"}");

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{}, gcl );
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);

        DefaultGrailsPluginManager manager = new DefaultGrailsPluginManager(new Class[]{first, second, third}, app);
        manager.setParentApplicationContext(parent);
        manager.setPluginFilter(new IncludingPluginFilter(new String[]{"dataSource", "first", "second", "third"}));

        manager.loadPlugins();

        List pluginList = manager.getPluginList();

        assertNotNull(manager.getGrailsPlugin("first"));
        assertNotNull(manager.getGrailsPlugin("second"));
        //dataSource depends on core
        assertNotNull(manager.getGrailsPlugin("core"));
        //third depends on i18n
        assertNotNull(manager.getGrailsPlugin("third"));

        assertEquals("Expected plugins not loaded. Expected " + 5 + " but got " + pluginList, 5, pluginList.size());

    }
}
