package org.codehaus.groovy.grails.plugins

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException
import grails.util.BuildSettings
import grails.util.Environment
import grails.util.BuildScope

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Dec 12, 2008
 */

public class GrailsPluginTests extends GroovyTestCase{

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

    void testSimpleScopeEvaluation() {
        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestGrailsPlugin {
    def version = 0.1
    def scopes = 'test'
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def plugin = new DefaultGrailsPlugin(test1, application)

        assertTrue plugin.supportsScope(BuildScope.TEST)
        assertFalse plugin.supportsScope(BuildScope.WAR)
    }

    void testListScopeEvaluation() {
        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestGrailsPlugin {
    def version = 0.1
    def scopes = ['test','war']
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def plugin = new DefaultGrailsPlugin(test1, application)

        assertTrue plugin.supportsScope(BuildScope.TEST)
        assertTrue plugin.supportsScope(BuildScope.WAR)
        assertFalse plugin.supportsScope(BuildScope.RUN)
    }

    void testIncludesExcludesScopeEvaluation() {
        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestGrailsPlugin {
    def version = 0.1
    def scopes = [includes:'test',excludes:'war']
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def plugin = new DefaultGrailsPlugin(test1, application)

        assertTrue plugin.supportsScope(BuildScope.TEST)
        assertFalse plugin.supportsScope(BuildScope.WAR)
        assertFalse plugin.supportsScope(BuildScope.RUN)


    }

    void testExcludesScopeEvaluation() {
        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestGrailsPlugin {
    def version = 0.1
    def scopes = [excludes:'war']
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def plugin = new DefaultGrailsPlugin(test1, application)

        assertTrue plugin.supportsScope(BuildScope.TEST)
        assertFalse plugin.supportsScope(BuildScope.WAR)
        assertTrue plugin.supportsScope(BuildScope.RUN)
    }

    void testIncludesWithListScopeEvaluation() {
        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestGrailsPlugin {
    def version = 0.1
    def scopes = [includes:['run','test']]
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        def plugin = new DefaultGrailsPlugin(test1, application)

        assertTrue plugin.supportsScope(BuildScope.TEST)
        assertFalse plugin.supportsScope(BuildScope.WAR)
        assertTrue plugin.supportsScope(BuildScope.RUN)
    }

    void testInvalidScopeName() {
        def gcl = new GroovyClassLoader()
        def test1 = gcl.parseClass('''
class TestGrailsPlugin {
    def version = 0.1
    def scopes = [includes:['run','bad']]
}
''')

        DefaultGrailsApplication application = new DefaultGrailsApplication()
        shouldFail(GrailsConfigurationException) {
            new DefaultGrailsPlugin(test1, application)
        }
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

        System.setProperty(Environment.KEY, Environment.PRODUCTION.getName())


        pluginManager = new DefaultGrailsPluginManager([test1] as Class[], application)
        pluginManager.loadPlugins()

        assertNull pluginManager.getGrailsPlugin("test")
    }
}