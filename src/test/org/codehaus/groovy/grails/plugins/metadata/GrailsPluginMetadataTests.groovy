package org.codehaus.groovy.grails.plugins.metadata

import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import grails.util.GrailsUtil

/**
 * @author Graeme Rocher
 * @since 1.2
 */

public class GrailsPluginMetadataTests extends GroovyTestCase{


    void testAnnotatedMetadata() {
        def app = new DefaultGrailsApplication([Test1, Test2, Test3] as Class[], getClass().classLoader)
        def pluginManager = new DefaultGrailsPluginManager([] as Class[], app)
        pluginManager.loadPlugins()

        assertEquals "/plugins/controllers-${GrailsUtil.grailsVersion}", pluginManager.getPluginPathForClass(Test1)
        assertEquals "/plugins/groovy-pages-${GrailsUtil.grailsVersion}", pluginManager.getPluginPathForClass(Test2)
        assertNull pluginManager.getPluginPathForClass(Test3)

        assertEquals "/plugins/controllers-${GrailsUtil.grailsVersion}", pluginManager.getPluginPathForInstance(new Test1())
        assertEquals "/plugins/groovy-pages-${GrailsUtil.grailsVersion}", pluginManager.getPluginPathForInstance(new Test2())
        assertNull pluginManager.getPluginPathForInstance(new Test3())

    }

}

@GrailsPlugin(name='controllers', version='1.0')
class Test1 {}
@GrailsPlugin(name='groovyPages', version='1.2')
class Test2 {}
class Test3 {}