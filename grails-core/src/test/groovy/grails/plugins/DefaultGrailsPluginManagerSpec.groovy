package grails.plugins

import grails.core.GrailsApplication
import grails.util.Metadata
import org.apache.commons.logging.Log
import org.grails.plugins.DefaultGrailsPlugin
import spock.lang.Specification
import spock.lang.Unroll

class DefaultGrailsPluginManagerSpec extends Specification {

    @Unroll
    def "should return #pluginGrailsVersion as plugin grails version"() {
        given:
        GrailsApplication app = stubGrailsApplicationWithVersion("4.0.1")
        DefaultGrailsPluginManager sut = buildPluginsManager(app)
        DefaultGrailsPlugin plugin = stubPluginWithGrailsVersion(app, pluginGrailsVersion)

        when:
        def version = sut.getPluginGrailsVersion(plugin)

        then:
        version == pluginGrailsVersion

        where:
        pluginGrailsVersion | _
        "3.3.10 > *"        | _
    }

    @Unroll
    def "it should check that plugin with grailsVersion=#pluginGrailsVersion is compatible with grails #grailsVersion"() {
        given:
        GrailsApplication app = stubGrailsApplicationWithVersion(grailsVersion)
        DefaultGrailsPluginManager sut = buildPluginsManager(app)
        DefaultGrailsPlugin plugin = stubPluginWithGrailsVersion(app, pluginGrailsVersion)

        when:
        def compatible = sut.isCompatiblePlugin(plugin)

        then:
        compatible == expectedCompatible

        where:
        grailsVersion | pluginGrailsVersion        || expectedCompatible
        "1.0"         | "3.3.1 > *"                || false
        "2.5"         | "3.0.1"                    || false
        "3.0.0"       | "3.3.10 > *"               || false
        "3.3.10"      | "4.0.0 > *"                || false
        "4.0.1"       | "3.0.0.BUILD-SNAPSHOT > *" || true
        "4.0.1"       | "4.0.1"                    || true
        "4.0.1"       | "3.0.1"                    || false
        "4.0.1"       | "3.3.1 > *"                || true
        "4.0.1"       | "3.3.10 > *"               || true
    }

    def stubGrailsApplicationWithVersion(def version) {
        GrailsApplication app = Mock(GrailsApplication)
        app.getMetadata() >> Metadata.getInstance(new ByteArrayInputStream("""
info:
    app:
        grailsVersion: $version
""".bytes))
        return app
    }

    def stubPluginWithGrailsVersion(GrailsApplication app, String grailsVersion) {
        def gcl = new GroovyClassLoader()
        return new DefaultGrailsPlugin(gcl.parseClass("class ACustomGrailsPlugin {\n" +
                "def version = \"1.0.0\"\n" +
                "def grailsVersion = \"$grailsVersion\"\n" +
                "}"), app)
    }

    def buildPluginsManager(GrailsApplication app) {
        def pluginsManager = new DefaultGrailsPluginManager(app)
        pluginsManager.LOG >> Mock(Log)
        return pluginsManager
    }
}
