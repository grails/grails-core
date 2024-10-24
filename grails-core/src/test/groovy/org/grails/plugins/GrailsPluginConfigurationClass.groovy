package org.grails.plugins

import grails.boot.config.GrailsAutoConfiguration
import grails.core.GrailsApplication
import grails.plugins.GrailsPlugin
import grails.plugins.GrailsPluginManager
import groovy.transform.CompileStatic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource

import java.nio.file.Files

@CompileStatic
@Configuration
class GrailsPluginConfigurationClass extends GrailsAutoConfiguration {

    public static Boolean YAML_EXISTS = false
    public static Boolean GROOVY_EXISTS = true

    @Bean(name = "grailsPluginManager")
    GrailsPluginManager getGrailsPluginManager() {
        MockGrailsPluginManager pluginManager = new MockGrailsPluginManager()
        createGrailsPlugins(pluginManager.application).each {
            pluginManager.registerMockPlugin(it)
        }
        pluginManager
    }

    private List<GrailsPlugin> createGrailsPlugins(GrailsApplication grailsApplication) {
        final String grailsVersion = '4.0.1'
        def gcl = new GroovyClassLoader()

        GrailsPlugin plugin = new MockTestGrailsPlugin(gcl.parseClass("""class TestGrailsPlugin {
        def version = '1.0.0'
        def grailsVersion = '$grailsVersion'
        def loadAfter = ['testTwo']
}"""), grailsApplication)

        GrailsPlugin plugin2 = new MockTestTwoGrailsPlugin(gcl.parseClass("""class TestTwoGrailsPlugin {
        def version = '1.0.0'
        def grailsVersion = '$grailsVersion'
}"""), grailsApplication)

        List<GrailsPlugin>.of(plugin, plugin2)
    }

    class MockTestGrailsPlugin extends DefaultGrailsPlugin {

        MockTestGrailsPlugin(Class<?> pluginClass, GrailsApplication application) {
            super(pluginClass, application)
        }

        protected Resource getConfigurationResource(Class<?> pluginClass, String path) {
            File tempDir = Files.createTempDirectory("MockTestGrailsPlugin").toFile()
            if (YAML_EXISTS && path == PLUGIN_YML_PATH) {
                File file = new File(tempDir, "plugin.yml")
                file.write("bar: foo\n")
                file.append("foo: one\n")
                file.append("example:\n")
                file.append("  bar: foo\n")
                return new FileSystemResource(file)
            }
            if (GROOVY_EXISTS && path == PLUGIN_GROOVY_PATH) {
                File file = new File(tempDir, "plugin.groovy")
                file.write("bar = 'foo'\n")
                file.append("foo = 'one'\n")
                file.append("example.bar = 'foo'\n")
                return new FileSystemResource(file)
            }
            return null
        }

        @Override
        String getVersion() {
            "1.0"
        }
    }

    class MockTestTwoGrailsPlugin extends DefaultGrailsPlugin {

        MockTestTwoGrailsPlugin(Class<?> pluginClass, GrailsApplication application) {
            super(pluginClass, application)
        }

        protected Resource getConfigurationResource(Class<?> pluginClass, String path) {
            File tempDir = Files.createTempDirectory("MockTestTwoGrailsPlugin").toFile()
            if (YAML_EXISTS && path == PLUGIN_YML_PATH) {
                File file = new File(tempDir, "plugin.yml")
                file.write("bar: foo2\n")
                file.append("foo: one\n")
                file.append("abc: xyz\n")
                return new FileSystemResource(file)
            }
            if (GROOVY_EXISTS && path == PLUGIN_GROOVY_PATH) {
                File file = new File(tempDir, "plugin.groovy")
                file.write("bar = 'foo2'\n")
                file.append("foo = 'one2'\n")
                file.append("abc = 'xyz'\n")
                return new FileSystemResource(file)
            }
            return null
        }

        @Override
        String getVersion() {
            "1.0"
        }
    }

}
