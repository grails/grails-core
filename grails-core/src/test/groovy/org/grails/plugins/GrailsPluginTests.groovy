package org.grails.plugins

import grails.core.DefaultGrailsApplication
import grails.plugins.DefaultGrailsPluginManager
import grails.util.Environment
import grails.util.GrailsUtil
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*

/**
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 1.0
 */
class GrailsPluginTests {

    @Test
    void testPluginVersion() {

        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestOneGrailsPlugin {
    def version = '0.1'
    def scopes = 'test'
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def plugin = new DefaultGrailsPlugin(test1, application)

        assertEquals "0.1", plugin.version
    }

    @Test
    void testPluginGrailsVersion() {

        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
import grails.util.GrailsUtil

class TestOneGrailsPlugin {
    def version = '0.1'
    def grailsVersion = GrailsUtil.getGrailsVersion()
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def plugin = new DefaultGrailsPlugin(test1, application)

        assertEquals "0.1", plugin.version
        assertEquals GrailsUtil.getGrailsVersion(), plugin.properties.grailsVersion
    }

    @Test
    void testPluginName() {

        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestOneGrailsPlugin {
    def version = '0.1'
    def scopes = 'test'
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def plugin = new DefaultGrailsPlugin(test1, application)

        assertEquals "testOne", plugin.name
        assertEquals "testOne-0.1", plugin.fullName
    }

    @Test
    void testPluginProperties() {

        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestOneGrailsPlugin {
    def version = '0.1'
    def scopes = 'test'
    def environments = 'dev'    
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]
    def dependsOn = [core: version, domainClass: version, services: version]
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def plugin = new DefaultGrailsPlugin(test1, application)

        assertEquals "0.1", plugin.properties.version
        assertEquals "test", plugin.properties.scopes
        assertEquals "dev", plugin.properties.environments
        assertEquals ["grails-app/views/error.gsp"], plugin.properties.pluginExcludes
        assertEquals [core: '0.1', domainClass: '0.1', services: '0.1'], plugin.properties.dependsOn
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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
        pluginManager.setLoadCorePlugins(false)

        pluginManager.loadPlugins()
        assertNotNull pluginManager.getGrailsPlugin("test")

        try {
            System.setProperty(Environment.KEY, Environment.PRODUCTION.getName())

            pluginManager = new DefaultGrailsPluginManager([test1] as Class[], application)
            pluginManager.setLoadCorePlugins(false)
            pluginManager.loadPlugins()
            assertNull pluginManager.getGrailsPlugin("test")
        } finally {
            System.setProperty(Environment.KEY, Environment.TEST.getName())
        }
    }
}
