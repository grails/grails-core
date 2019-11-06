package org.grails.plugins.metadata

import grails.plugins.DefaultGrailsPluginManager
import grails.core.DefaultGrailsApplication
import grails.plugins.metadata.GrailsPlugin
import grails.util.GrailsUtil

/**
 * @author Graeme Rocher
 * @since 1.2
 */
class GrailsPluginMetadataTests extends GroovyTestCase {

    void testAnnotatedMetadata() {
        def app = new DefaultGrailsApplication([Test1, Test2, Test3] as Class[], getClass().classLoader)
        def pluginManager = new DefaultGrailsPluginManager([] as Class[], app)
        pluginManager.loadPlugins()

        assertEquals "/plugins/controllers-${GrailsUtil.grailsVersion}", pluginManager.getPluginPathForClass(Test1)
        assertNull pluginManager.getPluginPathForClass(Test3)

        assertEquals "/plugins/controllers-${GrailsUtil.grailsVersion}", pluginManager.getPluginPathForInstance(new Test1())
        assertNull pluginManager.getPluginPathForInstance(new Test3())

        assertEquals "/plugins/controllers-${GrailsUtil.grailsVersion}/grails-app/views", pluginManager.getPluginViewsPathForClass(Test1)
        assertNull pluginManager.getPluginViewsPathForClass(Test3)
    }
}

@GrailsPlugin(name='controllers', version='1.0')
class Test1 {}
@GrailsPlugin(name='dataBinding', version='1.2')
class Test2 {}
class Test3 {}
