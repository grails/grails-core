package org.grails.plugins

import grails.core.DefaultGrailsApplication
import grails.plugins.DefaultGrailsPluginManager
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class PluginLoadOrderTests {

    @Test
    void testPluginLoadBeforeAfter() {
        def gcl = new GroovyClassLoader()

        gcl.parseClass '''
class FiveGrailsPlugin {
    def version = 0.1
    def loadAfter = ['one']
}

class FourGrailsPlugin {
    def version = 0.1
    def loadAfter = ['two']
}
class OneGrailsPlugin {
    def version = 0.1
    def loadBefore = ['two']
}
class TwoGrailsPlugin {
    def version = 0.1
    def loadAfter = ['three']
}
class ThreeGrailsPlugin {
    def version = 0.1
}
'''

        def one = gcl.loadClass("OneGrailsPlugin")
        def two = gcl.loadClass("TwoGrailsPlugin")
        def three = gcl.loadClass("ThreeGrailsPlugin")
        def four = gcl.loadClass("FourGrailsPlugin")
        def five = gcl.loadClass("FiveGrailsPlugin")
        def pluginManager = new DefaultGrailsPluginManager([one,two,three, four,five] as Class[],
            new DefaultGrailsApplication())

        pluginManager.loadCorePlugins = false
        pluginManager.loadPlugins()

        assertEquals "one", pluginManager.pluginList[0].name
        assertEquals "three", pluginManager.pluginList[1].name
        assertEquals "five", pluginManager.pluginList[2].name
        assertEquals "two", pluginManager.pluginList[3].name
        assertEquals "four", pluginManager.pluginList[4].name
    }
}
