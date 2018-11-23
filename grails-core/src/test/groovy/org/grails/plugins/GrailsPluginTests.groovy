package org.grails.plugins

import grails.util.Environment

import grails.core.DefaultGrailsApplication
import grails.plugins.DefaultGrailsPluginManager

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class GrailsPluginTests extends GroovyTestCase {

    void testPluginPath() {

        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestOneGrailsPlugin {
    def version = 0.1
    def scopes = 'test'
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def plugin = new DefaultGrailsPlugin(test1, application)

        assertEquals "/plugins/test-one-0.1", plugin.pluginPath
    }

    void testPluginPathLongName() {

        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestOnetwoThreeFourfiveGrailsPlugin {
    def version = 0.1
    def scopes = 'test'
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def plugin = new DefaultGrailsPlugin(test1, application)

        assertEquals "/plugins/test-onetwo-three-fourfive-0.1", plugin.pluginPath
    }

    void testPluginPathCamelCase() {

        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestOneGrailsPlugin {
    def version = 0.1
    def scopes = 'test'
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def plugin = new DefaultGrailsPlugin(test1, application)

        assertEquals "/plugins/testOne-0.1", plugin.pluginPathCamelCase
    }

    void testPluginPathCamelCaseLongName() {

        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestOnetwoThreeFourfiveGrailsPlugin {
    def version = 0.1
    def scopes = 'test'
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def plugin = new DefaultGrailsPlugin(test1, application)

        assertEquals "/plugins/testOnetwoThreeFourfive-0.1", plugin.pluginPathCamelCase
    }

    void testFileSystemName() {

        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestOneGrailsPlugin {
    def version = 0.1
    def scopes = 'test'
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def plugin = new DefaultGrailsPlugin(test1, application)

        assertEquals "test-one-0.1", plugin.fileSystemName
    }

    void testSimpleEnvironmentEvaluation() {
        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestGrailsPlugin {
    def version = 0.1
    def environments = 'dev'
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def plugin = new DefaultGrailsPlugin(test1, application)

        assertTrue plugin.supportsEnvironment(Environment.DEVELOPMENT)
        assertFalse plugin.supportsEnvironment(Environment.PRODUCTION)
    }

    void testListEnvironmentEvaluation() {
        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestGrailsPlugin {
    def version = 0.1
    def environments = ['test','dev']
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def plugin = new DefaultGrailsPlugin(test1, application)

        assertTrue plugin.supportsEnvironment(Environment.DEVELOPMENT)
        assertTrue plugin.supportsEnvironment(Environment.TEST)
        assertFalse plugin.supportsEnvironment(Environment.PRODUCTION)
    }

    void testEnvironmentsAndLoadIntoPluginManager() {
        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestGrailsPlugin {
    def version = 0.1
    def environments = ['test','dev']
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def pluginManager = new DefaultGrailsPluginManager([test1] as Class[], application)

        pluginManager.loadPlugins()
        assertNotNull pluginManager.getGrailsPlugin("test")

        try {
            System.setProperty(Environment.KEY, Environment.PRODUCTION.getName())

            pluginManager = new DefaultGrailsPluginManager([test1] as Class[], application)
            pluginManager.loadPlugins()
            assertNull pluginManager.getGrailsPlugin("test")
        } finally {
            System.setProperty(Environment.KEY, Environment.TEST.getName())
        }
    }
}
