/**
 * Class description here.
 
 * @author Graeme Rocher
 * @since 0.4
  *
 * Created: Aug 21, 2007
 * Time: 8:26:39 AM
 * 
 */
package org.codehaus.groovy.grails.plugins

import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException

class PluginMetaManagerTests extends GroovyTestCase {

 def simpleDescriptor = '''
<plugin name='simple' version='0.1'>
  <resources>
    <resource>FooController</resource>
  </resources>
</plugin>
 '''

 def badDescriptor = '''
<plugin>
  <resources>
    <resource>FooController</resource>
  </resources>
</plugin>
 '''

    PluginMetaManager metaManager

    void setUp() {
        def b = new ByteArrayResource(simpleDescriptor.getBytes())

        this.metaManager = new DefaultPluginMetaManager([b] as Resource[])
    }


    void testBadDescriptor() {       
        def b = new ByteArrayResource(badDescriptor.getBytes())

        shouldFail(GrailsConfigurationException) {
            new DefaultPluginMetaManager([b] as Resource[])
        }

    }

    void testGetPluginResources() {        
        def resources = metaManager.getPluginResources("simple")
        assertEquals 1, resources.size()
        assertEquals "FooController", resources[0]
    }

    void testGetPluginPathForResource() {
        assertEquals "/plugins/simple-0.1", metaManager.getPluginPathForResource("FooController")
    }

    void testGetPluginViewsPathForResource() {
        String viewsPath = metaManager.getPluginViewsPathForResource("FooController")
        println viewsPath
        assertEquals "/plugins/simple-0.1/grails-app/views", viewsPath
    }
}