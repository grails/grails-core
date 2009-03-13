package org.codehaus.groovy.grails.plugins

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class PluginLoadOrderTests extends GroovyTestCase{

    void testPluginLoadBeforeAfter() {
        def gcl = new GroovyClassLoader()

        gcl.parseClass('''
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

''')

        def one = gcl.loadClass("OneGrailsPlugin")
        def two = gcl.loadClass("TwoGrailsPlugin")
        def three = gcl.loadClass("ThreeGrailsPlugin")
        def four = gcl.loadClass("FourGrailsPlugin")
        def five = gcl.loadClass("FiveGrailsPlugin")
        def pluginManager = new DefaultGrailsPluginManager([one,two,three, four,five] as Class[], new DefaultGrailsApplication())

        pluginManager.loadCorePlugins = false
        pluginManager.loadPlugins()


        println pluginManager.pluginList
        println pluginManager.pluginList.size()
        assertEquals "one", pluginManager.pluginList[0].name
        assertEquals "three", pluginManager.pluginList[1].name
        assertEquals "five", pluginManager.pluginList[2].name
        assertEquals "two", pluginManager.pluginList[3].name
        assertEquals "four", pluginManager.pluginList[4].name
    }



}